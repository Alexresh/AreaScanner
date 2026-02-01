package ru.obabok.areascanner.client.gui.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class NoMalilibScreen extends Screen {

    public NoMalilibScreen() {
        super(Text.literal("No Malilib"));
    }

    @Override
    protected void init() {
        super.init();
        Text noMalilibText = Text.literal("Malilib mod not found, unable to load");
        addDrawableChild(new TextWidget(width / 2 - textRenderer.getWidth(noMalilibText) / 2, height / 2, textRenderer.getWidth(noMalilibText), 20, noMalilibText, textRenderer));
        addDrawableChild(new ButtonWidget.Builder(Text.literal("Exit"), button -> {System.exit(0);}).dimensions(width / 2 - 40, height / 2 + 40, 80, 20).build());
    }
}
