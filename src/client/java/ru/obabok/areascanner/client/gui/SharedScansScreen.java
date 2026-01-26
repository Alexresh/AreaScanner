package ru.obabok.areascanner.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.obabok.areascanner.client.network.ClientNetwork;
import ru.obabok.areascanner.common.model.JobInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
public class SharedScansScreen extends Screen {
    private final int currentPage;
    private static final int ENTRIES_PER_PAGE = 6;
    private final Screen parent;

    public SharedScansScreen(int page, Screen parent) {
        super(Text.literal("Shared scans"));
        this.parent = parent;
        this.currentPage = Math.max(0, page);
    }

    @Override
    protected void init() {
        ClientNetwork.requestSharedList();
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> client.setScreen(parent))
                .dimensions(30, height - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), btn -> {
            refresh();
        }).dimensions(width - 90, 10, 70, 20).build());
    }

    public void drawJobs(){
        List<JobInfo> scans = ClientNetwork.getJobList();

        int totalPages = (scans.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE;
        int from = currentPage * ENTRIES_PER_PAGE;
        int to = Math.min(from + ENTRIES_PER_PAGE, scans.size());
        List<JobInfo> page = scans.subList(from, to);



        if (scans.isEmpty()) {
            addDrawableChild(new TextWidget(width / 2 - 60, 40, 120, 20,
                    Text.literal("No shared scans"), textRenderer));
            return;
        }

        int y = 30;
        int rowHeight = 36;
        for (JobInfo info : page) {
            String title = " " + ((info.name() == null || info.name().isEmpty()) ? ""+info.id() : info.name());
            String owner = info.owner() == null ? "unknown" : info.owner();
            String progress = "Chunks: " + info.processedChunks() + "/" + info.totalChunks() + " Blocks: " + info.selectedBlocks();
            addDrawableChild(new TextWidget(60, y, width - 120, 12,
                    Text.literal(info.completedScan() ? "✓" : "⏳").formatted(info.completedScan() ? Formatting.GREEN : Formatting.YELLOW).append(Text.literal(title + " (" + owner + ") " + progress)), textRenderer));

            String dimension = info.dimension() == null ? "unknown" : info.dimension().toString();
            String whitelistName = info.whitelistName() == null ? "_" : info.whitelistName();
            String details = "name: " + whitelistName + " dim: " + dimension
                    + " range: " + formatRange(info);
            addDrawableChild(new TextWidget(60, y + 12, width - 120, 12,
                    Text.literal(details).formatted(Formatting.GRAY), textRenderer));

            ButtonWidget joinBtn = ButtonWidget.builder(
                            Text.literal(info.completedScan() ? "Join" : "Busy"),
                            btn -> ClientNetwork.joinScan(info))
                    .dimensions(width - 90, y, 60, 20).build();
            joinBtn.active = info.completedScan();
            addDrawableChild(joinBtn);

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                ClientNetwork.deleteScan(info.id());
                ClientNetwork.openSharedScansScreen(parent);
            }).dimensions(width - 25, y, 20, 20).build());

            y += rowHeight;
        }



        int buttonY = height - 60;
        if (currentPage > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn ->
                    ClientNetwork.openSharedScansScreen(parent)//client.setScreen(new SharedScansScreen(currentPage - 1))
            ).dimensions(width / 2 - 120, buttonY, 80, 20).build());
        }

        addDrawableChild(new TextWidget(
                width / 2 - 40, buttonY,
                80, 20,
                Text.literal("Page " + (currentPage + 1) + "/" + Math.max(1, totalPages)), textRenderer));

        if (to < scans.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn ->
                    ClientNetwork.openSharedScansScreen(parent)//client.setScreen(new SharedScansScreen(currentPage + 1))
            ).dimensions(width / 2 + 40, buttonY, 80, 20).build());
        }
    }

    @Override
    protected void applyBlur(){

    }
    @Override
    protected void renderDarkening(DrawContext context, int x, int y, int width, int height){

    }

    private void refresh(){
        ClientNetwork.requestSharedList();
        ClientNetwork.openSharedScansScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private String formatRange(JobInfo info) {
        if (info.range() == null) {
            return "unknown";
        }
        return info.range().getMinX() + "," + info.range().getMinY() + "," + info.range().getMinZ()
                + " -> "
                + info.range().getMaxX() + "," + info.range().getMaxY() + "," + info.range().getMaxZ();
    }
}
