package ru.obabok.arenascanner.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.obabok.arenascanner.client.util.References;
import ru.obabok.arenascanner.client.util.WhitelistsManager;

import java.util.List;

@Environment(EnvType.CLIENT)
public class WhitelistEditorScreen extends Screen {
    private final Screen parent;
    private final String filename;
    private TextFieldWidget blockInput;
    private final int currentPage;
    private static final int BLOCKS_PER_PAGE = 12;

    public WhitelistEditorScreen(Screen parent, String filename, int page) {
        super(Text.translatable("arenascanner.gui.title.whitelists.editor", filename.replace(".txt", "")));
        this.parent = parent;
        this.filename = filename.replace(".txt", "");
        this.currentPage = Math.max(0, page);
    }

    @Override
    protected void init() {
        List<Block> currentBlocks = WhitelistsManager.loadWhitelist(filename);
        if(currentBlocks == null){
            client.setScreen(parent);
            References.LOGGER.error("currentBlocks is null");
        }
        int totalPages = (currentBlocks.size() + BLOCKS_PER_PAGE - 1) / BLOCKS_PER_PAGE;

        int from = currentPage * BLOCKS_PER_PAGE;
        int to = Math.min(from + BLOCKS_PER_PAGE, currentBlocks.size());
        List<Block> pageBlocks = currentBlocks.subList(from, to);

        // Поле ввода блока
        blockInput = new TextFieldWidget(textRenderer, width / 2 - 100, 30, 150, 20, Text.empty());
        addDrawableChild(blockInput);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Block"), button -> {
            String idStr = blockInput.getText().trim();
            if (idStr.isEmpty()) return;
            try {
                Identifier id = Identifier.tryParse(idStr);
                if (id != null && Registries.BLOCK.containsId(id)) {
                    Block block = Registries.BLOCK.get(id);
                    WhitelistsManager.addToWhitelist(client.player, filename, block);
                    client.setScreen(new WhitelistEditorScreen(parent, filename, 0));
                }
            } catch (Exception e) {
                // игнорируем
            }
        }).dimensions(width / 2 + 55, 30, 60, 20).build());

        // Отображение блоков (с кнопками удаления)
        int y = 70;
        for (Block block : pageBlocks) {
            Identifier id = Registries.BLOCK.getId(block);
            addDrawableChild(ButtonWidget.builder(Text.literal(id.toString()), btn -> {})
                    .dimensions(width / 2 - 120, y, 200, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("❌"), btn -> {
                WhitelistsManager.removeFromWhitelist(client.player, filename, block);
                client.setScreen(new WhitelistEditorScreen(parent, filename, currentPage));
            }).dimensions(width / 2 + 85, y, 20, 20).build());

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

        if (to < currentBlocks.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn ->
                    client.setScreen(new WhitelistEditorScreen(parent, filename, currentPage + 1))
            ).dimensions(width / 2 + 40, buttonY, 80, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent))
                .dimensions(width / 2 - 40, height - 30, 80, 20).build());
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
