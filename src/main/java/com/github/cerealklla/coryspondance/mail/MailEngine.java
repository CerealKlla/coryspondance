package com.github.cerealklla.coryspondance.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import com.github.cerealklla.coryspondance.CoryspondanceMod;
import com.github.cerealklla.coryspondance.registration.ModAttachments;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Real implementation behind {@code api.Coryspondance} -- see that class's own doc for the public
 * surface. The entire reason this mod exists: {@link #sendMail} works identically whether the
 * recipient is online right now or not, so a caller never has to care.
 *
 * <p>Offline delivery mirrors Lyfe's own {@code api.Lyfe#addXp(MinecraftServer, UUID, ...)} exactly
 * (confirmed live precedent, not re-derived) -- if the recipient is online, mutate their live
 * attachment directly; if not, load their raw NBT via {@code PlayerDataStorage} (bypassing
 * {@code PlayerList}'s connection-gated save/load), deserialize into a throwaway {@code FakePlayer}
 * used purely as an {@code AttachmentHolder} vessel, mutate the attachment there, save it back. A
 * no-op if the UUID has never played on this server at all (no save file exists yet to write into).
 * Always re-checks online status first rather than always touching disk -- writing a {@code
 * FakePlayer}'s blank state over a connected player's real save file would risk a lost update.
 */
public final class MailEngine {

    private MailEngine() {
    }

    public static void sendMail(ServerPlayer to, Optional<UUID> fromPlayerId, String fromDisplayName,
                                 String message, List<ItemStack> attachments, boolean hideSender,
                                 boolean allowReply, boolean deleteWhenFullyClaimed) {
        Letter letter = buildLetter(to.getUUID(), fromPlayerId, fromDisplayName, message, attachments,
                hideSender, allowReply, deleteWhenFullyClaimed, to.level().getGameTime());
        PlayerMail updated = to.getData(ModAttachments.PLAYER_MAIL).withAdded(letter);
        to.setData(ModAttachments.PLAYER_MAIL, updated);
    }

    public static void sendMail(MinecraftServer server, UUID toPlayerId, Optional<UUID> fromPlayerId,
                                 String fromDisplayName, String message, List<ItemStack> attachments,
                                 boolean hideSender, boolean allowReply, boolean deleteWhenFullyClaimed) {
        ServerPlayer online = server.getPlayerList().getPlayer(toPlayerId);
        if (online != null) {
            sendMail(online, fromPlayerId, fromDisplayName, message, attachments, hideSender, allowReply, deleteWhenFullyClaimed);
            return;
        }

        PlayerDataStorage playerIo = server.getPlayerList().getPlayerIo();
        Optional<CompoundTag> saved = playerIo.load(new NameAndId(toPlayerId, "Unknown"));
        if (saved.isEmpty()) {
            return;
        }

        FakePlayer fake = new FakePlayer(server.overworld(), new GameProfile(toPlayerId, "Unknown"));
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(fake.problemPath(), CoryspondanceMod.LOGGER)) {
            ValueInput input = TagValueInput.create(reporter, server.registryAccess(), saved.get());
            fake.load(input);
        }
        Letter letter = buildLetter(toPlayerId, fromPlayerId, fromDisplayName, message, attachments,
                hideSender, allowReply, deleteWhenFullyClaimed, server.overworld().getGameTime());
        PlayerMail updated = fake.getData(ModAttachments.PLAYER_MAIL).withAdded(letter);
        fake.setData(ModAttachments.PLAYER_MAIL, updated);
        playerIo.save(fake);
    }

    private static Letter buildLetter(UUID toPlayerId, Optional<UUID> fromPlayerId, String fromDisplayName,
                                       String message, List<ItemStack> attachments, boolean hideSender,
                                       boolean allowReply, boolean deleteWhenFullyClaimed, long gameTime) {
        List<ItemStack> capped = attachments.size() > Letter.MAX_ATTACHMENTS
                ? List.copyOf(attachments.subList(0, Letter.MAX_ATTACHMENTS))
                : List.copyOf(attachments);
        return new Letter(UUID.randomUUID(), toPlayerId, fromPlayerId, fromDisplayName, message, capped,
                hideSender, allowReply, deleteWhenFullyClaimed, gameTime, false);
    }

    /** Writes a letter back (read flag, trimmed attachments, etc.) and resyncs the client -- mutating the fetched attachment alone never resyncs on its own, see this class's own doc precedent. */
    public static void replaceLetter(ServerPlayer player, Letter updated) {
        PlayerMail mail = player.getData(ModAttachments.PLAYER_MAIL).withReplaced(updated);
        player.setData(ModAttachments.PLAYER_MAIL, mail);
    }

    public static void removeLetter(ServerPlayer player, UUID letterId) {
        PlayerMail mail = player.getData(ModAttachments.PLAYER_MAIL).withRemoved(letterId);
        player.setData(ModAttachments.PLAYER_MAIL, mail);
    }
}
