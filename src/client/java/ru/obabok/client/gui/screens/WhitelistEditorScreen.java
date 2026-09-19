package ru.obabok.client.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import ru.obabok.client.Scan;
import ru.obabok.client.gui.widgets.SuggestionListWidget;
import ru.obabok.client.gui.widgets.ToggelableWidgedDropDownList;
import ru.obabok.client.models.ScreenPlus;
import ru.obabok.client.util.WhitelistManager;
import ru.obabok.common.BlockMatcher;
import ru.obabok.common.model.Whitelist;
import ru.obabok.common.model.WhitelistItem;

import java.util.*;

public class WhitelistEditorScreen extends ScreenPlus {

    Screen parent;
    private final ArrayList<WhitelistItem> current_whitelist;
    private final int currentPage;
    private static final int BLOCKS_PER_PAGE = 12;
    private EditBox blockInput;
    private final String filename;
    private final WhitelistItem createdWhitelistItem = new WhitelistItem(null, null, null, null);
    private Button addToWhitelistBtn;
    private static final List<String> waterloggedValues = List.of("true", "false");
    public static final List<String> pistonBehaviorValues = Arrays.stream(Scan.PistonBehavior.values()).map(Enum::toString).toList();
    private static final List<String> BLOCK_IDS = BuiltInRegistries.BLOCK.keySet()
            .stream()
            .map(Identifier::toString)
            .sorted(Comparator.comparing(id -> {
                int colon = id.indexOf(':');
                return colon >= 0 ? id.substring(colon + 1) : id;
            }))
            .toList();
    private SuggestionListWidget blockSuggestions;

    private ToggelableWidgedDropDownList<String> waterloggedWidget;
    private ToggelableWidgedDropDownList<String> pistonBehaviorWidget;
    private ToggelableWidgedDropDownList<String> blastResistanceComparisonOperatorsWidget;
    private ToggelableWidgedDropDownList<String> blockEqualsOperatorsWidget;
    private ToggelableWidgedDropDownList<String> pistonEqualsOperatorsWidget;
    private EditBox blastResistanceValue;

    protected WhitelistEditorScreen(Screen parent, String filename, int page) {
        super(Component.literal("WhitelistEditorScreen"));
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
        addRenderableWidget(new StringWidget(30, y, 60, 20, Component.literal("Block"), font));
        blockEqualsOperatorsWidget = new ToggelableWidgedDropDownList<>(90, y, 30, 20, 60, 2, BlockMatcher.EQUALS_OPERATORS);
        blockEqualsOperatorsWidget.setZLevel(100);
        blockEqualsOperatorsWidget.setSelectedEntry("=");
        addWidget(blockEqualsOperatorsWidget);

        blockInput = new EditBox(font, 130, y, 130, 20, Component.empty());
        blockInput.setMaxLength(256);
        blockInput.setResponder(this::onBlockTextChanged);
        addRenderableWidget(blockInput);

        int listWidth = 220;




        y+=rowHeight;
        //waterlogged
        addRenderableWidget(new StringWidget(30, y, 70, 20, Component.literal("Waterlogged"), font));
        waterloggedWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, waterloggedValues);
        waterloggedWidget.setZLevel(100);
        addWidget(waterloggedWidget);

