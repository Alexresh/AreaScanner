package ru.obabok.areascanner.client.gui;

import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.client.Scan;
import ru.obabok.areascanner.client.models.ScreenPlus;
import ru.obabok.areascanner.client.models.Whitelist;
import ru.obabok.areascanner.client.util.WhitelistManager;
import ru.obabok.areascanner.client.models.WhitelistItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhitelistEditorScreen extends ScreenPlus {

    Screen parent;
    private final ArrayList<WhitelistItem> current_whitelist;
    private final int currentPage;
    private static final int BLOCKS_PER_PAGE = 12;
    private TextFieldWidget blockInput;
    private final String filename;
    private final WhitelistItem createdWhitelistItem = new WhitelistItem(null, null, null, null);
    private ButtonWidget addToWhitelistBtn;
    private static final List<String> waterloggedValues = List.of("true", "false");
    public static List<String> pistonBehaviorValues = Arrays.stream(Scan.PistonBehavior.values()).map(Enum::toString).toList();


    private ToggelableWidgedDropDownList<String> waterloggedWidget;
    private ToggelableWidgedDropDownList<String> pistonBehaviorWidget;
    private ToggelableWidgedDropDownList<String> comparisonOperatorsWidget;
    private ToggelableWidgedDropDownList<String> equalsOperatorsWidget;
    private TextFieldWidget blastResistanceValue;

    protected WhitelistEditorScreen(Screen parent, String filename, int page) {
        super(Text.literal("WhitelistEditorScreen"));
        this.parent = parent;
        this.currentPage = Math.max(0, page);
        this.filename = filename;
        Whitelist whitelist = WhitelistManager.loadData(filename);
        if(whitelist == null){
            whitelist = new Whitelist(new ArrayList<>());
            WhitelistManager.saveData(whitelist, filename);
        }
        current_whitelist = whitelist.whitelist;
    }

    @Override
    protected void init() {
        super.init();
        int totalPages = (current_whitelist.size() + BLOCKS_PER_PAGE - 1) / BLOCKS_PER_PAGE;
        int from = currentPage * BLOCKS_PER_PAGE;
        int to = Math.min(from + BLOCKS_PER_PAGE, current_whitelist.size());
        List<WhitelistItem> pageBlocks = current_whitelist.subList(from, to);

        int rowHeight = 60;
        int y = 30;

        //block input
        addDrawableChild(new TextWidget(30, y, 120, 20, Text.literal("Block"), textRenderer).alignLeft());
        blockInput = new TextFieldWidget(textRenderer, 130, y, 100, 20, Text.empty());
        addDrawableChild(blockInput);

        y+=rowHeight;
        //waterlogged
        addDrawableChild(new TextWidget(30, y, 70, 20, Text.literal("Waterlogged"), textRenderer).alignLeft());
        waterloggedWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, waterloggedValues);
        waterloggedWidget.setZLevel(100);
        addWidget(waterloggedWidget);

        y+=rowHeight;
        //pistonBehavior
        addDrawableChild(new TextWidget(30, y, 120, 20, Text.literal("Piston behavior"), textRenderer).alignLeft());
        equalsOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, Scan.equalsOperatorsValues);
        equalsOperatorsWidget.setZLevel(100);
        addWidget(equalsOperatorsWidget);
        pistonBehaviorWidget = new ToggelableWidgedDropDownList<>(190, y, 70, 20, 60, 2, pistonBehaviorValues);
        pistonBehaviorWidget.setZLevel(100);
        addWidget(pistonBehaviorWidget);

        y+=rowHeight;
        //blastResistance
        addDrawableChild(new TextWidget(30, y, 120, 20, Text.literal("Blast resistance"), textRenderer).alignLeft());
        comparisonOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 100, 4, Scan.comparisonOperatorsValues);
        comparisonOperatorsWidget.setZLevel(100);
        addWidget(comparisonOperatorsWidget);
        blastResistanceValue = new TextFieldWidget(textRenderer, 190, y, 30, 20, Text.empty());
        blastResistanceValue.setChangedListener(s -> {

        });
        addDrawableChild(blastResistanceValue);

        y = 30;
        int i = 0;
        for (WhitelistItem item : pageBlocks) {
            i++;
            addDrawableChild(ButtonWidget.builder(Text.literal("❌"), btn -> {
                WhitelistManager.removeFromWhitelist(filename, item);
                client.setScreen(new WhitelistEditorScreen(parent, filename, currentPage));
            }).dimensions(280, y, 20, 20).build());
            TextWidget widget = new TextWidget(310, y, 120, 20,Text.literal("Condition " + i + "     OR"),textRenderer).alignLeft();

            String builder = (item.block == null ? "-\n" : item.block + " AND\n") +
                    (item.waterlogged == null ? "-\n" : "Waterlogged: " + item.waterlogged + " AND\n") +
                    (item.pistonBehavior == null ? "-\n" : "Piston behavior: " + item.pistonBehavior + " AND\n") +
                    (item.blastResistance == null ? "-" : "Blast resistance: " + item.blastResistance);
            widget.setTooltip(Tooltip.of(Text.literal(builder)));
            addDrawableChild(widget);
            y += 23;
        }

        //presets
        addDrawableChild(new TextWidget(width - 130, 10, 100, 20, Text.literal("Presets"), textRenderer).alignRight());

        List<Whitelist> list = new ArrayList<>();
        Whitelist worldEater = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem(null, null, ">9", "≠DESTROY"));
        }}, "World eater");
        Whitelist fluidsAndWaterlogged = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem(Blocks.LAVA, null, null, null));
            add(new WhitelistItem(Blocks.WATER, null, null, null));
            add(new WhitelistItem(null, "true", null, null));
        }}, "Fluids and Waterlogged");
        Whitelist quarry = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem(null, null, null, "=IMMOVABLE"));
            add(new WhitelistItem(null, null, ">9", "≠DESTROY"));
        }}, "Quarry");

        list.add(worldEater);
        list.add(fluidsAndWaterlogged);
        list.add(quarry);
        ToggelableWidgedDropDownList<Whitelist> presets = new ToggelableWidgedDropDownList<>(width - 180, 30, 150, 20, 100, 5, list);
        addWidget(presets);
        addDrawableChild(ButtonWidget.builder(Text.literal("Use preset"), btn -> {
            if(presets.getSelectedEntry() != null){
                current_whitelist.addAll(presets.getSelectedEntry().whitelist);
                WhitelistManager.saveData(new Whitelist(current_whitelist), filename);
                client.setScreen(new WhitelistEditorScreen(parent, filename, 0));
            }
        }).dimensions(width - 110, 115, 80, 20).build());


        int buttonY = height - 40;
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
                .dimensions(30, height - 30, 80, 20).build());

        addToWhitelistBtn = ButtonWidget.builder(Text.literal("Add to whitelist"), btn -> {
                    if(validateCreatedWhitelistItem()){
                        current_whitelist.add(createdWhitelistItem);
                        WhitelistManager.saveData(new Whitelist(current_whitelist), filename);
                        client.setScreen(new WhitelistEditorScreen(parent, filename, 0));
                    }
                }).dimensions(30, 260, 90, 20).build();
        addDrawableChild(addToWhitelistBtn);

    }

    private boolean validateCreatedWhitelistItem(){
        return createdWhitelistItem.block != null || createdWhitelistItem.waterlogged != null || createdWhitelistItem.pistonBehavior != null || createdWhitelistItem.blastResistance != null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //block
        try {
            if(!blockInput.getText().isEmpty() && Registries.BLOCK.containsId(Identifier.of(blockInput.getText()))){
                createdWhitelistItem.block = Registries.BLOCK.get(Identifier.of(blockInput.getText()));
            }else createdWhitelistItem.block = null;
        }catch (Exception ignored){}
        //waterlogged
        if(waterloggedWidget.getSelectedEntry() != null){
            createdWhitelistItem.waterlogged = waterloggedWidget.getSelectedEntry();
        }else createdWhitelistItem.waterlogged = null;

        //pistonBehavior
        if(equalsOperatorsWidget.getSelectedEntry() != null && pistonBehaviorWidget.getSelectedEntry() != null){
            createdWhitelistItem.pistonBehavior = equalsOperatorsWidget.getSelectedEntry() + pistonBehaviorWidget.getSelectedEntry();
        }else createdWhitelistItem.pistonBehavior = null;

        //blast resistance
        if(comparisonOperatorsWidget.getSelectedEntry() != null && !blastResistanceValue.getText().isEmpty()){
            createdWhitelistItem.blastResistance = comparisonOperatorsWidget.getSelectedEntry() + blastResistanceValue.getText();
        }else createdWhitelistItem.blastResistance = null;

        //borders
        context.drawBorder(20,20, 245, 265, Colors.LIGHT_GRAY);
        context.drawBorder(275,20, 130, height - 80, Colors.LIGHT_GRAY);

        addToWhitelistBtn.active = validateCreatedWhitelistItem();
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, Text.literal(createdWhitelistItem.toString()), 30,240, Colors.WHITE, true);
    }
}
