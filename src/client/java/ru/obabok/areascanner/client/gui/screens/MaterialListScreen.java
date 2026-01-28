package ru.obabok.areascanner.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import ru.obabok.areascanner.client.network.ClientNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialListScreen extends Screen {
    private static final Object2IntOpenHashMap<Block> materialList = new Object2IntOpenHashMap<>();
    private final Screen parent;

    private static final int PADDING = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_SIZE = 18;
    private static final int SCROLL_BAR_WIDTH = 8;

    private static List<Block> sortedBlocks;
    private double scrollOffset = 0.0;
    private boolean isDraggingScrollBar = false;
    private double lastMouseY = 0;

    private int contentWidth;
    private int contentHeight;
    private int left, top;

    public static long jobId;

    public MaterialListScreen(Screen parent, long _jobId) {
        super(Text.literal("Material list"));
        this.parent = parent;
        if(_jobId != 0){
            jobId = _jobId;
            ClientNetwork.requestMaterialList(_jobId);
        }
        updateList(materialList);
    }

    @Override
    protected void init() {
        super.init();

        contentWidth = 300;


        addDrawableChild(new ButtonWidget.Builder(Text.literal("Back"), button -> {
            client.setScreen(parent);
        }).dimensions(10, height - 30, 50, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);


        contentHeight = Math.min(sortedBlocks.size() * ROW_HEIGHT + PADDING * 2, this.height - 60);
        left = (this.width - contentWidth) / 2;
        top = (this.height - contentHeight) / 2;


        int availableHeight = contentHeight - PADDING * 2;
        int visibleRows = Math.max(1, availableHeight / ROW_HEIGHT);
        int maxScroll = Math.max(0, sortedBlocks.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Область контента
        int contentX = left + PADDING;
        int contentY = top + PADDING;
        int contentRight = left + contentWidth - PADDING - SCROLL_BAR_WIDTH;

        TextRenderer textRenderer = this.textRenderer;

        // Рисуем строки
        int startIndex = (int) scrollOffset;
        int endIndex = Math.min(startIndex + visibleRows + 1, sortedBlocks.size());

        for (int i = startIndex; i < endIndex; i++) {
            Block block = sortedBlocks.get(i);
            int count = materialList.getInt(block);
            int y = contentY + (i - startIndex) * ROW_HEIGHT;

            // Получаем ItemStack для отображения
            ItemStack stack = getDisplayStack(block);

            // Иконка
            context.drawItem(stack, contentX, y);
            String name;
            if(stack.isOf(Items.AIR)){
                name = block.getName().getString();
            }else {
                name = Text.translatable(stack.getItem().getTranslationKey()).getString();
            }
            context.drawText(textRenderer, name, contentX + ICON_SIZE + 4, y + (ROW_HEIGHT - textRenderer.fontHeight) / 2, Colors.WHITE, false);

            // Количество
            String countStr = String.valueOf(count);
            int countWidth = textRenderer.getWidth(countStr);
            context.drawText(textRenderer, countStr, contentRight - countWidth, y + (ROW_HEIGHT - textRenderer.fontHeight) / 2, Colors.YELLOW, false);
        }

        // Рамка вокруг контента
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawBorder(left - 1, top - 1, contentWidth + 2, contentHeight + 2, 0xFFAAAAAA);

        // Полоса прокрутки
        if (sortedBlocks.size() > visibleRows) {
            int totalHeight = sortedBlocks.size() * ROW_HEIGHT;
            int visibleHeight = visibleRows * ROW_HEIGHT;
            int barPixels = Math.max(8, visibleHeight * visibleHeight / totalHeight);
            int scrollableHeight = visibleHeight - barPixels;

            // Позиция ползунка
            double ratio = maxScroll > 0 ? scrollOffset / maxScroll : 0.0;
            int barY = top + PADDING + (int) (ratio * scrollableHeight);


            // Фон полосы
            context.fill(left + contentWidth - SCROLL_BAR_WIDTH, top + PADDING,
                    left + contentWidth, top + PADDING + visibleHeight,
                    0xFF404040);
            // Ползунок
            context.fill(left + contentWidth - SCROLL_BAR_WIDTH, barY,
                    left + contentWidth, barY + barPixels,
                    isDraggingScrollBar ? 0xFFAAAAAA : 0xFF666666);
        }

        super.render(context, mouseX, mouseY, delta);
    }



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isPointInsideContent(mouseX, mouseY)) {
            int visibleRows = (contentHeight - PADDING * 2) / ROW_HEIGHT;
            if (sortedBlocks.size() > visibleRows) {
                scrollOffset -= verticalAmount;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    protected void applyBlur(){

    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isScrollBarHovered(mouseX, mouseY)) {
            isDraggingScrollBar = true;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollBar) {
            int visibleRows = (contentHeight - PADDING * 2) / ROW_HEIGHT;
            int maxScroll = Math.max(0, sortedBlocks.size() - visibleRows);
            if (maxScroll > 0) {
                double dy = mouseY - lastMouseY;
                double ratio = dy / 200;//(contentHeight - PADDING * 2);
                scrollOffset += ratio * maxScroll;
                lastMouseY = mouseY;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public static void updateList(Map<Block, Integer> blocks){
        sortedBlocks = new ArrayList<>(blocks.keySet());
        sortedBlocks.sort((a, b) -> {
            String nameA = Registries.BLOCK.getId(a).toString();
            String nameB = Registries.BLOCK.getId(b).toString();
            return nameA.compareTo(nameB);

        });
        blocks.forEach(materialList::addTo);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputUtil.GLFW_KEY_ESCAPE){
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private ItemStack getDisplayStack(Block block) {
        return Registries.ITEM.get(Registries.BLOCK.getId(block)).getDefaultStack();
    }

    private boolean isPointInsideContent(double x, double y) {
        return x >= left && x <= left + contentWidth - SCROLL_BAR_WIDTH &&
                y >= top && y <= top + contentHeight;
    }

    private boolean isScrollBarHovered(double x, double y) {
        return x >= left + contentWidth - SCROLL_BAR_WIDTH && x <= left + contentWidth &&
                y >= top && y <= top + contentHeight;
    }

    public static void clear(){
        materialList.clear();
        materialList.trim();
    }
    public static void addBlock(Block block, int count){
        materialList.addTo(block, count);
    }

}
