package ru.obabok.arenascanner.client.gui;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
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

@Environment(EnvType.CLIENT)
public class ScanTaskScreen extends Screen {
    private final Screen parent;
    WidgetDropDownList<String> whitelistSelectorList;
    private final BlockBox initialBox;
    private TextFieldWidget minX, minY, minZ;
    private TextFieldWidget maxX, maxY, maxZ;
    private final List<CoordButton> coordButtons = new ArrayList<>();
    protected WidgetBase hoveredWidget = null;
    private final List<WidgetBase> widgets = new ArrayList<>();
    private ButtonWidget startButton;

    protected ScanTaskScreen(Screen parent) {
        super(Text.literal("Scan task screen"));
        this.parent = parent;
        if(MinecraftClient.getInstance().player == null) MinecraftClient.getInstance().setScreen(parent);
        this.initialBox = ScanCommand.getRange() == null ? new BlockBox(MinecraftClient.getInstance().player.getBlockPos()) : ScanCommand.getRange();
        if(ScanCommand.getRange() != initialBox){
            ScanCommand.setRange(initialBox);
        }
    }

    @Override
    public void init() {
        super.init();


        //create whitelists selector
        List<String> whitelistFiles = WhitelistSelectorScreen.getWhitelistFilenames();
        ArrayList<String> list = new ArrayList<>();
        list.add("WorldEater");
        list.addAll((whitelistFiles).stream().map(s -> s.replace(".txt", "")).toList());


        whitelistSelectorList = new WidgetDropDownList<>(20,200,160,18,200,10, list);
        whitelistSelectorList.setZLevel(100);
        addWidget(whitelistSelectorList);
        if(ScanCommand.getCurrentFilename() != null){
            whitelistSelectorList.setSelectedEntry(ScanCommand.getCurrentFilename().isEmpty() ? "WorldEater" : ScanCommand.getCurrentFilename());}




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
        addDrawableChild(new TextWidget(20, 180, 40, 20, Text.literal("Whitelist"), textRenderer));


        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent)).dimensions( 30, height - 30, 50, 20).build());
        startButton = ButtonWidget.builder(Text.literal("Start scan"), btn -> {
            try {
                ScanCommand.worldEaterMode = whitelistSelectorList.getSelectedEntry().equals("WorldEater");
                ScanCommand.executeAsync(client.world, ScanCommand.getRange(), whitelistSelectorList.getSelectedEntry().equals("WorldEater") ? "" : whitelistSelectorList.getSelectedEntry());
                this.close();
            }catch (Exception ignored){};
        }).dimensions( 90, height - 30, 80, 20).build();
        startButton.active = whitelistSelectorList.getSelectedEntry() != null && !whitelistSelectorList.getSelectedEntry().isEmpty() && ScanCommand.getRange() != null;
        addDrawableChild(startButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop scan"), btn ->{ScanCommand.stopScan(); client.setScreen(parent);}).dimensions(180, height - 30, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save state"), btn -> {ScanCommand.saveState(); this.close(); }).position(270, height - 30).size(90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load state"), btn -> {ScanCommand.loadState(); this.close();}).position(370, height - 30).size(90, 20).build());
    }


    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int y = height - 100;
        int x = 40;
        if(ScanCommand.getProcessing()){
            context.drawTextWithShadow(textRenderer,Text.literal("Scan in process..."), x, y, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("All chunks: " + ScanCommand.getAllChunksCounter()), x, y += 15, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("Unchecked chunks: " + ScanCommand.unloadedChunks.size()), x, y += 15, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("Percent left: " + String.format("%.2f", (double)ScanCommand.unloadedChunks.size() / ScanCommand.getAllChunksCounter() * 100)), x, y+=15, Colors.WHITE);
        }else{
            context.drawTextWithShadow(textRenderer,Text.literal("Scan is stopped"), x, y, Colors.WHITE);
        }
        this.drawWidgets(mouseX, mouseY, context);
        this.drawHoveredWidget(mouseX, mouseY, context);
        startButton.active = whitelistSelectorList.getSelectedEntry() != null && !whitelistSelectorList.getSelectedEntry().isEmpty() && ScanCommand.getRange() != null;
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

    public <T extends WidgetBase> T addWidget(T widget)
    {
        this.widgets.add(widget);
        return widget;
    }

    protected boolean shouldRenderHoverStuff()
    {
        return client.currentScreen == this;
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
                Integer.parseInt(s);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                int amount = (button == 0) ? 1 : (button == 1 ? -1 : 0);
                if (amount != 0) {
                    applyNudge(entry.field, amount);
                    return true;
                }
            }
        }
        onMouseClicked((int)mouseX, (int)mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                int amount = (int) Math.signum(verticalAmount);
                if (amount != 0) {
                    applyNudge(entry.field, amount);
                    return true;
                }
            }
        }
        for (WidgetBase widget : this.widgets)
        {
            if (widget.onMouseScrolled((int)mouseX, (int)mouseY, horizontalAmount, verticalAmount))
            {
                return true;
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

    private static class CoordButton {
        final ButtonWidget button;
        final TextFieldWidget field;

        CoordButton(ButtonWidget button, TextFieldWidget field) {
            this.button = button;
            this.field = field;
        }
    }
}
