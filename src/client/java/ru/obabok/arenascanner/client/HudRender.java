package ru.obabok.arenascanner.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.Config;
import ru.obabok.arenascanner.client.util.ChunkScheduler;

import java.util.Iterator;

import static ru.obabok.arenascanner.References.LOGGER;


public class HudRender {


    public static void render(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        if (Config.Generic.MAIN_RENDER.getBooleanValue() && Config.Hud.HUD_ENABLE.getBooleanValue()) {
            int windowWidth = drawContext.getScaledWindowWidth();
            int windowHeight = drawContext.getScaledWindowHeight();
            int hudStartX = Config.Hud.HUD_POS_X.getIntegerValue() >= 0
                    ? Config.Hud.HUD_POS_X.getIntegerValue()
                    : windowWidth + Config.Hud.HUD_POS_X.getIntegerValue();
            int hudStartY = Config.Hud.HUD_POS_Y.getIntegerValue() >= 0
                    ? Config.Hud.HUD_POS_Y.getIntegerValue()
                    : windowHeight + Config.Hud.HUD_POS_Y.getIntegerValue();
            drawContext.getMatrices().push();
            drawContext.getMatrices().scale(Config.Hud.HUD_SCALE.getFloatValue(),Config.Hud.HUD_SCALE.getFloatValue(),0);
            try {
                if(!ScanCommand.selectedBlocks.isEmpty()){
                    BlockPos pos = ScanCommand.selectedBlocks.iterator().next();
                    String selectedBlocksText = "Selected blocks: %d -> [%d, %d, %d]".formatted(ScanCommand.selectedBlocks.size(), pos.getX(), pos.getY(), pos.getZ());
                    int textWidthSelected = MinecraftClient.getInstance().textRenderer.getWidth(selectedBlocksText);
                    int posX = hudStartX;
                    if (Config.Hud.HUD_POS_X.getIntegerValue() < 0) {
                        posX -= textWidthSelected;
                    }

                    drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(selectedBlocksText), posX, hudStartY, Config.Hud.HUD_SELECTED_BLOCKS_COLOR.getIntegerValue());
                }
            }catch (Exception exception){
                LOGGER.error(exception.getMessage());
            }

            if(!ScanCommand.unloadedChunks.isEmpty()){
                try {
                    Iterator<ChunkPos> iterator = ScanCommand.unloadedChunks.iterator();
                    if(iterator.hasNext()){
                        String unloadedChunksText = "Unchecked chunks: %d -> %s".formatted(ScanCommand.unloadedChunks.size(), iterator.next().toString());
                        int textWidthUnloaded = MinecraftClient.getInstance().textRenderer.getWidth(unloadedChunksText);
                        int posX = hudStartX;
                        if (Config.Hud.HUD_POS_X.getIntegerValue() < 0) {
                            posX -= textWidthUnloaded;
                        }
                        drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(unloadedChunksText), posX, hudStartY + 10, Config.Hud.HUD_UNCHECKED_CHUNKS_COLOR.getIntegerValue());
                    }
                }catch (Exception exception){
                    LOGGER.error(exception.getMessage());
                }

            }
            if(!ChunkScheduler.getChunkQueue().isEmpty()){
                String processedChunksText = "ProcessedChunks: %d".formatted(ChunkScheduler.getChunkQueue().size());
                int textWidthUnloaded = MinecraftClient.getInstance().textRenderer.getWidth(processedChunksText);
                int posX = hudStartX;
                if (Config.Hud.HUD_POS_X.getIntegerValue() < 0) {
                    posX -= textWidthUnloaded;
                }
                drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(processedChunksText), posX, hudStartY + 20, 0xFFFFFFFF);
            }
            drawContext.getMatrices().pop();
        }
    }
}