        y+=rowHeight;
        //pistonBehavior
        addRenderableWidget(new StringWidget(30, y, 120, 20, Component.literal("Piston behavior"), font));
        pistonEqualsOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 60, 2, BlockMatcher.EQUALS_OPERATORS);
        pistonEqualsOperatorsWidget.setZLevel(100);
        addWidget(pistonEqualsOperatorsWidget);
        pistonBehaviorWidget = new ToggelableWidgedDropDownList<>(190, y, 70, 20, 60, 2, pistonBehaviorValues);
        pistonBehaviorWidget.setZLevel(100);
        addWidget(pistonBehaviorWidget);

        y+=rowHeight;
        //blastResistance
        addRenderableWidget(new StringWidget(30, y, 120, 20, Component.literal("Blast resistance"), font));
        blastResistanceComparisonOperatorsWidget = new ToggelableWidgedDropDownList<>(130, y, 50, 20, 100, 4, BlockMatcher.COMPARISON_OPERATORS);
        blastResistanceComparisonOperatorsWidget.setZLevel(100);
        addWidget(blastResistanceComparisonOperatorsWidget);
        blastResistanceValue = new EditBox(font, 190, y, 30, 20, Component.empty());
        blastResistanceValue.setResponder(s -> {

        });
        addRenderableWidget(blastResistanceValue);


        y = 30;
        int i = 0;
        for (WhitelistItem item : pageBlocks) {
            i++;
            addRenderableWidget(Button.builder(Component.literal("❌"), btn -> {
                WhitelistManager.removeFromWhitelist(filename, item);
                minecraft.setScreenAndShow(new WhitelistEditorScreen(parent, filename, currentPage));
            }).bounds(280, y, 20, 20).build());
            StringWidget widget = new StringWidget(310, y, 120, 20, Component.literal("Condition " + i + "     OR"), font);

            String builder = (item.block == null ? "-\n" : item.block + " AND\n") +
                    (item.waterlogged == null ? "-\n" : "Waterlogged: " + item.waterlogged + " AND\n") +
                    (item.pistonBehavior == null ? "-\n" : "Piston behavior: " + item.pistonBehavior + " AND\n") +
                    (item.blastResistance == null ? "-" : "Blast resistance: " + item.blastResistance);
            widget.setTooltip(Tooltip.create(Component.literal(builder)));
            addRenderableWidget(widget);
            y += 23;
        }
        blockSuggestions = addWidget(new SuggestionListWidget(
                blockInput,
                blockInput.getX(),
                blockInput.getY() + blockInput.getHeight() + 1,
                listWidth
        ));

        //presets
        addRenderableWidget(new StringWidget(width - 130, 10, 100, 20, Component.literal("Presets"), font));

        ToggelableWidgedDropDownList<Whitelist> presets = getPresets();
        addWidget(presets);
        addRenderableWidget(Button.builder(Component.literal("Use preset"), btn -> {
            if(presets.getSelectedEntry() != null){
                current_whitelist.addAll(presets.getSelectedEntry().whitelist);
                WhitelistManager.saveData(new Whitelist(current_whitelist), filename);
                minecraft.setScreenAndShow(new WhitelistEditorScreen(parent, filename, 0));
            }
        }).bounds(width - 110, 115, 80, 20).build());


        int buttonY = height - 40;
        if (currentPage > 0) {
            addRenderableWidget(Button.builder(Component.literal("< Prev"), btn ->
                    minecraft.setScreenAndShow(new WhitelistEditorScreen(parent, filename, currentPage - 1))
            ).bounds(width / 2 - 120, buttonY, 80, 20).build());
        }

        addRenderableWidget(Button.builder(
                Component.literal("Page " + (currentPage + 1) + "/" + Math.max(1, totalPages)),
                btn -> {}
        ).bounds(width / 2 - 40, buttonY, 80, 20).build());

        if (to < current_whitelist.size()) {
            addRenderableWidget(Button.builder(Component.literal("Next >"), btn ->
                    minecraft.setScreenAndShow(new WhitelistEditorScreen(parent, filename, currentPage + 1))
            ).bounds(width / 2 + 40, buttonY, 80, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), btn -> minecraft.setScreenAndShow(parent))
                .bounds(30, height - 30, 80, 20).build());

        addToWhitelistBtn = Button.builder(Component.literal("Add to whitelist"), btn -> {
                    if(validateCreatedWhitelistItem()){
                        current_whitelist.add(createdWhitelistItem);
                        WhitelistManager.saveData(new Whitelist(current_whitelist), filename);
                        minecraft.setScreenAndShow(new WhitelistEditorScreen(parent, filename, 0));
                    }
                }).bounds(30, 260, 90, 20).build();
        addRenderableWidget(addToWhitelistBtn);
    }


    private @NonNull ToggelableWidgedDropDownList<Whitelist> getPresets() {
        List<Whitelist> list = new ArrayList<>();
        Whitelist worldEater = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem(null, null, ">9", "≠DESTROY"));
        }}, "World eater");
        Whitelist fluidsAndWaterlogged = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem("=" + BuiltInRegistries.BLOCK.getKey(Blocks.WATER), null, null, null));
            add(new WhitelistItem("=" + BuiltInRegistries.BLOCK.getKey(Blocks.LAVA), null, null, null));
            add(new WhitelistItem(null, "true", null, null));
        }}, "Fluids and Waterlogged");
        Whitelist quarry = new Whitelist(new ArrayList<>(){{
            add(new WhitelistItem(null, null, null, "=IMMOVABLE"));
            add(new WhitelistItem(null, null, ">9", "≠DESTROY"));
            Blocks.GLAZED_TERRACOTTA.forEach(block -> add(new WhitelistItem("=" + BuiltInRegistries.BLOCK.getKey(block), null, null, null)));
        }}, "Quarry");

        list.add(worldEater);
        list.add(fluidsAndWaterlogged);
        list.add(quarry);
        return new ToggelableWidgedDropDownList<>(width - 180, 30, 150, 20, 100, 5, list);
    }

    private boolean validateCreatedWhitelistItem(){
        return createdWhitelistItem.block != null || createdWhitelistItem.waterlogged != null || createdWhitelistItem.pistonBehavior != null || createdWhitelistItem.blastResistance != null;
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        try {
            if(blockEqualsOperatorsWidget.getSelectedEntry() != null && !blockInput.getValue().isEmpty() && BuiltInRegistries.BLOCK.containsKey(Identifier.parse(blockInput.getValue()))){
                createdWhitelistItem.block = blockEqualsOperatorsWidget.getSelectedEntry() + BuiltInRegistries.BLOCK.getKey(BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockInput.getValue())));
            }else createdWhitelistItem.block = null;
        }catch (Exception ignored){}
        //waterlogged
        if(waterloggedWidget.getSelectedEntry() != null){
            createdWhitelistItem.waterlogged = waterloggedWidget.getSelectedEntry();
        }else createdWhitelistItem.waterlogged = null;

        //pistonBehavior
        if(pistonEqualsOperatorsWidget.getSelectedEntry() != null && pistonBehaviorWidget.getSelectedEntry() != null){
            createdWhitelistItem.pistonBehavior = pistonEqualsOperatorsWidget.getSelectedEntry() + pistonBehaviorWidget.getSelectedEntry();
        }else createdWhitelistItem.pistonBehavior = null;

        //blast resistance
        if(blastResistanceComparisonOperatorsWidget.getSelectedEntry() != null && !blastResistanceValue.getValue().isEmpty()){
            createdWhitelistItem.blastResistance = blastResistanceComparisonOperatorsWidget.getSelectedEntry() + blastResistanceValue.getValue();
        }else createdWhitelistItem.blastResistance = null;

        //borders
        context.outline(20,20, 245, 265, CommonColors.LIGHT_GRAY);
        context.outline(275,20, 130, height - 80, CommonColors.LIGHT_GRAY);

        addToWhitelistBtn.active = validateCreatedWhitelistItem();
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(font, Component.literal(createdWhitelistItem.toString()), 30,240, CommonColors.WHITE, true);
        blockSuggestions.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (blockSuggestions != null && blockSuggestions.visible) {
            if (event.key() == GLFW.GLFW_KEY_DOWN) {
                blockSuggestions.selectRelative(1);
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_UP) {
                blockSuggestions.selectRelative(-1);
                return true;
            }

            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER || event.key() == GLFW.GLFW_KEY_TAB) {
                if (blockSuggestions.confirmSelected()) {
                    return true;
                }
            }

            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                blockSuggestions.hide();
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (blockSuggestions != null && blockSuggestions.visible) {
            if (verticalAmount > 0) {
                blockSuggestions.selectRelative(-1);
                return true;
            }

            if (verticalAmount < 0) {
                blockSuggestions.selectRelative(1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (blockSuggestions != null && blockSuggestions.visible && blockSuggestions.isMouseOver(event.x(), event.y())) {
            blockSuggestions.mouseClicked(event, doubleClick);

            this.setFocused(blockInput);
            blockInput.setFocused(true);
            return true;
        }

        boolean result = super.mouseClicked(event, doubleClick);

        //close when clicked elsewhere
        if (blockSuggestions != null && blockSuggestions.visible
                && !blockSuggestions.isMouseOver(event.x(), event.y())
                && !blockInput.isMouseOver(event.x(), event.y())) {
            blockSuggestions.hide();
        }

        //show tooltip if clicked
        if (blockInput.isFocused()
                && blockInput.isMouseOver(event.x(), event.y())
                && !blockInput.getValue().isEmpty()) {
            onBlockTextChanged(blockInput.getValue());
        }

        return result;
    }

    private void onBlockTextChanged(String text) {
        if (blockSuggestions == null) {
            return;
        }

        String block = parseBlock(text);
        createdWhitelistItem.block = block;
        blockInput.setTextColor(block != null ? 0xFF55FF55 : 0xFFFFFFFF);

        String query = normalize(text);

        if (query.isEmpty()) {
            blockSuggestions.setSuggestions(List.of());
            return;
        }


        List<String> matches = BLOCK_IDS.stream()
                .filter(id -> id.contains(query))
                .sorted(Comparator.comparingInt((String id) -> suggestionScore(id, query))
                        .thenComparing(WhitelistEditorScreen::pathOf))
                .toList();

        blockSuggestions.setSuggestions(matches);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }

        return s.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
    @Nullable
    private String parseBlock(String raw) {
        String normalized = normalize(raw);

        if (normalized.isEmpty()) {
            return null;
        }

        Identifier id = tryParseId(normalized);

        if (id == null && !normalized.contains(":")) {
            id = tryParseId("minecraft:" + normalized);
        }

        if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.getValue(id).toString();
        }

        return null;
    }

    @Nullable
    private Identifier tryParseId(String s) {
        try {
            return Identifier.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String pathOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static int suggestionScore(String id, String query) {
        String path = pathOf(id);

        if (id.equals(query) || path.equals(query)) {
            return 0;
        }

        if (id.startsWith(query) || path.startsWith(query)) {
            return 1;
        }

        int idx = path.indexOf(query);
        if (idx > 0 && path.charAt(idx - 1) == '_') {
            return 2;
        }

        return 3;
    }

}
