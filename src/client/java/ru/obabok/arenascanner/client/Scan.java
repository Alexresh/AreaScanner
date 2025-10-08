package ru.obabok.arenascanner.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.util.ChunkScheduler;
import ru.obabok.arenascanner.client.util.RenderUtil;
import ru.obabok.arenascanner.client.util.ScanSaveData;
import ru.obabok.arenascanner.client.util.WhitelistsManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

public class Scan {
    public static HashSet<BlockPos> selectedBlocks = new HashSet<>();
    public static HashSet<ChunkPos> unloadedChunks = new HashSet<>();
    private static ArrayList<Block> whitelist = new ArrayList<>();
    private static BlockBox range;
    public static boolean worldEaterMode = false;
    private static boolean processing = false;
    private static long allChunksCounter;
    private static String currentFilename;

    public static int executeAsync(ClientWorld world, BlockBox _range, String filename){
        stopScan();
        processing = true;
        range = _range;
        if (world == null) return 0;
        currentFilename = filename;
        whitelist = WhitelistsManager.loadWhitelist(currentFilename);
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
        ScanSaveData.saveData(new ScanSaveData(selectedBlocks, unloadedChunks, whitelist, range, worldEaterMode, allChunksCounter, currentFilename));
        stopScan();
    }
    public static boolean loadState(){
        ScanSaveData saveData = ScanSaveData.loadData();
        if(saveData == null) return false;
        selectedBlocks = (HashSet<BlockPos>) saveData.selectedBlocks;
        unloadedChunks = (HashSet<ChunkPos>) saveData.unloadedChunks;
        whitelist = new ArrayList<>(saveData.whitelist);
        range = saveData.range;
        worldEaterMode = saveData.worldEaterMode;
        allChunksCounter = saveData.allChunksCounter;
        currentFilename = saveData.currentFilename;
        processing = true;
        return true;
    }

    public static void stopScan(){
        selectedBlocks.clear();
        unloadedChunks.clear();
        allChunksCounter = 0;
        range = null;
        currentFilename = null;
        RenderUtil.clearRender();
        processing = false;
    }

    public static void setRange(BlockBox _range){
        if(!processing) range = _range;
    }
    public static String getCurrentFilename(){
        return currentFilename;
    }
    public static boolean getProcessing(){
        return processing;
    }
    public static BlockBox getRange(){
        return range;
    }
    public static long getAllChunksCounter(){return allChunksCounter;}

    public static void processChunk(ClientWorld world, ChunkPos chunkPos){
        if(range == null || world == null || whitelist == null || chunkPos == null) return;
        if(!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) return;
        if((chunkPos.x >= range.getMinX() >> 4) && (chunkPos.x <= range.getMaxX() >> 4) && (chunkPos.z >= range.getMinZ() >> 4) && (chunkPos.z <= range.getMaxZ() >> 4)){
            //delete destroyed blocks from selected blocks
            updateChunk(chunkPos, world);
            //add new blocks to selected blocks
            for (int x = 0; x < 16; x++) {
                for (int y = range.getMinY(); y <= range.getMaxY(); y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos blockPos = new BlockPos(chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
                        processBlock(blockPos, world.getBlockState(blockPos));
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
                if (worldEaterMode) {
                    if (!((getBlastResistance(world.getBlockState(blockPos), world.getBlockState(blockPos).getFluidState()).isPresent()
                            && getBlastResistance(world.getBlockState(blockPos), world.getBlockState(blockPos).getFluidState()).get() > 9)
                            && world.getBlockState(blockPos).getPistonBehavior() != PistonBehavior.DESTROY)) {
                        iterator.remove();
                    }
                } else if (!whitelist.contains(world.getBlockState(blockPos).getBlock())) {
                    iterator.remove();
                }
            }
        }
        //checkProcessing();
    }

    private static void checkProcessing(){
        if(selectedBlocks.isEmpty() && unloadedChunks.isEmpty()) {
            if(MinecraftClient.getInstance().player != null){
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Scan finished"),false);
                MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            stopScan();
        }
    }


    //adds blocks to selected blocks
    public static void processBlock(BlockPos blockPos, BlockState blockState){
        if(range == null || whitelist == null) return;
        if(blockPos.getX() <= range.getMaxX() && blockPos.getX() >= range.getMinX() &&
                blockPos.getY() <= range.getMaxY() && blockPos.getY() >= range.getMinY() &&
                blockPos.getZ() <= range.getMaxZ() && blockPos.getZ() >= range.getMinZ()){
            if (whitelist.contains(blockState.getBlock())) {
                selectedBlocks.add(blockPos);
            }

            if(worldEaterMode && (getBlastResistance(blockState, blockState.getFluidState()).isPresent() && getBlastResistance(blockState, blockState.getFluidState()).get() > 9)
                    && blockState.getPistonBehavior() != PistonBehavior.DESTROY){
                selectedBlocks.add(blockPos);
            }
        }
        checkProcessing();
    }
    public static Optional<Float> getBlastResistance(BlockState blockState, FluidState fluidState) {
        return blockState.isAir() && fluidState.isEmpty() ? Optional.empty() : Optional.of(Math.max(blockState.getBlock().getBlastResistance(), fluidState.getBlastResistance()));
    }

}
