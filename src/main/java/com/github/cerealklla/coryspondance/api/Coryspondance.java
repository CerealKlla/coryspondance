package com.github.cerealklla.coryspondance.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.cerealklla.coryspondance.mail.MailEngine;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Public facade -- the whole reason this mod exists (explicit user spec, 2026-10-12): "endpoints for
 * other mods to call... facilitates a 'mailing' mechanic." Every overload works identically whether
 * the recipient is online right now or not -- see {@code mail.MailEngine}'s own doc for how. A
 * consumer should add its own {@code bridge.CoryspondanceMailBridge} with an {@code isLoaded()} guard
 * (Coryspondance is an optional soft dependency for everyone, same convention every bridge in this
 * suite already follows), not call this class directly without checking first.
 */
public final class Coryspondance {

    private Coryspondance() {
    }

    /**
     * Sends a letter to {@code toPlayerId}, online or not.
     *
     * @param fromPlayerId             the real sending player, if any -- empty for NPC/system mail.
     * @param fromDisplayName          always what's actually shown as the sender, regardless of {@code hideSender}.
     * @param attachments              up to 9 item stacks; extra entries beyond the 9th are silently dropped.
     * @param hideSender               if true, the recipient sees "Unknown Sender" instead of {@code fromDisplayName}.
     * @param allowReply               if true (and {@code fromPlayerId} is present), the recipient can open Compose pre-filled with this sender.
     * @param deleteWhenFullyClaimed   if true, the letter removes itself once read and its attachments are fully claimed.
     */
    public static void sendMail(MinecraftServer server, UUID toPlayerId, Optional<UUID> fromPlayerId,
                                 String fromDisplayName, String message, List<ItemStack> attachments,
                                 boolean hideSender, boolean allowReply, boolean deleteWhenFullyClaimed) {
        MailEngine.sendMail(server, toPlayerId, fromPlayerId, fromDisplayName, message, attachments,
                hideSender, allowReply, deleteWhenFullyClaimed);
    }

    /** Convenience overload for an already-online recipient -- identical delivery, just skips the offline-lookup path. */
    public static void sendMail(ServerPlayer to, Optional<UUID> fromPlayerId, String fromDisplayName,
                                 String message, List<ItemStack> attachments, boolean hideSender,
                                 boolean allowReply, boolean deleteWhenFullyClaimed) {
        MailEngine.sendMail(to, fromPlayerId, fromDisplayName, message, attachments, hideSender, allowReply, deleteWhenFullyClaimed);
    }

    /** Shorthand for the common "system/NPC mail, real name always shown, no reply, cleans itself up once claimed" case -- e.g. a reward letter for a fixed bug report. */
    public static void sendSystemMail(MinecraftServer server, UUID toPlayerId, String fromDisplayName,
                                       String message, List<ItemStack> attachments) {
        sendMail(server, toPlayerId, Optional.empty(), fromDisplayName, message, attachments, false, false, true);
    }
}
