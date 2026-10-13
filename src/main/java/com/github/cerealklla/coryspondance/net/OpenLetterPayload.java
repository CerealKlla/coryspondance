package com.github.cerealklla.coryspondance.net;

import java.util.UUID;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.coryspondance.CoryspondanceMod;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Claim Items" on a selected letter in {@code client.MailboxScreen}. */
public record OpenLetterPayload(UUID letterId) implements CustomPacketPayload {

    public static final Type<OpenLetterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CoryspondanceMod.MODID, "open_letter"));

    public static final StreamCodec<ByteBuf, OpenLetterPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OpenLetterPayload::letterId,
            OpenLetterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
