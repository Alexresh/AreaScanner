package ru.obabok.areascanner.client;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import ru.obabok.areascanner.client.gui.screens.MaterialListScreen;
import ru.obabok.areascanner.client.models.ScanState;
import ru.obabok.areascanner.common.BlockMatcher;
import ru.obabok.areascanner.common.model.Whitelist;
import ru.obabok.areascanner.client.util.*;

import java.util.*;

public class Scan {
    public static HashSet<BlockPos> selectedBlocks = new HashSet<>();
    public static HashSet<ChunkPos> unloadedChunks = new HashSet<>();
    private static Whitelist whitelist;
    private static BlockBox range;
    private static boolean processing = false;
    private static boolean remoteProcessing = false;
    private static long allChunksCounter;
    private static String currentFilename;

    public enum PistonBehavior {
            NORMAL,
            IMMOVABLE,
            DESTROY
    }


    public static int executeAsync(ClientWorld world, BlockBox _range, String filename){
        stopScan();
        processing = true;
        range = _range;
        if (world == null) return 0;
        currentFilename = filename;
        whitelist = WhitelistManager.loadData(currentFilename);
        if(whitelist == null) return 0;

        int startChunkX = range.getMinX() >> 4;
        int startChunkZ = range.getMinZ() >> 4;
        int endChunkX = range.getMaxX() >> 4;
        int endChunkZ = range.getMaxZ() >> 4;

        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                if (world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    ChunkScheduler.addChunkToProcess(chunkPos);
                }
                unloadedChunks.add(chunkPos);
                allChunksCounter++;
            }
        }
        return 1;
    }


    public static void saveState(){
        ScanState.saveData(new ScanState(selectedBlocks, unloadedChunks, whitelist, range, allChunksCounter, currentFilename));
        stopScan();
    }
    public static boolean loadState(){
        ScanState saveData = ScanState.loadData();
        if(saveData == null) return false;
        selectedBlocks = (HashSet<BlockPos>) saveData.selectedBlocks;
        unloadedChunks = (HashSet<ChunkPos>) saveData.unloadedChunks;
        whitelist = saveData.whitelist;
        range = saveData.range;
        allChunksCounter = saveData.allChunksCounter;
        currentFilename = saveData.currentFilename;
        processing = true;
        return true;
    }

    public static void stopScan(){
        processing = false;
        selectedBlocks.clear();
        unloadedChunks.clear();
        allChunksCounter = 0;
        range = null;
        currentFilename = null;
        remoteProcessing = false;
        ChunkScheduler.clearQueue();
        RenderUtil.clearRender();
        MaterialListScreen.clear();
    }

    public static void setRange(BlockBox _range){
        if(!processing) range = _range;
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
    public static BlockBox getRange(){
        return range;
    }
    public static long getAllChunksCounter(){return allChunksCounter;}


    public static void startRemoteScan(BlockBox _range, String filename, long totalChunks) {
        stopScan();
        processing = true;
        remoteProcessing = true;
        range = _range;
        currentFilename = filename;
        allChunksCounter = totalChunks;
        if (_range == null) {
            return;
        }
        int startChunkX = range.getMinX() >> 4;
        int startChunkZ = range.getMinZ() >> 4;
        int endChunkX = range.getMaxX() >> 4;
        int endChunkZ = range.getMaxZ() >> 4;
        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                unloadedChunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
    }

    public static void applyRemoteChunkData(long[] positions) {
        if (!remoteProcessing) return;
        if (positions == null || positions.length == 0) return;
        for (int i = 0; i < positions.length; i++) {
            addPositions(positions);
        }
        //selectedBlocks.addAll(BlockPos.fromLong(positions));
    }

    private static void addPositions(long[] positions){
        for (int i = 0; i < positions.length; i++) {
            selectedBlocks.add(BlockPos.fromLong(positions[i]));
        }
    }

    private static void removePositions(long[] positions){
        for (int i = 0; i < positions.length; i++) {
            selectedBlocks.remove(BlockPos.fromLong(positions[i]));
        }
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

    public static void markRemoteChunkProcessed(ChunkPos chunkPos) {
        if (!remoteProcessing) return;
        unloadedChunks.remove(chunkPos);
    }

//    public static void finishRemoteScanProcessing() {
//        if (!remoteProcessing) return;
//        //dont touch this!
//        processing = false;
//    }

    public static void processChunk(ClientWorld world, ChunkPos chunkPos){
        if(!processing || range == null || world == null || whitelist == null || chunkPos == null) return;
        if(!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) return;
        if((chunkPos.x >= range.getMinX() >> 4) && (chunkPos.x <= range.getMaxX() >> 4) && (chunkPos.z >= range.getMinZ() >> 4) && (chunkPos.z <= range.getMaxZ() >> 4)){
            //delete destroyed blocks from selected blocks
            updateChunk(chunkPos, world);
            //add new blocks to selected blocks
            checkProcessing();
            if(processing){
                for (int x = 0; x < 16; x++) {
                    for (int y = range.getMinY(); y <= range.getMaxY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos blockPos = new BlockPos(chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
                            processBlock(blockPos, world.getBlockState(blockPos), world);
                        }
                    }
                }
            }

            //remove chunk from processing
            unloadedChunks.remove(chunkPos);
            checkProcessing();
        }
    }

    //deletes blocks from selected blocks
    public static void updateChunk(ChunkPos chunkPos, ClientWorld world){
        Iterator<BlockPos> iterator = selectedBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = iterator.next();
            if (blockPos.getX() >> 4 == chunkPos.x && blockPos.getZ() >> 4 == chunkPos.z) {
                if(!BlockMatcher.matches(whitelist, world.getBlockState(blockPos), world, blockPos)){
                    iterator.remove();
                    MaterialListScreen.addBlock(world.getBlockState(blockPos).getBlock(), -1);
                }
            }
        }
        //checkProcessing();
    }

    private static void checkProcessing(){
        if(processing && selectedBlocks.isEmpty() && unloadedChunks.isEmpty()) {
            if(MinecraftClient.getInstance().player != null){
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Scan finished"),false);
                MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            stopScan();
        }
    }

    //adds blocks to selected blocks
    public static void processBlock(BlockPos blockPos, BlockState blockState, World world){
        if(range == null || whitelist == null) return;
        if(blockPos.getX() <= range.getMaxX() && blockPos.getX() >= range.getMinX() &&
                blockPos.getY() <= range.getMaxY() && blockPos.getY() >= range.getMinY() &&
                blockPos.getZ() <= range.getMaxZ() && blockPos.getZ() >= range.getMinZ()){
            if(BlockMatcher.matches(whitelist, blockState, world, blockPos)){
                selectedBlocks.add(blockPos);
                MaterialListScreen.addBlock(blockState.getBlock(), 1);
            }
        }
        checkProcessing();
    }
}
