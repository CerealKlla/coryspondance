package com.github.cerealklla.coryspondance.registration;

import com.github.cerealklla.coryspondance.CoryspondanceMod;
import com.github.cerealklla.coryspondance.mail.ComposeMailMenu;
import com.github.cerealklla.coryspondance.mail.LetterMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CoryspondanceMod.MODID);

    // Client-side reconstruction just needs a blank 9-slot placeholder -- real item contents arrive
    // separately via vanilla's own slot-sync packets, same as every other menu in this suite. The
    // real letterId only matters server-side (see LetterMenu's own doc), so it's never read from buf.
    public static final DeferredHolder<MenuType<?>, MenuType<LetterMenu>> LETTER = MENU_TYPES.register(
            "letter",
            () -> IMenuTypeExtension.create((windowId, inventory, buf) ->
                    new LetterMenu(null, windowId, inventory, buf.readUUID(), new SimpleContainer(LetterMenu.SLOT_COUNT))));

    public static final DeferredHolder<MenuType<?>, MenuType<ComposeMailMenu>> COMPOSE_MAIL = MENU_TYPES.register(
            "compose_mail",
            () -> IMenuTypeExtension.create((windowId, inventory, buf) -> ComposeMailMenu.create(null, windowId, inventory)));
}
