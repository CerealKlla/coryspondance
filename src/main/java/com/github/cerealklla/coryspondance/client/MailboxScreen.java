package com.github.cerealklla.coryspondance.client;

import com.github.cerealklla.coryspondance.menu.client.MailboxTab;
import com.github.cerealklla.nuclyus.menu.client.WidgetHost;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/**
 * The mailbox's standalone top-level entry point -- opened via {@code
 * CoryspondanceModClient#OPEN_MAILBOX} ("N"). Thin host around {@link MailboxTab}, kept so the
 * mailbox still works exactly as before even without Nuclyus's shared "K" menu actually open --
 * see that tab's own doc for the full design rationale.
 */
public final class MailboxScreen extends Screen implements WidgetHost {

    private final MailboxTab tab = new MailboxTab();

    public MailboxScreen() {
        super(Component.literal("Mailbox"));
        tab.attachHost(this);
    }

    private Rect2i area() {
        return new Rect2i(0, 0, width, height);
    }

    @Override
    protected void init() {
        tab.onShown();
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addMenuWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public void removeMenuWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (tab.mouseClicked(area(), event, doubleClick)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return tab.mouseScrolled(area(), mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        tab.render(graphics, area(), mouseX, mouseY, partialTick);
    }
}
