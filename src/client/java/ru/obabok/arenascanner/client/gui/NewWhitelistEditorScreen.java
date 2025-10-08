package ru.obabok.arenascanner.client.gui;

import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.obabok.arenascanner.client.models.ScreenPlus;
import ru.obabok.arenascanner.client.models.Whitelist;
import ru.obabok.arenascanner.client.util.NewWhitelistManager;
import ru.obabok.arenascanner.client.models.WhitelistItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NewWhitelistEditorScreen extends ScreenPlus {

    Screen parent;
    private ArrayList<WhitelistItem> current_whitelist;
    private final int currentPage;
    private static final int BLOCKS_PER_PAGE = 12;
    private TextFieldWidget blockInput;
    private final String filename;
    private WhitelistItem createdWhitelistItem = new WhitelistItem(null, null, null, null);
    private ButtonWidget addToWhitelistBtn;
    private static final List<String> waterloggedValues = List.of("true", "false");
    private static final List<String> pistonBehaviorValues = Stream.of(PistonBehavior.values()).map(Enum::toString).toList();
    private static final List<String> comparisonOperatorsValues = List.of("==", "!=", "> ", "< ", ">=", "<=");
    private static final List<String> equalsOperatorsValues = List.of("==", "!=");

    private ToggelableWidgedDropDownList<String> waterloggedWidget;
    private ToggelableWidgedDropDownList<String> pistonBehaviorWidget;
    private ToggelableWidgedDropDownList<String> comparisonOperatorsWidget;
    private ToggelableWidgedDropDownList<String> equalsOperatorsWidget;
    private TextFieldWidget blastResistanceValue;

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
        equalsOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, equalsOperatorsValues);
        equalsOperatorsWidget.setZLevel(100);
        addWidget(equalsOperatorsWidget);
        pistonBehaviorWidget = new ToggelableWidgedDropDownList<>(190, y, 70, 20, 60, 2, pistonBehaviorValues);
        pistonBehaviorWidget.setZLevel(100);
        addWidget(pistonBehaviorWidget);

        y+=rowHeight;
        //blastResistance
        addDrawableChild(new TextWidget(30, y, 120, 20, Text.literal("Blast resistance"), textRenderer).alignLeft());
        comparisonOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, comparisonOperatorsValues);
        comparisonOperatorsWidget.setZLevel(100);
        addWidget(comparisonOperatorsWidget);
        blastResistanceValue = new TextFieldWidget(textRenderer, 190, y, 30, 20, Text.empty()); //todo add input verifier
        blastResistanceValue.setChangedListener(s -> {

        });
        addDrawableChild(blastResistanceValue);

        y = 70;
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
                .dimensions(30, height - 30, 80, 20).build());

        addToWhitelistBtn = ButtonWidget.builder(Text.literal("Add to whitelist"), btn -> {
                    if(validateCreatedWhitelistItem()){
                        current_whitelist.add(createdWhitelistItem);
                        NewWhitelistManager.saveData(new Whitelist(current_whitelist), filename);
                        client.setScreen(new NewWhitelistEditorScreen(parent, filename, 0));
                    }
                }).dimensions(120, height - 30, 120, 20).build();
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


        addToWhitelistBtn.active = validateCreatedWhitelistItem();
        context.drawText(textRenderer, Text.literal(createdWhitelistItem.toString()), 30,height - 50, Colors.WHITE, true);
        super.render(context, mouseX, mouseY, delta);
    }
}
