package ru.obabok.arenascanner.client.models;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ScreenPlus extends Screen {
    protected final List<WidgetBase> widgets = new ArrayList<>();
    protected WidgetBase hoveredWidget = null;
    protected ScreenPlus(Text title) {
        super(title);
    }



    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.drawWidgets(mouseX, mouseY, context);
        this.drawHoveredWidget(mouseX, mouseY, context);
        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        boolean handled = false;
        for (WidgetBase widget : this.widgets)
        {
            if (widget.isMouseOver(mouseX, mouseY) && widget.onMouseClicked(mouseX, mouseY, mouseButton))
            {
                handled = true;
            }
        }
        return handled;
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (WidgetBase widget : this.widgets)
        {
            widget.onMouseReleased((int)mouseX, (int)mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void drawWidgets(int mouseX, int mouseY, DrawContext drawContext)
    {
        this.hoveredWidget = null;

        if (!this.widgets.isEmpty())
        {
            for (WidgetBase widget : this.widgets)
            {
                widget.render(mouseX, mouseY, false, drawContext);

                if (widget.isMouseOver(mouseX, mouseY))
                {
                    this.hoveredWidget = widget;
                }
            }
        }
    }

    protected boolean shouldRenderHoverStuff()
    {
        return client.currentScreen == this;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        onMouseClicked((int)mouseX, (int)mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (WidgetBase widget : this.widgets)
        {
            if (widget.onMouseScrolled((int)mouseX, (int)mouseY, horizontalAmount, verticalAmount))
            {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    protected void drawHoveredWidget(int mouseX, int mouseY, DrawContext drawContext)
    {
        if (!this.shouldRenderHoverStuff())
        {
            return;
        }

        if (this.hoveredWidget != null)
        {
            this.hoveredWidget.postRenderHovered(mouseX, mouseY, false, drawContext);
        }
    }

    public <T extends WidgetBase> T addWidget(T widget)
    {
        this.widgets.add(widget);
        return widget;
    }

}
