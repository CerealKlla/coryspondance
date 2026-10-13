package com.github.cerealklla.coryspondance.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import com.github.cerealklla.coryspondance.api.Coryspondance;
import com.github.cerealklla.coryspondance.mail.Letter;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** DEBUG ONLY -- {@code /coryspondance givetest} sends the invoking player a test letter with a small Gold Nugget attachment, exercising the whole send/receive/claim pipeline without needing another mod to call the API. {@code /coryspondance sendmail} is the real operator tool this was actually asked for (2026-10-12, right after the bug-report-reward flat amount was set): send a named player a Gold Nugget letter whether they're online or not -- no more waiting around for someone to log in just to run {@code /yconomics purse add}. Same deliberately-no-permission-gate precedent as every other debug command in this suite. */
public final class DebugCommands {

    private DebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coryspondance")
                .requires(source -> true)
                .then(Commands.literal("givetest").executes(DebugCommands::giveTest))
                .then(Commands.literal("sendmail")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("gold", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(DebugCommands::sendMailToPlayer))))));
    }

    private static int giveTest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        Coryspondance.sendMail(player, Optional.empty(), "Coryspondance Test",
                "This is a test letter, sent to exercise the whole mail pipeline end to end.",
                List.of(new ItemStack(Items.GOLD_NUGGET, 5)), false, false, false);
        ctx.getSource().sendSuccess(() -> Component.literal("Sent a test letter."), true);
        return 1;
    }

    private static int sendMailToPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        int gold = IntegerArgumentType.getInteger(ctx, "gold");
        String message = StringArgumentType.getString(ctx, "message");

        Optional<NameAndId> profile = ctx.getSource().getServer().services().nameToIdCache().get(playerName);
        if (profile.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No known player named \"" + playerName + "\" (they need to have joined this server at least once)."));
            return 0;
        }
        // Real incident, 2026-10-12: a first version of this command put the whole requested amount
        // into ONE ItemStack -- fine for a small test (givetest's flat 5), but a flat 500-gold reward
        // amount set the same day immediately broke it. A single Gold Nugget stack's own real max
        // (64, same as any vanilla item not explicitly unstackable) is well below what this command
        // needs to support, and the letter's own attachment encoding rejected the oversized stack
        // outright rather than silently clamping it -- caught immediately (nothing was actually saved
        // to the recipient's data; confirmed via a direct `data get entity` check before this fix).
        // Splits across multiple full-size stacks instead, up to the letter's own attachment cap.
        int maxStackSize = new ItemStack(Items.GOLD_NUGGET).getMaxStackSize();
        int maxGoldPerLetter = maxStackSize * Letter.MAX_ATTACHMENTS;
        if (gold > maxGoldPerLetter) {
            ctx.getSource().sendFailure(Component.literal(
                    "That's too much for one letter -- max is " + maxGoldPerLetter + " Gold Nuggets (send multiple letters for more)."));
            return 0;
        }
        List<ItemStack> attachments = new ArrayList<>();
        int remaining = gold;
        while (remaining > 0) {
            int stackCount = Math.min(remaining, maxStackSize);
            attachments.add(new ItemStack(Items.GOLD_NUGGET, stackCount));
            remaining -= stackCount;
        }

        UUID toPlayerId = profile.get().id();
        Coryspondance.sendSystemMail(ctx.getSource().getServer(), toPlayerId, "Kyngdoms", message, attachments);
        ctx.getSource().sendSuccess(() -> Component.literal("Mailed " + gold + " Gold Nugget(s) to " + playerName + "."), true);
        return 1;
    }
}
