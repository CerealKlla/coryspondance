package com.github.cerealklla.coryspondance.client;

import com.github.cerealklla.coryspondance.mail.LetterMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** {@link LetterMenu}'s screen -- same shape as Yconomics' own {@code bag.client.LootBagScreen} (a copy of vanilla's generic container screen plus a "Take All" button), just 1 row instead of 6. */
public final class LetterScreen extends AbstractContainerScreen<LetterMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private final int containerRows;

    public LetterScreen(LetterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
        this.containerRows = menu.getRowCount();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 80;
        addRenderableWidget(Button.builder(Component.literal("Take All"), button ->
                        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, LetterMenu.TAKE_ALL_BUTTON_ID))
                .bounds(leftPos + (imageWidth - buttonWidth) / 2, topPos - 24, buttonWidth, 20)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, this.containerRows * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo + this.containerRows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }
}
