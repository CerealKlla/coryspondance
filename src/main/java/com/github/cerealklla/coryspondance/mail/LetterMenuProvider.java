package com.github.cerealklla.coryspondance.mail;

import com.github.cerealklla.coryspondance.registration.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.IMenuProviderExtension;

/** Opens {@link LetterMenu} via {@code ServerPlayer#openMenu} -- the letter id rides along on the extra-data channel, read back by {@code registration.ModMenus}' client-reconstruction factory. */
public final class LetterMenuProvider implements MenuProvider, IMenuProviderExtension {

    private final Letter letter;

    public LetterMenuProvider(Letter letter) {
        this.letter = letter;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Letter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return LetterMenu.forLetter(ModMenus.LETTER.get(), containerId, inventory, letter);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(letter.id());
    }
}
