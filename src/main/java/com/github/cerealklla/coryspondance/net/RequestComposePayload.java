package com.github.cerealklla.coryspondance.net;

import com.github.cerealklla.coryspondance.CoryspondanceMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Write Letter" / "Reply" on {@code client.MailboxScreen} -- opens a fresh {@code mail.ComposeMailMenu}. No fields; the recipient/message text live entirely client-side until {@code SendLetterPayload}. */
public record RequestComposePayload() implements CustomPacketPayload {

    public static final Type<RequestComposePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CoryspondanceMod.MODID, "request_compose"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, RequestComposePayload> STREAM_CODEC =
            StreamCodec.unit(new RequestComposePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
