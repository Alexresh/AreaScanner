package ru.obabok.arenascanner.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.NewScan;

import java.util.Queue;
import java.util.concurrent.*;



public class ChunkScheduler {

    private static final Queue<ChunkPos> chunkQueue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
    private static final ScheduledExecutorService schedulerRender = Executors.newScheduledThreadPool(0);
    private static ScheduledFuture<?> scheduledFuture;
    private static long period = 30;

    public static void startProcessing() {
        scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean processing = true;
                while (processing){
                    ChunkPos chunkPos = chunkQueue.poll();
                    if (chunkPos != null && MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                        processing = false;
                        NewScan.processChunk(MinecraftClient.getInstance().world, chunkPos);
                    }
                }

            }catch (Exception e){
                References.LOGGER.error(e.getMessage());
            }

        }, 0, period, TimeUnit.MILLISECONDS);

        schedulerRender.scheduleAtFixedRate(() -> {
            try {
                RenderUtil.clearRender();
                RenderUtil.addAllRenderBlocks(NewScan.selectedBlocks);
                RenderUtil.addAllRenderChunks(NewScan.unloadedChunks);
            }catch (Exception ignored){}

        }, 0, 1000, TimeUnit.MILLISECONDS);


    }

    public static void updatePeriod(long newPeriod) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
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
