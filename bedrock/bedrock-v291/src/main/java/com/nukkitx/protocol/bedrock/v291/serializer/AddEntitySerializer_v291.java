package com.nukkitx.protocol.bedrock.v291.serializer;

import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.v291.BedrockUtils;
import com.nukkitx.protocol.serializer.PacketSerializer;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddEntitySerializer_v291 implements PacketSerializer<AddEntityPacket> {
    public static final AddEntitySerializer_v291 INSTANCE = new AddEntitySerializer_v291();

    @Override
    public void serialize(ByteBuf buffer, AddEntityPacket packet) {
        VarInts.writeLong(buffer, packet.getUniqueEntityId());
        VarInts.writeUnsignedLong(buffer, packet.getRuntimeEntityId());
        VarInts.writeUnsignedInt(buffer, packet.getEntityType());
        BedrockUtils.writeVector3f(buffer, packet.getPosition());
        BedrockUtils.writeVector3f(buffer, packet.getMotion());
        BedrockUtils.writeVector3f(buffer, packet.getRotation());
        BedrockUtils.writeArray(buffer, packet.getAttributes(), BedrockUtils::writeEntityAttribute);
        BedrockUtils.writeMetadata(buffer, packet.getMetadata());
        BedrockUtils.writeArray(buffer, packet.getEntityLinks(), BedrockUtils::writeEntityLink);
    }

    @Override
    public void deserialize(ByteBuf buffer, AddEntityPacket packet) {
        packet.setUniqueEntityId(VarInts.readLong(buffer));
        packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
        packet.setEntityType(VarInts.readUnsignedInt(buffer));
        packet.setPosition(BedrockUtils.readVector3f(buffer));
        packet.setMotion(BedrockUtils.readVector3f(buffer));
        packet.setRotation(BedrockUtils.readVector3f(buffer));
        BedrockUtils.readArray(buffer, packet.getAttributes(), BedrockUtils::readEntityAttribute);
        BedrockUtils.readMetadata(buffer, packet.getMetadata());
        BedrockUtils.readArray(buffer, packet.getEntityLinks(), BedrockUtils::readEntityLink);
    }
}
