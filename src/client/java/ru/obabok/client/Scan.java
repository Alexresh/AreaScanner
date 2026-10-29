package ru.obabok.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import ru.obabok.client.gui.screens.MaterialListScreen;
import ru.obabok.common.model.BlockArea;
import ru.obabok.client.models.ScanState;
import ru.obabok.client.util.ChunkScheduler;
import ru.obabok.client.render.RenderUtil;
import ru.obabok.client.util.WhitelistManager;
import ru.obabok.common.BlockMatcher;
import ru.obabok.common.model.Whitelist;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Scan {
    public static volatile HashSet<BlockPos> selectedBlocks = new HashSet<>();
    public static volatile HashSet<ChunkPos> unloadedChunks = new HashSet<>();
    private static volatile Whitelist whitelist;
    private static volatile BlockArea area;
    private static volatile boolean processing = false;
    private static boolean remoteProcessing = false;
    private static volatile long remoteChunkProcessedCounter;
    private static long allChunksCounter;
    private static volatile String currentFilename;
    public static volatile boolean renderDirty = true;

    public enum PistonBehavior {
            NORMAL,
            IMMOVABLE,
            DESTROY
    }


    public static int executeAsync(ClientLevel world, BlockArea _range, String filename){
        stopScan();
        area = _range;
        if (world == null || _range == null || _range.isEmpty()) return 0;
        currentFilename = filename;
        whitelist = WhitelistManager.loadData(currentFilename);
        if(whitelist == null){
            if(Minecraft.getInstance().player != null){
                Minecraft.getInstance().player.displayClientMessage(Component.literal("Whitelist not found"), false);
            }
            stopScan();
            return 0;
        }
        processing = true;

        HashSet<ChunkPos> affectedChunks = area.getAffectedChunks();
        for (ChunkPos chunkPos : affectedChunks){
            if (world.getChunkSource().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) != null) {
                ChunkScheduler.addChunkToProcess(chunkPos);
            }
            unloadedChunks.add(chunkPos);
            allChunksCounter++;
        }
        renderDirty = true;
        return 1;
    }


    public static void saveState(){
        ScanState.saveData(new ScanState(selectedBlocks, unloadedChunks, whitelist, area, allChunksCounter, currentFilename));
        stopScan();
    }

    public static boolean loadState(){
        ScanState saveData = ScanState.loadData();
        if(saveData == null) return false;
        selectedBlocks = (HashSet<BlockPos>) saveData.selectedBlocks;
        unloadedChunks = (HashSet<ChunkPos>) saveData.unloadedChunks;
        renderDirty = true;
        whitelist = saveData.whitelist;
        area = saveData.range;
        allChunksCounter = saveData.allChunksCounter;
        currentFilename = saveData.currentFilename;
        processing = true;
        return true;
    }

    public static void stopScan(){
        processing = false;
        selectedBlocks.clear();
        unloadedChunks.clear();
        renderDirty = true;
        allChunksCounter = 0;
        area = null;
        currentFilename = null;
        remoteProcessing = false;
        whitelist = null;
        ChunkScheduler.clearQueue();
        RenderUtil.clearRender();
        MaterialListScreen.clear();
    }

    public static void setArea(BlockArea _range){
        if(!processing) area = _range;
    }
    public static String getCurrentFilename(){
        return currentFilename;
    }
    public static boolean isProcessing(){
        return processing;
    }
    public static boolean isRemoteProcessing() {
        return remoteProcessing;
    }
    public static BlockArea getArea(){
        return area;
    }
    public static long getAllChunksCounter(){return allChunksCounter;}


    public static void startRemoteScan(BlockArea _range, String filename, long totalChunks) {
        stopScan();
        processing = true;
        remoteProcessing = true;
        area = _range;
        currentFilename = filename;
        allChunksCounter = totalChunks;
        remoteChunkProcessedCounter = totalChunks;
    }

    private static void addPositions(long[] positions){
        for (int i = 0; i < positions.length; i++) {
            selectedBlocks.add(BlockPos.of(positions[i]));
        }
        renderDirty = true;
    }

    private static void removePositions(long[] positions){
        for (int i = 0; i < positions.length; i++) {
            selectedBlocks.remove(BlockPos.of(positions[i]));
        }
        renderDirty = true;
    }

    public static void applyRemoteDelta(long[] positions, boolean add) {
        if (!remoteProcessing) return;
        if (positions == null || positions.length == 0) return;
        if (add) {
            addPositions(positions);
        }else{
            removePositions(positions);
        }
    }

    public static long getChunkProcessedCounter(){
        return isRemoteProcessing() ? remoteChunkProcessedCounter : allChunksCounter - unloadedChunks.size();
    }

    public static void markRemoteChunkProcessed(long remoteProcessed) {
        if (!remoteProcessing) return;
        remoteChunkProcessedCounter = remoteProcessed;
    }

    public static void processChunk(ClientLevel world, ChunkPos chunkPos){
        if (!processing || isRemoteProcessing() || area == null || area.isEmpty() || world == null || whitelist == null || chunkPos == null) {
            return;
        }
        if (world.getChunkSource().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) == null) {
            return;
        }

        List<BlockBox> relevantBoxes = area.getBoxesIntersectingChunk(chunkPos);

        if (relevantBoxes.isEmpty()) {
            return;
        }

        updateChunk(chunkPos, world);

        checkFinished();
        if (processing) {
            for (BlockBox box : relevantBoxes) {
                int localMinX = Math.max(0, box.min().getX() - (chunkPos.x << 4));
                int localMaxX = Math.min(15, box.max().getX() - (chunkPos.x << 4));
                int localMinZ = Math.max(0, box.min().getZ() - (chunkPos.z << 4));
                int localMaxZ = Math.min(15, box.max().getZ() - (chunkPos.z << 4));

                for (int x = localMinX; x <= localMaxX; x++) {
                    for (int y = box.min().getY(); y <= box.max().getY(); y++) {
                        for (int z = localMinZ; z <= localMaxZ; z++) {
                            BlockPos pos = new BlockPos((chunkPos.x << 4) + x, y, (chunkPos.z << 4) + z);
                            processBlock(pos, world.getBlockState(pos), world);
                        }
                    }
                }
            }
        }

        if(unloadedChunks.remove(chunkPos)){
            renderDirty = true;
        }
        checkFinished();
    }

    //deletes blocks from selected blocks
    public static void updateChunk(ChunkPos chunkPos, ClientLevel world){
        Iterator<BlockPos> iterator = selectedBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = iterator.next();
            if (blockPos.getX() >> 4 == chunkPos.x && blockPos.getZ() >> 4 == chunkPos.z) {
                if(!BlockMatcher.matches(whitelist, world.getBlockState(blockPos), world, blockPos)){
                    iterator.remove();
                    renderDirty = true;
                    MaterialListScreen.addBlock(world.getBlockState(blockPos).getBlock(), -1);
                }
            }
        }
    }

    private static void checkFinished(){
        if(!processing) return;
        if(!selectedBlocks.isEmpty() || !unloadedChunks.isEmpty()) return;
        if(!ChunkScheduler.getChunkQueue().isEmpty()) return;
        stopScan();
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if(mc.player != null){
                mc.player.displayClientMessage(Component.literal("Scan finished"), false);
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        });
    }

    //adds blocks to selected blocks
    public static void processBlock(BlockPos blockPos, BlockState blockState, Level world){
        if (whitelist == null) return;

        if (BlockMatcher.matches(whitelist, blockState, world, blockPos)) {
            if(selectedBlocks.add(blockPos)){
                renderDirty = true;
            }
            MaterialListScreen.addBlock(blockState.getBlock(), 1);
        }

        //checkFinished();
    }
}
