package ru.obabok.areascanner.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.obabok.areascanner.client.util.WhitelistManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class WhitelistSelectorScreen extends Screen {
    private final Screen parent;
    private final int currentPage;
    private static final int ENTRIES_PER_PAGE = 12;

    public WhitelistSelectorScreen(Screen parent, int page) {
        super(Text.translatable("areascanner.gui.title.whitelists"));
        this.parent = parent;
        this.currentPage = Math.max(0, page);
    }
    public static List<String> getWhitelistFilenames() {
        File dir = new File(WhitelistManager.stringWhitelistsPath);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        List<String> whitelistFiles = getWhitelistFilenames();
        int totalPages = (whitelistFiles.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE;

        int from = currentPage * ENTRIES_PER_PAGE;
        int to = Math.min(from + ENTRIES_PER_PAGE, whitelistFiles.size());
        List<String> pageFiles = whitelistFiles.subList(from, to);

        int y = 30;
        // Поле для создания нового вайтлиста
        TextFieldWidget newWhitelistField = new TextFieldWidget(textRenderer, width / 2 - 100, y, 150, 20, Text.empty());
        addDrawableChild(newWhitelistField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent))
                .dimensions( 30, height - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), button -> {
            String name = newWhitelistField.getText().trim();
            if (!name.isEmpty()) {
                if(WhitelistManager.createWhitelist(name) && client != null && client.player != null){
                    client.player.sendMessage(Text.literal("Created").formatted(Formatting.GOLD), true);
                    client.setScreen(new WhitelistSelectorScreen(parent, 0));
                }
            }
        }).dimensions(width / 2 + 55, y, 60, 20).build());


        y += 25;
        for (String filename : pageFiles) {
            String displayName = filename.replaceFirst("\\.json", "");
            addDrawableChild(ButtonWidget.builder(Text.literal(displayName), button -> {
                if(client != null) client.setScreen(new WhitelistEditorScreen(this, filename, 0));
            }).dimensions(width / 2 - 100, y, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("❌"), button -> {
                if(WhitelistManager.deleteWhitelist(filename) && client != null && client.player != null) {
                    client.player.sendMessage(Text.literal("Deleted").formatted(Formatting.GOLD), true);
                    client.setScreen(new WhitelistSelectorScreen(parent, currentPage));
                }
            }).dimensions(width / 2 + 55, y, 20, 20).build());

            y += 23;
        }
        int buttonY = height - 60;
        if (currentPage > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn ->{
                    if(client != null) client.setScreen(new WhitelistSelectorScreen(parent, currentPage - 1));
            }).dimensions(width / 2 - 120, buttonY, 80, 20).build());
        }

        addDrawableChild(new TextWidget(
                width / 2 - 40, buttonY,
                80, 20,
                Text.literal("Page " + (currentPage + 1) + "/" + Math.max(1, totalPages)),textRenderer));

        if (to < whitelistFiles.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn ->{
                    if(client != null) client.setScreen(new WhitelistSelectorScreen(parent, currentPage + 1));
            }).dimensions(width / 2 + 40, buttonY, 80, 20).build());
        }

    }

    @Override
    public boolean shouldPause() {
        return false;
    }



    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }
}
