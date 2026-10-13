package com.github.cerealklla.coryspondance.mail;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * The outgoing-attachment screen for writing a new letter (explicit user spec, 2026-10-12: letters
 * "can come from... other players", not just mods calling the API) -- a plain 9-slot {@link
 * ChestMenu}, same shape as {@link LetterMenu} but starting empty; the player drags their own items
 * in via the normal inventory slot-click/shift-click handling {@code ChestMenu} already provides for
 * free. The recipient name and message text live entirely client-side on {@code client.ComposeMailScreen}
 * (plain widgets, not menu slots) and travel to the server only once, in {@code net.SendLetterPayload}
 * -- see that payload's own doc for why the server reads this menu's live slots directly rather than
 * trying to carry item data through the payload itself.
 *
 * <p>{@link #removed} returns any items still sitting in the 9 slots back to the player (same
 * "give back unused ingredients on close" behavior vanilla's own crafting table/anvil already have) --
 * without this, closing Compose without sending would simply destroy whatever was dragged in.
 */
public final class ComposeMailMenu extends ChestMenu {

    public static final int SLOT_COUNT = Letter.MAX_ATTACHMENTS;

    public ComposeMailMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, Container container) {
        super(menuType, containerId, playerInventory, container, 1);
    }

    public static ComposeMailMenu create(MenuType<?> menuType, int containerId, Inventory playerInventory) {
        return new ComposeMailMenu(menuType, containerId, playerInventory, new SimpleContainer(SLOT_COUNT));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getSlot(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            getSlot(i).set(ItemStack.EMPTY);
        }
    }
}
