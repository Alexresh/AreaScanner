package ru.obabok.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SuggestionListWidget extends AbstractWidget {
    private final EditBox editBox;

    private List<String> suggestions = List.of();

    private int selected = -1;
    private int scroll = 0;

    private final int lineHeight = 12;
    private final int maxVisible = 8;

    public SuggestionListWidget(EditBox editBox, int x, int y, int width) {
        super(x, y, width, 0, Component.empty());
        this.editBox = editBox;
        this.visible = false;
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (!visible || suggestions.isEmpty()) {
            return;
        }

        int x = getX();
        int y = getY();
        int width = getWidth();

        int shown = Math.min(maxVisible, suggestions.size());
        int boxHeight = shown * lineHeight + 4;

        //background
        graphics.fill(x, y, x + width, y + boxHeight, 0xF0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF000000);
        graphics.fill(x, y + boxHeight - 1, x + width, y + boxHeight, 0xFF000000);

        var font = Minecraft.getInstance().font;

        for (int i = 0; i < shown; i++) {
            int index = scroll + i;

            if (index >= suggestions.size()) {
                break;
            }

            int entryY = y + 2 + i * lineHeight;

            boolean hovered = mouseX >= x &&
                              mouseX < x + width &&
                              mouseY >= entryY &&
                              mouseY < entryY + lineHeight;

            if (hovered || index == selected) {
                graphics.fill(x + 1, entryY, x + width - 1, entryY + lineHeight, hovered ? 0xFF555555 : 0xFF333333);
            }

            graphics.text(font, suggestions.get(index), x + 4, entryY + 2, index == selected ? 0xFFFFFFFF : 0xFFB0B0B0, false);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!visible || suggestions.isEmpty() || event.button() != 0) {
            return false;
        }

        int x = getX();
        int y = getY();
        int width = getWidth();

        int shown = Math.min(maxVisible, suggestions.size());

        if (!(event.x() >= x && event.x() < x + width && event.y() >= y && event.y() < y + 2 + shown * lineHeight)) {
            return false;
        }

        int relative = (int) (event.y() - y - 2);

        if (relative < 0) {
            return false;
        }

        int index = scroll + relative / lineHeight;

        if (index >= 0 && index < suggestions.size()) {
            apply(suggestions.get(index));
            return true;
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && super.isMouseOver(mouseX, mouseY);
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
        this.selected = suggestions.isEmpty() ? -1 : 0;
        this.scroll = 0;
        this.visible = !suggestions.isEmpty();
        this.setHeight(suggestions.isEmpty() ? 0 : Math.min(maxVisible, suggestions.size()) * lineHeight + 4);
    }

    public void hide() {
        this.suggestions = List.of();
        this.selected = -1;
        this.scroll = 0;
        this.visible = false;
        this.setHeight(0);
    }

    public boolean hasSelection() {
        return selected >= 0 && selected < suggestions.size();
    }

    public void selectRelative(int delta) {
        if (suggestions.isEmpty()) {
            return;
        }

        selected = Math.floorMod(selected + delta, suggestions.size());

        if (selected < scroll) {
            scroll = selected;
        }

        if (selected >= scroll + maxVisible) {
            scroll = selected - maxVisible + 1;
        }
    }

    public boolean confirmSelected() {
        if (!hasSelection()) {
            return false;
        }

        apply(suggestions.get(selected));
        return true;
    }

    private void apply(String value) {
        editBox.setValue(value);
        hide();
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {

    }
}
