package ru.obabok.arenascanner.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.obabok.arenascanner.client.util.NewWhitelistManager;
import ru.obabok.arenascanner.client.util.Whitelist;
import ru.obabok.arenascanner.client.util.WhitelistItem;
import ru.obabok.arenascanner.client.util.WhitelistsManager;

import java.util.ArrayList;
import java.util.List;

public class NewWhitelistEditorScreen extends Screen {

    Screen parent;
    private ArrayList<WhitelistItem> current_whitelist;
    private final int currentPage;
    private static final int BLOCKS_PER_PAGE = 12;
    private TextFieldWidget blockInput;
    private final String filename;

    protected NewWhitelistEditorScreen(Screen parent, String filename, int page) {
        super(Text.literal("WhitelistEditorScreen"));
        this.parent = parent;
        this.currentPage = Math.max(0, page);
        this.filename = filename;
        current_whitelist = NewWhitelistManager.loadData(filename).whitelist;

    }

    @Override
    protected void init() {
        super.init();
        int totalPages = (current_whitelist.size() + BLOCKS_PER_PAGE - 1) / BLOCKS_PER_PAGE;
        int from = currentPage * BLOCKS_PER_PAGE;
        int to = Math.min(from + BLOCKS_PER_PAGE, current_whitelist.size());
        List<WhitelistItem> pageBlocks = current_whitelist.subList(from, to);
        blockInput = new TextFieldWidget(textRenderer, 30, 30, 150, 20, Text.empty());
        addDrawableChild(blockInput);

        int y = 70;
        for (WhitelistItem item : pageBlocks) {
            //addDrawableChild(new TextWidget(width / 2 - 120, y, 200, 20,Text.literal(item.toString()),textRenderer));
            addDrawableChild(ButtonWidget.builder(Text.literal(item.toString()), btn -> {
                //todo
            }).dimensions(width / 2 - 150, y, 300, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("❌"), btn -> {
                NewWhitelistManager.removeFromWhitelist(filename, item);
                client.setScreen(new NewWhitelistEditorScreen(parent, filename, currentPage));
            }).dimensions(width / 2 + 145, y, 20, 20).build());

            y += 23;
        }

        int buttonY = height - 60;
        if (currentPage > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn ->
                    client.setScreen(new WhitelistEditorScreen(parent, filename, currentPage - 1))
            ).dimensions(width / 2 - 120, buttonY, 80, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Page " + (currentPage + 1) + "/" + Math.max(1, totalPages)),
                btn -> {}
        ).dimensions(width / 2 - 40, buttonY, 80, 20).build());

        if (to < current_whitelist.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn ->
                    client.setScreen(new WhitelistEditorScreen(parent, filename, currentPage + 1))
            ).dimensions(width / 2 + 40, buttonY, 80, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent))
                .dimensions(width / 2 - 40, height - 30, 80, 20).build());

    }
}
