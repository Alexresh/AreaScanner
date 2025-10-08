package ru.obabok.arenascanner.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.obabok.arenascanner.client.util.WhitelistsManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class WhitelistSelectorScreen extends Screen {
    private final Screen parent;
    private final int currentPage;
    private static final int ENTRIES_PER_PAGE = 12;
    public static String selectedWhitelist = "";
    public static boolean worldEaterMode;

    public WhitelistSelectorScreen(Screen parent, int page) {
        super(Text.translatable("arenascanner.gui.title.whitelists"));
        this.parent = parent;
        this.currentPage = Math.max(0, page);
    }
    public static List<String> getWhitelistFilenames() {
        File dir = new File(WhitelistsManager.stringWhitelistsPath);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        List<String> whitelistFiles = getWhitelistFilenames(); // нужно реализовать
        int totalPages = (whitelistFiles.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE;

        //whitelistFiles.sort(String::compareTo);
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
                WhitelistsManager.createWhitelist(client.player, name);
                client.setScreen(new WhitelistSelectorScreen(parent, 0));
            }
        }).dimensions(width / 2 + 55, y, 60, 20).build());


        y += 25;
        for (String filename : pageFiles) {
            String displayName = filename.replace(".txt", "");
            addDrawableChild(ButtonWidget.builder(Text.literal(displayName), button -> {
                client.setScreen(new WhitelistEditorScreen(this, filename, 0));
            }).dimensions(width / 2 - 100, y, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("❌"), button -> {
                WhitelistsManager.deleteWhitelist(client.player, filename.replace(".txt", ""));
                client.setScreen(new WhitelistSelectorScreen(parent, currentPage));
            }).dimensions(width / 2 + 55, y, 20, 20).build());

            y += 23;
        }
        int buttonY = height - 60;
        if (currentPage > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn ->
                    client.setScreen(new WhitelistSelectorScreen(parent, currentPage - 1))
            ).dimensions(width / 2 - 120, buttonY, 80, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Page " + (currentPage + 1) + "/" + Math.max(1, totalPages)),
                btn -> {}
        ).dimensions(width / 2 - 40, buttonY, 80, 20).build());

        if (to < whitelistFiles.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn ->
                    client.setScreen(new WhitelistSelectorScreen(parent, currentPage + 1))
            ).dimensions(width / 2 + 40, buttonY, 80, 20).build());
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
