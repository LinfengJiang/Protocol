package com.nukkitx.protocol.bedrock;

import com.nukkitx.network.raknet.RakNetReliability;
import com.nukkitx.network.raknet.RakNetSession;
import com.nukkitx.network.raknet.RakNetSessionListener;
import com.nukkitx.network.raknet.RakNetState;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.annotation.NoEncryption;
import com.nukkitx.protocol.bedrock.compat.BedrockCompat;
import com.nukkitx.protocol.bedrock.compressionhandler.BedrockCompressionHandler;
import com.nukkitx.protocol.bedrock.compressionhandler.DefaultBedrockCompressionHandler;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.handler.DefaultBatchHandler;
import com.nukkitx.protocol.util.NativeCodeFactory;
import com.voxelwind.server.jni.hash.VoxelwindHash;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.jni.cipher.BungeeCipher;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class BedrockSession {
    static final InternalLogger log = InternalLoggerFactory.getInstance(BedrockSession.class);
    private static final ThreadLocal<VoxelwindHash> hashLocal = ThreadLocal.withInitial(NativeCodeFactory.hash::newInstance);

    private final Queue<BedrockPacket> queuedPackets = new ConcurrentLinkedQueue<>();
    private final AtomicLong sentEncryptedPacketCount = new AtomicLong();
    final RakNetSession connection;
    private BedrockPacketCodec packetCodec = BedrockCompat.COMPAT_CODEC;
    private Set<Consumer<DisconnectReason>> disconnectHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private BedrockPacketHandler packetHandler;
    private BedrockCompressionHandler compressionHandler = DefaultBedrockCompressionHandler.DEFAULT;
    private BatchHandler batchedHandler = DefaultBatchHandler.INSTANCE;
    private BungeeCipher encryptionCipher = null;
    private BungeeCipher decryptionCipher = null;
    private SecretKey agreedKey;
    private volatile boolean closed = false;
    private volatile boolean logging = true;

    BedrockSession(RakNetSession connection) {
        this.connection = connection;
        this.setupConnection();
    }

    protected abstract void setupConnection();

    public void setPacketHandler(@Nonnull BedrockPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public void setPacketCodec(BedrockPacketCodec packetCodec) {
        this.packetCodec = Objects.requireNonNull(packetCodec, "packetCodec");
    }

    public void setCompressionHandler(BedrockCompressionHandler compressionHandler) {
        this.compressionHandler = Objects.requireNonNull(compressionHandler, "compressionHandler");
    }

    void checkForClosed() {
        if (this.closed) {
            throw new IllegalStateException("Connection has been closed");
        }
    }

    public void sendPacket(BedrockPacket packet) {
        this.checkForClosed();
        Objects.requireNonNull(packet, "packet");
        if (log.isTraceEnabled() && this.logging) {
            String to = this.connection.getAddress().toString();
            log.trace("Outbound {}: {}", to, packet);
        }

        // Verify that the packet ID exists.
        this.packetCodec.getId(packet);

        this.queuedPackets.add(packet);
    }

    public void sendPacketImmediately(BedrockPacket packet) {
        this.checkForClosed();
        Objects.requireNonNull(packet, "packet");

        if (log.isTraceEnabled() && this.logging) {
            String to = this.connection.getAddress().toString();
            log.trace("Outbound {}: {}", to, packet);
        }

        this.sendWrapped(Collections.singletonList(packet), !packet.getClass().isAnnotationPresent(NoEncryption.class));
    }

    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt) {
        ByteBuf compressed = null;
        try {
            compressed = this.compressionHandler.compressPackets(this.packetCodec, packets);
            this.sendWrapped(compressed, encrypt);
        } finally {
            if (compressed != null) {
                compressed.release();
            }
        }
    }

    public void sendWrapped(ByteBuf compressed, boolean encrypt) {
        Objects.requireNonNull(compressed, "compressed");
        ByteBuf finalPayload = null;
        try {
            int startIndex = compressed.readerIndex();
            finalPayload = PooledByteBufAllocator.DEFAULT.directBuffer();
            finalPayload.writeByte(0xfe); // Wrapped packet ID
            if (this.encryptionCipher != null && encrypt) {
                ByteBuf withTrailer = PooledByteBufAllocator.DEFAULT.directBuffer();
                compressed.readerIndex(startIndex);
                byte[] trailer = generateTrailer(compressed);
                compressed.readerIndex(startIndex);
                withTrailer.writeBytes(compressed);
                withTrailer.writeBytes(trailer);

                encryptionCipher.cipher(withTrailer, finalPayload);
            } else {
                finalPayload.writeBytes(compressed);
            }
            this.connection.send(finalPayload, RakNetReliability.RELIABLE_ORDERED);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to encrypt package", e);
        } finally {
            if (finalPayload != null) {
                finalPayload.release();
            }
        }
    }

    void onTick() {
        if (this.closed) {
            return;
        }

        this.sendQueued();
    }

    private void sendQueued() {
        BedrockPacket packet;
        Collection<BedrockPacket> toBatch = new ArrayDeque<>();
        while ((packet = this.queuedPackets.poll()) != null) {
            if (packet.getClass().isAnnotationPresent(NoEncryption.class)) {
                // We hit a unencryptable packet. Send the current wrapper and then send the unencryptable packet.
                if (!toBatch.isEmpty()) {
                    sendWrapped(toBatch, true);
                    toBatch = new ArrayDeque<>();
                }

                this.sendPacketImmediately(packet);

                try {
                    // Delay things a tiny bit
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                }
                continue;
            }

            toBatch.add(packet);
        }

        if (!toBatch.isEmpty()) {
            this.sendWrapped(toBatch, true);
        }
    }

    public void enableEncryption(@Nonnull SecretKey secretKey) {
        this.checkForClosed();
        log.debug("Encryption enabled.");
        //noinspection ResultOfMethodCallIgnored
        Objects.requireNonNull(secretKey, "secretKey");
        if (!secretKey.getAlgorithm().equals("AES")) {
            throw new IllegalArgumentException("Invalid key algorithm");
        }
        if (this.encryptionCipher != null || this.decryptionCipher != null) {
            throw new IllegalStateException("Encryption has already been enabled");
        }

        this.agreedKey = secretKey;
        byte[] iv = Arrays.copyOf(secretKey.getEncoded(), 16);
        try {
            this.encryptionCipher = NativeCodeFactory.cipher.newInstance();
            this.decryptionCipher = NativeCodeFactory.cipher.newInstance();

            this.encryptionCipher.init(true, secretKey, iv);
            this.decryptionCipher.init(false, secretKey, iv);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize ciphers", e);
        }

    }

    private byte[] generateTrailer(ByteBuf buf) {
        VoxelwindHash hash = hashLocal.get();
        ByteBuf counterBuf = null;
        ByteBuf keyBuf = null;
        try {
            counterBuf = PooledByteBufAllocator.DEFAULT.directBuffer(8);
            keyBuf = PooledByteBufAllocator.DEFAULT.directBuffer(agreedKey.getEncoded().length);
            counterBuf.writeLongLE(this.sentEncryptedPacketCount.getAndIncrement());
            keyBuf.writeBytes(this.agreedKey.getEncoded());

            hash.update(counterBuf);
            hash.update(buf);
            hash.update(keyBuf);
            byte[] digested = hash.digest();
            return Arrays.copyOf(digested, 8);
        } finally {
            if (counterBuf != null) {
                counterBuf.release();
            }
            if (keyBuf != null) {
                keyBuf.release();
            }
        }
    }

    public boolean isEncrypted() {
        return encryptionCipher != null;
    }

    public abstract void disconnect();

    void close(DisconnectReason reason) {
        checkForClosed();
        this.closed = true;
        // Free native resources if required
        if (this.encryptionCipher != null) {
            this.encryptionCipher.free();
        }
        if (this.decryptionCipher != null) {
            this.decryptionCipher.free();
        }

        // Destroy secret key
        if (this.agreedKey != null && !this.agreedKey.isDestroyed()) {
            try {
                this.agreedKey.destroy();
            } catch (DestroyFailedException e) {
                // Ignore - throws exception by default
            }
        }
        for (Consumer<DisconnectReason> disconnectHandler : this.disconnectHandlers) {
            disconnectHandler.accept(reason);
        }
    }

    private void onWrappedPacket(final ByteBuf wrappedData) {
        ByteBuf unwrappedData = null;
        try {
            if (this.isEncrypted()) {
                // Decryption
                unwrappedData = PooledByteBufAllocator.DEFAULT.directBuffer(wrappedData.readableBytes());
                this.decryptionCipher.cipher(wrappedData, unwrappedData);
                // TODO: Maybe verify the checksum?
                unwrappedData = unwrappedData.slice(0, unwrappedData.readableBytes() - 8);
            } else {
                // Encryption not enabled so it should be readable.
                unwrappedData = wrappedData;
            }
            unwrappedData.markReaderIndex();

            Collection<BedrockPacket> packets = this.compressionHandler.decompressPackets(this.packetCodec, unwrappedData);
            this.batchedHandler.handle(this, unwrappedData, packets);
        } catch (GeneralSecurityException e) {
            // ignore
        } finally {
            if (unwrappedData != null && unwrappedData != wrappedData) {
                unwrappedData.release();
            }
        }
    }

    public InetSocketAddress getAddress() {
        return this.connection.getAddress();
    }

    public boolean isClosed() {
        return this.connection.isClosed();
    }

    public BedrockPacketCodec getPacketCodec() {
        return this.packetCodec;
    }

    public BedrockPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    public BedrockCompressionHandler getCompressionHandler() {
        return this.compressionHandler;
    }

    public BatchHandler getBatchedHandler() {
        return this.batchedHandler;
    }

    public void setBatchedHandler(BatchHandler batchedHandler) {
        this.batchedHandler = Objects.requireNonNull(batchedHandler, "batchHandler");
    }

    public boolean isLogging() {
        return this.logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public void addDisconnectHandler(Consumer<DisconnectReason> disconnectHandler) {
        Objects.requireNonNull(disconnectHandler, "disconnectHandler");
        this.disconnectHandlers.add(disconnectHandler);
    }

    @ParametersAreNonnullByDefault
    abstract class BedrockSessionListener implements RakNetSessionListener {

        @Override
        public void onUserPacket(ByteBuf packet) {
            if (BedrockSession.this.connection.getState() != RakNetState.CONNECTED) {
                // We shouldn't be receiving packets till the connection is full established.
                return;
            }

            int packetId = packet.readUnsignedByte();
            if (packetId == 0xfe /* Wrapper packet */) {
                BedrockSession.this.onWrappedPacket(packet);
            }
        }
    }
}
