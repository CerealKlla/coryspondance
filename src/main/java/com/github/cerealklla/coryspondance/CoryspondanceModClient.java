package com.github.cerealklla.coryspondance;

import com.github.cerealklla.coryspondance.client.ComposeMailScreen;
import com.github.cerealklla.coryspondance.client.LetterScreen;
import com.github.cerealklla.coryspondance.client.MailboxScreen;
import com.github.cerealklla.coryspondance.registration.ModMenus;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Open/close the mailbox with a keybind (explicit user suggestion, 2026-10-12: "maybe just another
 * hotkey to open/close their mailbox") -- same shape as Myssions' own {@code J} -> Quest Log. Key
 * chosen: {@code N} -- checked against every key already claimed in this suite (Settlemynts G/H,
 * Myssions J, Lyfe K/M/[/]/O) and free.
 */
@Mod(value = CoryspondanceMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CoryspondanceMod.MODID, value = Dist.CLIENT)
public class CoryspondanceModClient {

    public static final KeyMapping OPEN_MAILBOX = new KeyMapping(
            "key.coryspondance.open_mailbox", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, KeyMapping.Category.MISC);

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAILBOX);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LETTER.get(), LetterScreen::new);
        event.register(ModMenus.COMPOSE_MAIL.get(), ComposeMailScreen::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_MAILBOX.consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            if (minecraft.screen instanceof MailboxScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new MailboxScreen());
            }
        }
    }
}
