package ru.obabok.arenascanner.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockBox;
import ru.obabok.arenascanner.client.ScanCommand;

import java.util.ArrayList;
import java.util.List;

public class AreaSelectionGui extends Screen {
    private BlockBox initialBox;
    private TextFieldWidget minX, minY, minZ;
    private TextFieldWidget maxX, maxY, maxZ;
    private final List<CoordButton> coordButtons = new ArrayList<>();

    public AreaSelectionGui(BlockBox box) {
        super(Text.literal("Edit BlockBox"));
        this.initialBox = box;
        if(ScanCommand.getRange() != initialBox){
            ScanCommand.setRange(initialBox);
        }
    }

    @Override
    protected void init() {
        int x = 20;
        int y = 20;
        int rowHeight = 30;


        //Corner1
        addDrawableChild(new TextWidget(x, y, 100, 20, Text.literal("Corner 1"), textRenderer).alignCenter());

        x+=20;
        //X
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("X:"), textRenderer).alignLeft());
        minX = createField(x, y, initialBox.getMinX());
        ButtonWidget btnMinX = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMinX, minX));
        //Y
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Y:"), textRenderer).alignLeft());
        minY = createField(x, y, initialBox.getMinY());
        ButtonWidget btnMinY = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMinY, minY));
        //Z
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Z:"), textRenderer).alignLeft());
        minZ = createField(x, y, initialBox.getMinZ());
        ButtonWidget btnMinZ = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMinZ, minZ));
        addDrawableChild(ButtonWidget.builder(Text.literal("Move to player"), btn -> moveToPlayer(true)).position(x - 15, y + 30).size(90, 20).build());

        x += 100;
        y = 20;

        //Corner2
        addDrawableChild(new TextWidget(x, y, 100, 20, Text.literal("Corner 2"), textRenderer).alignCenter());
        x+=20;
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("X:"), textRenderer).alignLeft());
        maxX = createField(x, y, initialBox.getMaxX());
        ButtonWidget btnMaxX = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxX, maxX));

        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Y:"), textRenderer).alignLeft());
        maxY = createField(x, y, initialBox.getMaxY());
        ButtonWidget btnMaxY = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxY, maxY));

        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Z:"), textRenderer).alignLeft());
        maxZ = createField(x, y, initialBox.getMaxZ());
        ButtonWidget btnMaxZ = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxZ, maxZ));
        addDrawableChild(ButtonWidget.builder(Text.literal("Move to player"), btn -> moveToPlayer(false)).position(x - 15, y + 30).size(90, 20).build());
    }

    @Override
    public void blur() {

    }

    private void moveToPlayer(boolean minCorner){
        if(client==null || client.player == null) return;
        if(minCorner){
            minX.setText(""+client.player.getBlockX());
            minY.setText(""+client.player.getBlockY());
            minZ.setText(""+client.player.getBlockZ());
        }else{
            maxX.setText(""+client.player.getBlockX());
            maxY.setText(""+client.player.getBlockY());
            maxZ.setText(""+client.player.getBlockZ());
        }
        updateRange();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    private TextFieldWidget createField(int x, int y, int value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 60, 18, Text.empty());
        field.setText(String.valueOf(value));
        field.setChangedListener(s -> {
            try {
                Integer.parseInt(s); // validate
                updateRange();
            } catch (NumberFormatException ignored) {}
        });
        addDrawableChild(field);
        return field;
    }

    private ButtonWidget createNudgeButton(int x, int y) {
        return addDrawableChild(ButtonWidget.builder(Text.literal("±"), button -> {})
                .position(x, y).size(18, 18).build());
    }

    // Обработка КЛИКОВ по кнопкам (левый/правый)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                int amount = (button == 0) ? 1 : (button == 1 ? -1 : 0); // ЛКМ = +1, ПКМ = -1
                if (amount != 0) {
                    applyNudge(entry.field, amount);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                int amount = (int) Math.signum(verticalAmount); // вверх = +1, вниз = -1
                if (amount != 0) {
                    applyNudge(entry.field, amount);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void applyNudge(TextFieldWidget field, int direction) {
        int step = 1;
        if (hasControlDown()) step *= 100;
        if (hasShiftDown()) step *= 10;
        if (hasAltDown()) step *= 5;
        int amount = direction * step;

        try {
            int currentValue = Integer.parseInt(field.getText());
            field.setText(String.valueOf(currentValue + amount));
            updateRange();
        } catch (NumberFormatException ignored) {}
    }

    private void updateRange() {
        int minXv = parseInt(minX.getText(), 0);
        int minYv = parseInt(minY.getText(), 0);
        int minZv = parseInt(minZ.getText(), 0);
        int maxXv = parseInt(maxX.getText(), 0);
        int maxYv = parseInt(maxY.getText(), 0);
        int maxZv = parseInt(maxZ.getText(), 0);

        // Нормализуем: min <= max по каждой оси
        int fMinX = Math.min(minXv, maxXv);
        int fMaxX = Math.max(minXv, maxXv);
        int fMinY = Math.min(minYv, maxYv);
        int fMaxY = Math.max(minYv, maxYv);
        int fMinZ = Math.min(minZv, maxZv);
        int fMaxZ = Math.max(minZv, maxZv);

        ScanCommand.setRange(new BlockBox(fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ));
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // Вспомогательный класс для привязки кнопки к полю
    private static class CoordButton {
        final ButtonWidget button;
        final TextFieldWidget field;

        CoordButton(ButtonWidget button, TextFieldWidget field) {
            this.button = button;
            this.field = field;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Опционально: рисуем подсказку при наведении на "±"
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawBorder(15,15, 110, 150, Colors.LIGHT_GRAY);
        context.drawBorder(135,15, 110, 150, Colors.LIGHT_GRAY);
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                List<Text> tooltip = List.of(
                        Text.literal("Left click: +1"),
                        Text.literal("Right click: -1"),
                        Text.literal("Scroll: adjust"),
                        Text.literal("Ctrl(±100) Shift(±10) Alt(±5)")
                );
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
}
