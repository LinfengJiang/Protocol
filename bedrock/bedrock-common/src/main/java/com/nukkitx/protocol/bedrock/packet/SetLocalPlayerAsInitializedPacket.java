package com.nukkitx.protocol.bedrock.packet;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetLocalPlayerAsInitializedPacket extends BedrockPacket {
    private long runtimeEntityId;

    @Override
    public final boolean handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }
}
