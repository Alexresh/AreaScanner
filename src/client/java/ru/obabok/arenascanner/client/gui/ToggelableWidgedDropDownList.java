package ru.obabok.arenascanner.client.gui;

import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;

import java.util.List;


public class ToggelableWidgedDropDownList<T> extends WidgetDropDownList<T> {
    public boolean active = true;

    public ToggelableWidgedDropDownList(int x, int y, int width, int height, int maxHeight, int maxVisibleEntries, List<T> entries) {
        super(x, y, width, height, maxHeight, maxVisibleEntries, entries);
    }


    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(!active) return false;
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, double horizontalAmount, double verticalAmount) {
        if(!active) return false;
        return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int mouseButton) {
        if(!active) return;
        super.onMouseReleased(mouseX, mouseY, mouseButton);
    }
}
