package com.github.cerealklla.coryspondance.net;

import java.util.UUID;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.coryspondance.CoryspondanceMod;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Delete" on a selected, already-emptied letter in {@code client.MailboxScreen}. */
public record DeleteLetterPayload(UUID letterId) implements CustomPacketPayload {

    public static final Type<DeleteLetterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CoryspondanceMod.MODID, "delete_letter"));

    public static final StreamCodec<ByteBuf, DeleteLetterPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DeleteLetterPayload::letterId,
            DeleteLetterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
