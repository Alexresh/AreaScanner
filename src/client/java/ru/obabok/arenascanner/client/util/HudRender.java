package ru.obabok.arenascanner.client.util;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.Config;
import ru.obabok.arenascanner.client.Scan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static ru.obabok.arenascanner.client.util.References.LOGGER;


public class HudRender {

    private static final List<String> lines = new ArrayList<>();
    public static void render(DrawContext drawContext) {
        if (Config.Generic.MAIN_RENDER.getBooleanValue() && Config.Hud.HUD_ENABLE.getBooleanValue()) {
            lines.clear();

            if(!ChunkScheduler.getChunkQueue().isEmpty()){
                String processedChunksText = "ProcessedChunks: %d".formatted(ChunkScheduler.getChunkQueue().size());
                lines.add(processedChunksText);
            }

            try {
                if(!Scan.selectedBlocks.isEmpty()){
                    BlockPos pos = Scan.selectedBlocks.iterator().next();
                    String selectedBlocksText = "Selected blocks: %d -> [%d, %d, %d]".formatted(Scan.selectedBlocks.size(), pos.getX(), pos.getY(), pos.getZ());
                    lines.add(selectedBlocksText);
                }
            }catch (Exception exception){
                LOGGER.error(exception.getMessage());
            }

            if(!Scan.unloadedChunks.isEmpty()){
                try {
                    Iterator<ChunkPos> iterator = Scan.unloadedChunks.iterator();
                    if(iterator.hasNext()){
                        String unloadedChunksText = "Unchecked chunks: %d -> %s".formatted(Scan.unloadedChunks.size(), iterator.next().toString());
                        lines.add(unloadedChunksText);
                    }
                }catch (Exception exception){
                    LOGGER.error(exception.getMessage());
                }

            }

            RenderUtils.renderText(Config.Hud.HUD_POS_X.getIntegerValue(), Config.Hud.HUD_POS_Y.getIntegerValue(), Config.Hud.HUD_SCALE.getFloatValue(), Colors.WHITE, Colors.BLACK, (HudAlignment)Config.Hud.HUD_ALIGNMENT.getOptionListValue(), false, false, lines, drawContext);
        }
    }
}
