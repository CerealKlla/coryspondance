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
 * grid below), plus a recipient box, a message box, and a Send button placed in a dedicated extra
 * area below the vanilla chest/inventory box.
 *
 * <p><b>Fixed 2026-10-12, real live report</b>: the first version placed those three widgets at
 * negative y-offsets above {@code topPos} (the same trick Yconomics' {@code bag.client.LootBagScreen}
 * uses for its single Take All button), which only has room on a tall enough window -- at the user's
 * own resolution it ran off the top of the screen entirely. The real fix is to size this screen's own
 * {@code imageHeight} to actually include that space (so {@code topPos} -- which centers the *whole*
 * window -- accounts for it), not to keep hoping there's free space above an unrelated vanilla box.
 * The extra widgets now live *below* the chest box instead (in the newly added room), which also
 * sidesteps the same problem from ever recurring regardless of window size.
 */
public final class ComposeMailScreen extends AbstractContainerScreen<ComposeMailMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int MESSAGE_BOX_HEIGHT = 60;
    private static final int WIDGET_GAP = 6;
    private static final int SEND_BUTTON_HEIGHT = 20;
    // Top/bottom padding around the extra widget area, plus the recipient box's own 18px height.
    private static final int TEXT_AREA_HEIGHT = 10 + 18 + WIDGET_GAP + MESSAGE_BOX_HEIGHT + WIDGET_GAP + SEND_BUTTON_HEIGHT + 10;

    public static String pendingPrefillRecipient = "";

    private final int chestBoxHeight;
    private EditBox recipientBox;
    private MultiLineEditBox messageBox;

    public ComposeMailScreen(ComposeMailMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18 + TEXT_AREA_HEIGHT);
        this.chestBoxHeight = 114 + menu.getRowCount() * 18;
        this.inventoryLabelY = this.chestBoxHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int boxLeft = leftPos;
        int boxWidth = imageWidth;
        int recipientY = topPos + chestBoxHeight + 10;
        int messageY = recipientY + 18 + WIDGET_GAP;
        int buttonY = messageY + MESSAGE_BOX_HEIGHT + WIDGET_GAP;

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
                .bounds(leftPos + imageWidth - 70, buttonY, 70, SEND_BUTTON_HEIGHT).build());
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
