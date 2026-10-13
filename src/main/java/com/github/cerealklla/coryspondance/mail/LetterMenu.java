package com.github.cerealklla.coryspondance.mail;

import java.util.UUID;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * The 9-slot attachment claim screen for one letter, opened via {@code net.OpenLetterPayload} --
 * same "{@code ChestMenu} + one Take All button" shape as Yconomics' own {@code bag.LootBagMenu},
 * just one row instead of six (the letter's own {@link Letter#MAX_ATTACHMENTS}). Reading the
 * message/sender/deciding whether to open this at all happens entirely client-side off the already-
 * synced {@code PlayerMail} attachment (see {@code client.MailboxScreen}) -- this menu exists purely
 * to let the player actually grab the items, nothing else.
 *
 * <p>On close ({@link #removed}), whatever's left in the 9 slots is written back into the real
 * {@link Letter} (so partially-claimed mail doesn't lose the rest), and the letter is deleted outright
 * if it's now empty, already read, and {@link Letter#deleteWhenFullyClaimed()} -- the one piece of
 * real behavior beyond a plain item grab.
 */
public final class LetterMenu extends ChestMenu {

    public static final int SLOT_COUNT = Letter.MAX_ATTACHMENTS;
    public static final int TAKE_ALL_BUTTON_ID = 0;

    private final UUID letterId;

    public LetterMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, UUID letterId, Container container) {
        super(menuType, containerId, playerInventory, container, 1);
        this.letterId = letterId;
    }

    public static LetterMenu forLetter(MenuType<?> menuType, int containerId, Inventory playerInventory, Letter letter) {
        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < letter.attachments().size() && i < SLOT_COUNT; i++) {
            container.setItem(i, letter.attachments().get(i));
        }
        return new LetterMenu(menuType, containerId, playerInventory, letter.id(), container);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != TAKE_ALL_BUTTON_ID) {
            return super.clickMenuButton(player, id);
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            Slot slot = getSlot(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            slot.set(ItemStack.EMPTY);
        }
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        java.util.List<ItemStack> remaining = new java.util.ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getSlot(i).getItem();
            if (!stack.isEmpty()) {
                remaining.add(stack);
            }
        }
        Letter current = serverPlayer.getData(com.github.cerealklla.coryspondance.registration.ModAttachments.PLAYER_MAIL)
                .find(letterId).orElse(null);
        if (current == null) {
            return; // Deleted from elsewhere while this was open -- nothing to write back.
        }
        if (remaining.isEmpty() && current.read() && current.deleteWhenFullyClaimed()) {
            MailEngine.removeLetter(serverPlayer, letterId);
        } else {
            MailEngine.replaceLetter(serverPlayer, current.withAttachments(remaining));
        }
    }
}
