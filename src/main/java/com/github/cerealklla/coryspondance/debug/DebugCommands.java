package com.github.cerealklla.coryspondance.debug;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import com.github.cerealklla.coryspondance.api.Coryspondance;

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
        UUID toPlayerId = profile.get().id();
        Coryspondance.sendSystemMail(ctx.getSource().getServer(), toPlayerId, "Kyngdoms",
                message, List.of(new ItemStack(Items.GOLD_NUGGET, gold)));
        ctx.getSource().sendSuccess(() -> Component.literal("Mailed " + gold + " Gold Nugget(s) to " + playerName + "."), true);
        return 1;
    }
}
