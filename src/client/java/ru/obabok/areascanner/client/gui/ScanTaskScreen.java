package ru.obabok.areascanner.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockBox;
import ru.obabok.areascanner.client.Scan;
import ru.obabok.areascanner.client.models.ScreenPlus;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ScanTaskScreen extends ScreenPlus {
    private final Screen parent;
    private ToggelableWidgedDropDownList<String> whitelistSelectorList;
    private final BlockBox initialBox;
    private TextFieldWidget minX, minY, minZ;
    private TextFieldWidget maxX, maxY, maxZ;
    private final List<CoordButton> coordButtons = new ArrayList<>();
    private ButtonWidget startButton;

    protected ScanTaskScreen(Screen parent) {
        super(Text.literal("Scan task screen"));
        this.parent = parent;
        if(MinecraftClient.getInstance().player == null) MinecraftClient.getInstance().setScreen(parent);
        this.initialBox = Scan.getRange() == null ? new BlockBox(MinecraftClient.getInstance().player.getBlockPos()) : Scan.getRange();
        if(Scan.getRange() != initialBox){
            Scan.setRange(initialBox);
        }
    }

    public ScanTaskScreen(Screen parent, BlockBox range){
        super(Text.literal("Scan task screen"));
        this.parent = parent;
        this.initialBox = range;
        Scan.setRange(initialBox);
    }

    @Override
    public void init() {
        super.init();

        //create whitelists selector
        List<String> whitelistFiles = WhitelistSelectorScreen.getWhitelistFilenames();
        ArrayList<String> list = new ArrayList<>();
        //list.add("WorldEater");
        list.addAll(whitelistFiles);

        whitelistSelectorList = new ToggelableWidgedDropDownList<>(20,200,160,18,200,10, list);
        whitelistSelectorList.setZLevel(100);
        whitelistSelectorList.active = !Scan.getProcessing();
        addWidget(whitelistSelectorList);
        if(Scan.getCurrentFilename() != null){
            whitelistSelectorList.setSelectedEntry(Scan.getCurrentFilename());
        }

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

        ButtonWidget moveToPlayer1 = ButtonWidget.builder(Text.literal("Move to player"), btn -> moveToPlayer(true)).position(x - 15, y + 30).size(90, 20).build();
        moveToPlayer1.active = !Scan.getProcessing();
        addDrawableChild(moveToPlayer1);

        x += 100;
        y = 20;

        //Corner2
        addDrawableChild(new TextWidget(x, y, 100, 20, Text.literal("Corner 2"), textRenderer).alignCenter());
        x+=20;
        //maxX
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("X:"), textRenderer).alignLeft());
        maxX = createField(x, y, initialBox.getMaxX());
        ButtonWidget btnMaxX = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxX, maxX));

        //maxY
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Y:"), textRenderer).alignLeft());
        maxY = createField(x, y, initialBox.getMaxY());
        ButtonWidget btnMaxY = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxY, maxY));

        //maxZ
        y+=rowHeight;
        addDrawableChild(new TextWidget(x - 15, y, 15, 20, Text.literal("Z:"), textRenderer).alignLeft());
        maxZ = createField(x, y, initialBox.getMaxZ());
        ButtonWidget btnMaxZ = createNudgeButton(x + 60, y);
        coordButtons.add(new CoordButton(btnMaxZ, maxZ));

        ButtonWidget moveToPlayer2 = ButtonWidget.builder(Text.literal("Move to player"), btn -> moveToPlayer(false)).position(x - 15, y + 30).size(90, 20).build();
        moveToPlayer2.active = !Scan.getProcessing();
        addDrawableChild(moveToPlayer2);

        //Whitelist text
        addDrawableChild(new TextWidget(20, 180, 40, 20, Text.literal("Whitelist"), textRenderer));

        //back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent)).dimensions( 30, height - 30, 50, 20).build());

        //start button
        startButton = ButtonWidget.builder(Text.literal("Start scan"), btn -> {
            try {
                //NewScan.worldEaterMode = whitelistSelectorList.getSelectedEntry().equals("WorldEater");
                Scan.executeAsync(client.world, Scan.getRange(), whitelistSelectorList.getSelectedEntry());
                this.close();
            }catch (Exception ignored){}
        }).dimensions( 90, height - 30, 80, 20).build();
        startButton.active = whitelistSelectorList.getSelectedEntry() != null && !whitelistSelectorList.getSelectedEntry().isEmpty() && Scan.getRange() != null;
        addDrawableChild(startButton);

        //if(ScanCommand.getProcessing())



        ButtonWidget stopScanBtn = ButtonWidget.builder(Text.literal("Stop scan"), btn ->{
            BlockBox box = Scan.getRange();
            Scan.stopScan();
            client.setScreen(new ScanTaskScreen(parent, box));
        }).dimensions(180, height - 30, 80, 20).build();
        stopScanBtn.active = Scan.getProcessing();
        addDrawableChild(stopScanBtn);

        ButtonWidget saveStateBtn = ButtonWidget.builder(Text.literal("Save state"), btn -> {
            Scan.saveState();
            this.close();
        }).position(270, height - 30).size(90, 20).build();
        saveStateBtn.active = Scan.getProcessing();
        addDrawableChild(saveStateBtn);

        ButtonWidget loadStateBtn = ButtonWidget.builder(Text.literal("Load state"), btn -> {
            client.player.sendMessage(Text.literal(Scan.loadState() ? "State loaded" : "State didn't load"), true);
            this.close();
        }).position(370, height - 30).size(90, 20).build();
        loadStateBtn.active = !Scan.getProcessing();
        addDrawableChild(loadStateBtn);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputUtil.GLFW_KEY_ESCAPE){
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int y = height - 100;
        int x = 40;
        if(Scan.getProcessing()){
            context.drawTextWithShadow(textRenderer,Text.literal("Scan in process..."), x, y, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("All chunks: " + Scan.getAllChunksCounter()), x, y += 15, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("Unchecked chunks: " + Scan.unloadedChunks.size()), x, y += 15, Colors.WHITE);
            context.drawTextWithShadow(textRenderer,Text.literal("Percent left: " + String.format("%.2f", (double) Scan.unloadedChunks.size() / Scan.getAllChunksCounter() * 100)), x, y+=15, Colors.WHITE);
        }else{
            context.drawTextWithShadow(textRenderer,Text.literal("Scan is stopped"), x, y, Colors.WHITE);
        }

        startButton.active = whitelistSelectorList.getSelectedEntry() != null && !whitelistSelectorList.getSelectedEntry().isEmpty() && Scan.getRange() != null;
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
        super.render(context, mouseX, mouseY, delta);
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


    private TextFieldWidget createField(int x, int y, int value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 60, 18, Text.empty());
        field.setText(String.valueOf(value));
        field.active = !Scan.getProcessing();
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
        ButtonWidget nudgeBtn = ButtonWidget.builder(Text.literal("±"), button -> {})
                .position(x, y).size(18, 18).build();
        nudgeBtn.active = !Scan.getProcessing();
        return addDrawableChild(nudgeBtn);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                playClickSound();
                int amount = (button == 0) ? 1 : (button == 1 ? -1 : 0);
                if (amount != 0) {
                    applyNudge(entry.field, amount);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound(){
        if(client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CoordButton entry : coordButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                playClickSound();
                int amount = (int) Math.signum(verticalAmount);
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
        int minXv = parseInt(minX.getText());
        int minYv = parseInt(minY.getText());
        int minZv = parseInt(minZ.getText());
        int maxXv = parseInt(maxX.getText());
        int maxYv = parseInt(maxY.getText());
        int maxZv = parseInt(maxZ.getText());

        int fMinX = Math.min(minXv, maxXv);
        int fMaxX = Math.max(minXv, maxXv);
        int fMinY = Math.min(minYv, maxYv);
        int fMaxY = Math.max(minYv, maxYv);
        int fMinZ = Math.min(minZv, maxZv);
        int fMaxZ = Math.max(minZv, maxZv);

        Scan.setRange(new BlockBox(fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ));
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
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
