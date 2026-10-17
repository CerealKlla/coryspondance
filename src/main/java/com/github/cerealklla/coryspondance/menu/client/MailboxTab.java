package com.github.cerealklla.coryspondance.menu.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.cerealklla.coryspondance.client.ComposeMailScreen;
import com.github.cerealklla.coryspondance.mail.Letter;
import com.github.cerealklla.coryspondance.net.DeleteLetterPayload;
import com.github.cerealklla.coryspondance.net.OpenLetterPayload;
import com.github.cerealklla.coryspondance.net.RequestComposePayload;
import com.github.cerealklla.coryspondance.registration.ModAttachments;
import com.github.cerealklla.nuclyus.client.TextWrap;
import com.github.cerealklla.nuclyus.menu.client.NuclyusMenu;
import com.github.cerealklla.nuclyus.menu.client.WidgetHost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * "Mail" tab -- direct port of the former standalone {@code client.MailboxScreen} into the shared
 * Nuclyus menu, 2026-10-17. Behavior unchanged -- see that class's own removed doc for the full
 * design rationale (reads the inbox entirely off the already-synced {@code PlayerMail} attachment,
 * no network round trip just to open/browse). {@code client.MailboxScreen} itself is now a thin
 * host around this same tab, keeping the standalone "N" keybind working unchanged.
 *
 * <p>Unlike Myssions' Quest Log tab, "Claim Items"/"Write Letter"/"Reply" need no "return to host"
 * handling at all -- each just sends a request packet, and the **server** responds by opening a
 * real container menu ({@code mail.LetterMenu}/{@code ComposeMailMenu}), which replaces whatever
 * screen is open via vanilla's own menu-open mechanism. That transparently closes the Nuclyus menu
 * (or the standalone Mailbox) exactly as it always closed the old standalone screen -- nothing new
 * to handle, same as clicking open a chest always replaces whatever screen was open before it.
 */
public final class MailboxTab implements NuclyusMenu {

    private static final int MARGIN = 12;
    private static final int LIST_WIDTH_FRACTION_NUM = 3; // 30%
    private static final int FRACTION_DEN = 10;
    private static final int ROW_HEIGHT = 14;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int DIM_TEXT_COLOR = 0xFFA0A0A0;
    private static final int UNREAD_COLOR = 0xFFFFD700;
    private static final int HEADER_COLOR = 0xFFFFFFFF;
    private static final int BUTTON_ROW_HEIGHT = 20;

    private record ListRow(UUID letterId, String label, int color) {
    }

    private Screen host;
    private List<ListRow> listRows = List.of();
    private UUID selectedId;
    private int listScroll;
    private Button claimButton;
    private Button deleteButton;
    private Button replyButton;
    private Button writeLetterButton;

    @Override
    public Component title() {
        return Component.literal("Mail");
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public void attachHost(Screen host) {
        this.host = host;
    }

    private Rect2i area() {
        return host instanceof com.github.cerealklla.nuclyus.menu.client.NuclyusMenuScreen hostScreen
                ? hostScreen.contentArea()
                : new Rect2i(0, 0, host.width, host.height);
    }

    @Override
    public void onShown() {
        rebuildListRows();
        if (selectedId == null && !listRows.isEmpty()) {
            selectedId = listRows.get(0).letterId();
        }
        rebuildButtons();
        if (host instanceof WidgetHost widgetHost) {
            Rect2i area = area();
            writeLetterButton = widgetHost.addMenuWidget(Button.builder(Component.literal("Write Letter"), b -> openCompose(""))
                    .bounds(area.getX() + MARGIN, area.getY() + area.getHeight() - MARGIN - BUTTON_ROW_HEIGHT, 150, BUTTON_ROW_HEIGHT).build());
        }
    }

    @Override
    public void onHidden() {
        if (!(host instanceof WidgetHost widgetHost)) {
            return;
        }
        if (writeLetterButton != null) {
            widgetHost.removeMenuWidget(writeLetterButton);
            writeLetterButton = null;
        }
        removeDetailButtons(widgetHost);
    }

    private void rebuildListRows() {
        var mail = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_MAIL);
        List<ListRow> rows = new ArrayList<>();
        for (Letter letter : mail.letters()) {
            String prefix = letter.read() ? "" : "* ";
            String label = prefix + letter.displaySenderName() + (letter.attachments().isEmpty() ? "" : " [item]");
            rows.add(new ListRow(letter.id(), label, letter.read() ? TEXT_COLOR : UNREAD_COLOR));
        }
        listRows = rows;
    }

    private void removeDetailButtons(WidgetHost widgetHost) {
        if (claimButton != null) {
            widgetHost.removeMenuWidget(claimButton);
            claimButton = null;
        }
        if (deleteButton != null) {
            widgetHost.removeMenuWidget(deleteButton);
            deleteButton = null;
        }
        if (replyButton != null) {
            widgetHost.removeMenuWidget(replyButton);
            replyButton = null;
        }
    }

