package com.github.cerealklla.coryspondance.debug;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;

import com.github.cerealklla.coryspondance.api.Coryspondance;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** DEBUG ONLY -- {@code /coryspondance givetest} sends the invoking player a test letter with a small Gold Nugget attachment, exercising the whole send/receive/claim pipeline without needing another mod to call the API. Same deliberately-no-permission-gate precedent as every other debug command in this suite. */
public final class DebugCommands {

    private DebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coryspondance")
                .requires(source -> true)
                .then(Commands.literal("givetest").executes(DebugCommands::giveTest)));
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
}
