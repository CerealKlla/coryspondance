package com.github.cerealklla.coryspondance.net;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.coryspondance.CoryspondanceMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: "Send" on {@code client.ComposeMailScreen}. The server reads the sender's own
 * currently-open {@code mail.ComposeMailMenu} directly for the 9 attachment slots rather than trying
 * to carry item data through this payload -- see that menu's own doc.
 */
public record SendLetterPayload(String toPlayerName, String message) implements CustomPacketPayload {

    private static final int MAX_NAME_LENGTH = 16; // Real Minecraft username cap.
    private static final int MAX_MESSAGE_LENGTH = 1000;

    public static final Type<SendLetterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CoryspondanceMod.MODID, "send_letter"));

    public static final StreamCodec<ByteBuf, SendLetterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), SendLetterPayload::toPlayerName,
            ByteBufCodecs.stringUtf8(MAX_MESSAGE_LENGTH), SendLetterPayload::message,
            SendLetterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
