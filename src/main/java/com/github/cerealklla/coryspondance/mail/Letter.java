package com.github.cerealklla.coryspondance.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

/**
 * One piece of mail -- the whole point of this mod (explicit user spec, 2026-10-12): "a To and From,
 * a message, and up to 9 attachments (item slots, allowing stacks)." {@code fromPlayerId} empty means
 * an NPC/system sender (no real player to reply to); {@code fromDisplayName} is always what's actually
 * shown to the recipient, independent of whether a real sender exists -- a system letter sets it to
 * something like "The Town Hall" directly, a player letter sets it to the sender's own real name
 * (or {@code "Unknown Sender"} if {@link #hideSender()} is set, same field either way).
 *
 * <p>{@code hideSender}/{@code allowReply}/{@code deleteWhenFullyClaimed} are the first three
 * sender-configurable options (explicit user spec: "hiding the name, preventing Reply, and several
 * other things you can think of") -- more are just new boolean/optional fields here, not a new
 * mechanism, so this stays deliberately small rather than building a generic flag-bag up front.
 */
public record Letter(UUID id, UUID toPlayerId, Optional<UUID> fromPlayerId, String fromDisplayName,
                      String message, List<ItemStack> attachments, boolean hideSender, boolean allowReply,
                      boolean deleteWhenFullyClaimed, long sentAtGameTime, boolean read) {

    public static final int MAX_ATTACHMENTS = 9;

    public static final Codec<Letter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Letter::id),
            UUIDUtil.CODEC.fieldOf("to_player_id").forGetter(Letter::toPlayerId),
            UUIDUtil.CODEC.optionalFieldOf("from_player_id").forGetter(Letter::fromPlayerId),
            Codec.STRING.fieldOf("from_display_name").forGetter(Letter::fromDisplayName),
            Codec.STRING.fieldOf("message").forGetter(Letter::message),
            ItemStack.CODEC.listOf().optionalFieldOf("attachments", List.of()).forGetter(Letter::attachments),
            Codec.BOOL.optionalFieldOf("hide_sender", false).forGetter(Letter::hideSender),
            Codec.BOOL.optionalFieldOf("allow_reply", true).forGetter(Letter::allowReply),
            Codec.BOOL.optionalFieldOf("delete_when_fully_claimed", false).forGetter(Letter::deleteWhenFullyClaimed),
            Codec.LONG.fieldOf("sent_at_game_time").forGetter(Letter::sentAtGameTime),
            Codec.BOOL.optionalFieldOf("read", false).forGetter(Letter::read)
    ).apply(instance, Letter::new));

    public String displaySenderName() {
        return hideSender ? "Unknown Sender" : fromDisplayName;
    }

    public Letter withRead(boolean newRead) {
        return new Letter(id, toPlayerId, fromPlayerId, fromDisplayName, message, attachments, hideSender,
                allowReply, deleteWhenFullyClaimed, sentAtGameTime, newRead);
    }

    public Letter withAttachments(List<ItemStack> newAttachments) {
        return new Letter(id, toPlayerId, fromPlayerId, fromDisplayName, message, List.copyOf(newAttachments),
                hideSender, allowReply, deleteWhenFullyClaimed, sentAtGameTime, read);
    }
}
