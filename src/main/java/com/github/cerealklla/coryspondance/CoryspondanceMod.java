package com.github.cerealklla.coryspondance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.cerealklla.coryspondance.api.Coryspondance;
import com.github.cerealklla.coryspondance.debug.DebugCommands;
import com.github.cerealklla.coryspondance.mail.ComposeMailMenu;
import com.github.cerealklla.coryspondance.mail.Letter;
import com.github.cerealklla.coryspondance.mail.LetterMenuProvider;
import com.github.cerealklla.coryspondance.mail.MailEngine;
import com.github.cerealklla.coryspondance.net.DeleteLetterPayload;
import com.github.cerealklla.coryspondance.net.OpenLetterPayload;
import com.github.cerealklla.coryspondance.net.RequestComposePayload;
import com.github.cerealklla.coryspondance.net.SendLetterPayload;
import com.github.cerealklla.coryspondance.registration.ModAttachments;
import com.github.cerealklla.coryspondance.registration.ModMenus;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CoryspondanceMod.MODID)
public class CoryspondanceMod {

    public static final String MODID = "coryspondance";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CoryspondanceMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DebugCommands.register(event.getDispatcher()));
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Coryspondance common setup");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(OpenLetterPayload.TYPE, OpenLetterPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    Letter letter = player.getData(ModAttachments.PLAYER_MAIL).find(payload.letterId()).orElse(null);
                    if (letter == null) {
                        return;
                    }
                    if (!letter.read()) {
                        letter = letter.withRead(true);
                        MailEngine.replaceLetter(player, letter);
                    }
                    player.openMenu(new LetterMenuProvider(letter));
                });

        registrar.playToServer(DeleteLetterPayload.TYPE, DeleteLetterPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    Letter letter = player.getData(ModAttachments.PLAYER_MAIL).find(payload.letterId()).orElse(null);
                    if (letter == null) {
                        return;
                    }
                    if (!letter.attachments().isEmpty()) {
                        player.sendSystemMessage(Component.literal("Claim this letter's items before deleting it."));
                        return;
                    }
                    MailEngine.removeLetter(player, payload.letterId());
                });

        registrar.playToServer(RequestComposePayload.TYPE, RequestComposePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, inventory, p) -> ComposeMailMenu.create(ModMenus.COMPOSE_MAIL.get(), containerId, inventory),
                            Component.literal("Write Letter")));
                });

        registrar.playToServer(SendLetterPayload.TYPE, SendLetterPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer sender)) {
                        return;
                    }
                    if (!(sender.containerMenu instanceof ComposeMailMenu menu)) {
                        return;
                    }
                    String toName = payload.toPlayerName() == null ? "" : payload.toPlayerName().trim();
                    if (toName.isEmpty()) {
                        sender.sendSystemMessage(Component.literal("Enter a recipient name."));
                        return;
                    }
                    ServerPlayer recipient = sender.level().getServer().getPlayerList().getPlayerByName(toName);
                    if (recipient == null) {
                        sender.sendSystemMessage(Component.literal("\"" + toName + "\" isn't online right now -- offline recipients by name aren't supported yet."));
                        return;
                    }
                    if (recipient.getUUID().equals(sender.getUUID())) {
                        sender.sendSystemMessage(Component.literal("You can't mail yourself."));
                        return;
                    }

                    List<ItemStack> attachments = new ArrayList<>();
                    for (int i = 0; i < ComposeMailMenu.SLOT_COUNT; i++) {
                        ItemStack stack = menu.getSlot(i).getItem();
                        if (!stack.isEmpty()) {
                            attachments.add(stack.copy());
                            menu.getSlot(i).set(ItemStack.EMPTY);
                        }
                    }
                    menu.broadcastChanges();

                    Coryspondance.sendMail(recipient, Optional.of(sender.getUUID()), sender.getName().getString(),
                            payload.message(), attachments, false, true, false);
                    sender.sendSystemMessage(Component.literal("Letter sent to " + recipient.getName().getString() + "."));
                });
    }
}
