package com.github.cerealklla.coryspondance.client;

import com.github.cerealklla.coryspondance.mail.ComposeMailMenu;
import com.github.cerealklla.coryspondance.net.SendLetterPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Write-a-letter screen (explicit user spec, 2026-10-12: letters "can come from... other players") --
 * a real {@link ComposeMailMenu} (9 slots the player drags their own items into, standard inventory
 * grid below) plus two plain client-side widgets stacked above the container box, same "extra widget
 * positioned above {@code topPos}" trick Yconomics' own {@code bag.client.LootBagScreen} already uses
 * for its Take All button.
 *
 * <p>{@link #pendingPrefillRecipient} is a one-shot static handoff from {@code MailboxScreen}'s
 * "Reply" button, set immediately before requesting this menu open and consumed here in {@link
 * #init()} -- a container-open packet carries no room for arbitrary extra text, and the recipient
 * field is purely a client-side convenience (the server never needs to know "this was a reply").
 */
public final class ComposeMailScreen extends AbstractContainerScreen<ComposeMailMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int MESSAGE_BOX_HEIGHT = 60;

    public static String pendingPrefillRecipient = "";

    private EditBox recipientBox;
    private MultiLineEditBox messageBox;

    public ComposeMailScreen(ComposeMailMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int boxLeft = leftPos;
        int boxWidth = imageWidth;
        int recipientY = topPos - 24 - MESSAGE_BOX_HEIGHT - 24;
        int messageY = topPos - 24 - MESSAGE_BOX_HEIGHT;

        recipientBox = new EditBox(font, boxLeft, recipientY, boxWidth, 18, Component.literal("Recipient"));
        recipientBox.setMaxLength(16);
        recipientBox.setValue(pendingPrefillRecipient);
        pendingPrefillRecipient = "";
        addRenderableWidget(recipientBox);

        messageBox = MultiLineEditBox.builder()
                .setX(boxLeft)
                .setY(messageY)
                .setPlaceholder(Component.literal("Write your message..."))
                .build(font, boxWidth, MESSAGE_BOX_HEIGHT, Component.literal("Message"));
        addRenderableWidget(messageBox);

        addRenderableWidget(Button.builder(Component.literal("Send"), b -> send())
                .bounds(leftPos + imageWidth - 70, topPos - 24, 70, 20).build());
    }

    private void send() {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(
                new SendLetterPayload(recipientBox.getValue(), messageBox.getValue())));
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        int rows = menu.getRowCount();
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, rows * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo + rows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }
}