    private void rebuildButtons() {
        if (!(host instanceof WidgetHost widgetHost)) {
            return;
        }
        removeDetailButtons(widgetHost);
        Letter selected = selectedLetter();
        if (selected == null) {
            return;
        }
        Rect2i area = area();
        int detailsLeft = area.getX() + area.getWidth() * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN + MARGIN * 2;
        int buttonY = area.getY() + MARGIN;
        if (!selected.attachments().isEmpty()) {
            claimButton = widgetHost.addMenuWidget(Button.builder(Component.literal("Claim Items"), b -> claim(selected.id()))
                    .bounds(detailsLeft, buttonY, 120, BUTTON_ROW_HEIGHT).build());
        } else {
            deleteButton = widgetHost.addMenuWidget(Button.builder(Component.literal("Delete"), b -> delete(selected.id()))
                    .bounds(detailsLeft, buttonY, 120, BUTTON_ROW_HEIGHT).build());
        }
        if (selected.allowReply() && selected.fromPlayerId().isPresent()) {
            replyButton = widgetHost.addMenuWidget(Button.builder(Component.literal("Reply"), b -> openCompose(selected.displaySenderName()))
                    .bounds(detailsLeft + 130, buttonY, 120, BUTTON_ROW_HEIGHT).build());
        }
    }

    private Letter selectedLetter() {
        if (selectedId == null) {
            return null;
        }
        return Minecraft.getInstance().player.getData(ModAttachments.PLAYER_MAIL).find(selectedId).orElse(null);
    }

    private void claim(UUID letterId) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new OpenLetterPayload(letterId)));
    }

    private void delete(UUID letterId) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new DeleteLetterPayload(letterId)));
        selectedId = null;
        rebuildListRows();
        rebuildButtons();
    }

    private void openCompose(String prefillRecipient) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new RequestComposePayload()));
        ComposeMailScreen.pendingPrefillRecipient = prefillRecipient;
    }

    @Override
    public boolean mouseClicked(Rect2i area, MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int listLeft = area.getX() + MARGIN;
        int listRight = area.getX() + area.getWidth() * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        int top = area.getY() + MARGIN;
        int bottom = area.getY() + area.getHeight() - MARGIN - (BUTTON_ROW_HEIGHT + 8);
        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= top && mouseY <= bottom) {
            int clickedIndex = (int) ((mouseY - top + listScroll) / ROW_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < listRows.size()) {
                selectedId = listRows.get(clickedIndex).letterId();
                rebuildButtons();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(Rect2i area, double mouseX, double mouseY, double scrollX, double scrollY) {
        int listRight = area.getX() + area.getWidth() * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        if (mouseX <= listRight) {
            int maxScroll = Math.max(0, listRows.size() * ROW_HEIGHT - (area.getHeight() - MARGIN * 2 - (BUTTON_ROW_HEIGHT + 8)));
            listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) (scrollY * ROW_HEIGHT * 2)));
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Rect2i area, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int listLeft = area.getX() + MARGIN;
        int listRight = area.getX() + area.getWidth() * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        int detailsLeft = listRight + MARGIN;
        int detailsRight = area.getX() + area.getWidth() - MARGIN;
        int top = area.getY() + MARGIN;
        int bottom = area.getY() + area.getHeight() - MARGIN - (BUTTON_ROW_HEIGHT + 8);

        graphics.fill(listLeft, top, listRight, bottom, 0x40000000);
        graphics.enableScissor(listLeft, top, listRight, bottom);
        int y = top - listScroll;
        for (ListRow row : listRows) {
            if (y + ROW_HEIGHT >= top && y <= bottom) {
                boolean selected = row.letterId().equals(selectedId);
                if (selected) {
                    graphics.fill(listLeft, y, listRight, y + ROW_HEIGHT, 0x60FFFFFF);
                }
                graphics.text(font, Component.literal(row.label()), listLeft + 3, y + 3, row.color(), false);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        Letter selected = selectedLetter();
        if (selected != null) {
            int detailsTop = top + 26;
            int wrapWidth = detailsRight - detailsLeft - 6;
            int lineHeight = font.lineHeight + 2;
            int dy = detailsTop;
            dy = drawWrapped(graphics, font, "From: " + selected.displaySenderName(), detailsLeft, dy, wrapWidth, HEADER_COLOR, lineHeight);
            dy += 4;
            dy = drawWrapped(graphics, font, selected.message(), detailsLeft, dy, wrapWidth, TEXT_COLOR, lineHeight);
            dy += 4;
            if (!selected.attachments().isEmpty()) {
                drawWrapped(graphics, font, selected.attachments().size() + " attachment(s) waiting to be claimed.", detailsLeft, dy, wrapWidth, DIM_TEXT_COLOR, lineHeight);
            }
        }
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int wrapWidth, int color, int lineHeight) {
        for (String line : TextWrap.wrap(font, text, wrapWidth)) {
            graphics.text(font, Component.literal(line), x, y, color, false);
            y += lineHeight;
        }
        return y;
    }
}
