package com.github.cerealklla.coryspondance.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.cerealklla.coryspondance.mail.Letter;
import com.github.cerealklla.coryspondance.net.DeleteLetterPayload;
import com.github.cerealklla.coryspondance.net.OpenLetterPayload;
import com.github.cerealklla.coryspondance.net.RequestComposePayload;
import com.github.cerealklla.coryspondance.registration.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * The mailbox (explicit user spec, 2026-10-12: "a hotkey to open/close their mailbox") -- two
 * independently-scrollable panes, same hand-rolled-scrolling shape as Myssions' own {@code
 * QuestLogScreen} (no list widget exists anywhere in this suite). Reads the inbox entirely off the
 * client-local, already-synced {@code PlayerMail} attachment -- no network round trip just to open
 * this screen or read a letter's text, only to actually claim/delete/compose.
 */
public final class MailboxScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int LIST_WIDTH_FRACTION_NUM = 3; // 30%
    private static final int FRACTION_DEN = 10;
    private static final int ROW_HEIGHT = 14;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int DIM_TEXT_COLOR = 0xFFA0A0A0;
    private static final int UNREAD_COLOR = 0xFFFFD700;
    private static final int HEADER_COLOR = 0xFFFFFFFF;

    private record ListRow(UUID letterId, String label, int color) {
    }

    private List<ListRow> listRows = List.of();
    private UUID selectedId;
    private int listScroll;
    private Button claimButton;
    private Button deleteButton;
    private Button replyButton;

    public MailboxScreen() {
        super(Component.literal("Mailbox"));
    }

    @Override
    protected void init() {
        rebuildListRows();
        if (selectedId == null && !listRows.isEmpty()) {
            selectedId = listRows.get(0).letterId();
        }
        rebuildButtons();

        addRenderableWidget(Button.builder(Component.literal("Write Letter"), b -> openCompose(""))
                .bounds(MARGIN, height - MARGIN - 20, 150, 20).build());
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

    private void rebuildButtons() {
        if (claimButton != null) {
            removeWidget(claimButton);
            claimButton = null;
        }
        if (deleteButton != null) {
            removeWidget(deleteButton);
            deleteButton = null;
        }
        if (replyButton != null) {
            removeWidget(replyButton);
            replyButton = null;
        }
        Letter selected = selectedLetter();
        if (selected == null) {
            return;
        }
        int detailsLeft = width * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN + MARGIN * 2;
        int buttonY = MARGIN;
        if (!selected.attachments().isEmpty()) {
            claimButton = addRenderableWidget(Button.builder(Component.literal("Claim Items"), b -> claim(selected.id()))
                    .bounds(detailsLeft, buttonY, 120, 20).build());
        } else {
            deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> delete(selected.id()))
                    .bounds(detailsLeft, buttonY, 120, 20).build());
        }
        if (selected.allowReply() && selected.fromPlayerId().isPresent()) {
            replyButton = addRenderableWidget(Button.builder(Component.literal("Reply"), b -> openCompose(selected.displaySenderName()))
                    .bounds(detailsLeft + 130, buttonY, 120, 20).build());
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
        // Opening the real LetterMenu happens server-side (ServerPlayer#openMenu triggers the client
        // to switch screens automatically) -- this screen doesn't need to do anything else here.
    }

    private void delete(UUID letterId) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new DeleteLetterPayload(letterId)));
        selectedId = null;
        rebuildListRows();
        rebuildButtons();
    }

    private void openCompose(String prefillRecipient) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new RequestComposePayload()));
        // Server opens mail.ComposeMailMenu, which auto-switches the client's screen via vanilla's
        // own menu-open packet -- client.ComposeMailScreen reads this prefill off a static holder
        // set just before sending, see that screen's own doc.
        ComposeMailScreen.pendingPrefillRecipient = prefillRecipient;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int listLeft = MARGIN;
        int listRight = width * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        int top = MARGIN + 20;
        int bottom = height - MARGIN - 28;
        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= top && mouseY <= bottom) {
            int clickedIndex = (int) ((mouseY - top + listScroll) / ROW_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < listRows.size()) {
                selectedId = listRows.get(clickedIndex).letterId();
                rebuildButtons();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listRight = width * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        if (mouseX <= listRight) {
            int maxScroll = Math.max(0, listRows.size() * ROW_HEIGHT - (height - MARGIN * 2 - 48));
            listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) (scrollY * ROW_HEIGHT * 2)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int listLeft = MARGIN;
        int listRight = width * LIST_WIDTH_FRACTION_NUM / FRACTION_DEN;
        int detailsLeft = listRight + MARGIN;
        int detailsRight = width - MARGIN;
        int top = MARGIN + 20;
        int bottom = height - MARGIN - 28;

        graphics.text(font, "Mailbox", MARGIN, MARGIN, HEADER_COLOR);

        graphics.fill(listLeft, top, listRight, bottom, 0x40000000);
        graphics.enableScissor(listLeft, top, listRight, bottom);
        int y = top - listScroll;
        for (ListRow row : listRows) {
            if (y + ROW_HEIGHT >= top && y <= bottom) {
                boolean selected = row.letterId().equals(selectedId);
                if (selected) {
                    graphics.fill(listLeft, y, listRight, y + ROW_HEIGHT, 0x60FFFFFF);
                }
                graphics.text(font, row.label(), listLeft + 3, y + 3, row.color());
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
            dy = drawWrapped(graphics, "From: " + selected.displaySenderName(), detailsLeft, dy, wrapWidth, HEADER_COLOR, lineHeight);
            dy += 4;
            dy = drawWrapped(graphics, selected.message(), detailsLeft, dy, wrapWidth, TEXT_COLOR, lineHeight);
            dy += 4;
            if (!selected.attachments().isEmpty()) {
                drawWrapped(graphics, selected.attachments().size() + " attachment(s) waiting to be claimed.", detailsLeft, dy, wrapWidth, DIM_TEXT_COLOR, lineHeight);
            }
        }
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int wrapWidth, int color, int lineHeight) {
        for (String line : wrapToWidth(text, wrapWidth)) {
            graphics.text(font, line, x, y, color);
            y += lineHeight;
        }
        return y;
    }

    private List<String> wrapToWidth(String text, int wrapWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > wrapWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}
