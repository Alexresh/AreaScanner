package ru.obabok.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NoMalilibScreen extends Screen {
    public static boolean showed = false;
    private final Screen parent;
    public NoMalilibScreen(Screen parent) {
        super(Component.literal("No Malilib"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        showed = true;
        Component noMalilibText = Component.literal("Malilib mod not found, unable to load AreaScanner");
        addRenderableWidget(new StringWidget(width / 2 - font.width(noMalilibText) / 2, height / 2, font.width(noMalilibText), 20, noMalilibText, font));
        addRenderableWidget(Button.builder(Component.literal("Exit"), btn -> System.exit(0)).bounds(width / 2 - 90, height / 2 + 40, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Continue"), btn -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(width / 2 + 10, height / 2 + 40, 80, 20).build());
    }
}
