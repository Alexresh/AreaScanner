package ru.obabok.arenascanner.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.Scan;

import java.util.Queue;
import java.util.concurrent.*;



public class ChunkScheduler {

    private static final Queue<ChunkPos> chunkQueue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService schedulerChunk = Executors.newScheduledThreadPool(0);
    private static final ScheduledExecutorService schedulerRender = Executors.newScheduledThreadPool(0);
    private static ScheduledFuture<?> chunkScheduledFuture;
    private static ScheduledFuture<?> renderScheduledFuture;
    private static long period = 30;

    public static void startProcessing() {
        chunkScheduledFuture = schedulerChunk.scheduleAtFixedRate(() -> {
            try {
                ChunkPos chunkPos = chunkQueue.poll();

                if (chunkPos == null) {
                    return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null) {
                    return;
                }

                if (client.world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                    Scan.processChunk(client.world, chunkPos);
                }

            }catch (Exception e){
                References.LOGGER.error(e.getMessage());
            }

        }, 0, period, TimeUnit.MILLISECONDS);


        renderScheduledFuture = schedulerRender.scheduleAtFixedRate(() -> {
            try {
                RenderUtil.clearRender();
                RenderUtil.addAllRenderBlocks(Scan.selectedBlocks);
                RenderUtil.addAllRenderChunks(Scan.unloadedChunks);
            }catch (Exception ignored){}

        }, 0, 1000, TimeUnit.MILLISECONDS);


    }

    public static void updatePeriod(long newPeriod) {
        if (chunkScheduledFuture != null) {
            chunkScheduledFuture.cancel(false);
        }
        if(renderScheduledFuture != null){
            renderScheduledFuture.cancel(false);
        }
        period = newPeriod;
        startProcessing();
    }

    public static Queue<ChunkPos> getChunkQueue(){
        return chunkQueue;
    }

    public static void addChunkToProcess(ChunkPos pos){
        chunkQueue.add(pos);
    }

}
