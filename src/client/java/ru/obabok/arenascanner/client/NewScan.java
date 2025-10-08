package ru.obabok.arenascanner.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.models.Whitelist;
import ru.obabok.arenascanner.client.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

public class NewScan {
    public static HashSet<BlockPos> selectedBlocks = new HashSet<>();
    public static HashSet<ChunkPos> unloadedChunks = new HashSet<>();
    private static Whitelist whitelist;
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
        whitelist = NewWhitelistManager.loadData(currentFilename);
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
        //todo ScanSaveData.saveData(new ScanSaveData(selectedBlocks, unloadedChunks, whitelist, range, worldEaterMode, allChunksCounter, currentFilename));
        stopScan();
    }
    public static boolean loadState(){
        ScanSaveData saveData = ScanSaveData.loadData();
        if(saveData == null) return false;
        selectedBlocks = (HashSet<BlockPos>) saveData.selectedBlocks;
        unloadedChunks = (HashSet<ChunkPos>) saveData.unloadedChunks;
        //todo whitelist = new ArrayList<>(saveData.whitelist);
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
                whitelist.whitelist.forEach(whitelistItem -> {
                    boolean remove = true;
                    //block check
                    if(whitelistItem.block != null && whitelistItem.block != world.getBlockState(blockPos).getBlock()) remove = false;

                    //waterlogged
                    if(whitelistItem.waterlogged != null && world.getBlockState(blockPos).getProperties().contains(Properties.WATERLOGGED) && world.getBlockState(blockPos).getFluidState().getBlastResistance() > 0.0f){
                        remove = Boolean.parseBoolean(whitelistItem.waterlogged) && remove;
                    }
                    //blastResistance
                    if(whitelistItem.blastResistance != null){
                        BlockState state = world.getBlockState(blockPos);
                        Optional<Float> resistanceOpt = getBlastResistance(state, state.getFluidState());
                        if(resistanceOpt.isEmpty()){
                            remove = true;
                        }else {
                            String input = whitelistItem.blastResistance.trim();
                            String[] operators = {">=", "<=", "==", "!=", ">", "<"};
                            String operator = null;
                            String numberPart = null;
                            for (String op : operators) {
                                if (input.startsWith(op)) {
                                    operator = op;
                                    numberPart = input.substring(op.length()).trim();
                                    break;
                                }
                            }
                            if (operator == null || numberPart.isEmpty()) {
                                // Неверный формат
                                References.LOGGER.warn("[remove] invalid blastResistance format: {}", whitelistItem.blastResistance);
                                return; // или throw, или пропустить
                            }
                            if (!isParsableToInt(numberPart)) {
                                References.LOGGER.warn("[remove] invalid number in blastResistance: {}", numberPart);
                                return;
                            }
                            int threshold = Integer.parseInt(numberPart);
                            if(getBlastResistance(state, state.getFluidState()).isPresent()){
                                float actualResistance = getBlastResistance(state, state.getFluidState()).get();
                                boolean matches = switch (operator) {
                                    case "==" -> actualResistance == threshold;
                                    case "!=" -> actualResistance != threshold;
                                    case ">"  -> actualResistance >  threshold;
                                    case "<"  -> actualResistance <  threshold;
                                    case ">=" -> actualResistance >= threshold;
                                    case "<=" -> actualResistance <= threshold;
                                    default -> false;
                                };
                                remove = !matches && remove;
                            }


                        }
                    }

                    //pistonBehavior
                    if(whitelistItem.pistonBehavior != null){
                        String input = whitelistItem.pistonBehavior.trim();
                        String[] operators = {"==", "!="};
                        String operator = null;
                        String behaviorPart = null;
                        for (String op : operators) {
                            if (input.startsWith(op)) {
                                operator = op;
                                behaviorPart = input.substring(op.length()).trim();
                                break;
                            }
                        }
                        if (operator == null || behaviorPart.isEmpty()) {
                            // Неверный формат
                            References.LOGGER.warn("[remove] invalid pistonBehavior format: {}", whitelistItem.pistonBehavior);
                            return; // или throw, или пропустить
                        }
                        try {
                            PistonBehavior behavior = PistonBehavior.valueOf(behaviorPart);
                            PistonBehavior actual = world.getBlockState(blockPos).getPistonBehavior();
                            boolean matches = switch (operator) {
                                case "==" -> actual == behavior;
                                case "!=" -> actual != behavior;
                                default -> false;
                            };
                            remove = !matches && remove;
                        }catch (Exception e){
                            References.LOGGER.error("PistonBehavior is corrupted");
                            return;
                        }
                    }
                    if(remove){
                        iterator.remove();
                    }
                });
            }
        }
        //checkProcessing();
    }
    private static boolean isParsableToInt(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
            whitelist.whitelist.forEach(whitelistItem -> {
                boolean add = false;
                if(whitelistItem.block != null && whitelistItem.block == blockState.getBlock()) add = true;

                //waterlogged
                if(whitelistItem.waterlogged != null && blockState.getProperties().contains(Properties.WATERLOGGED) && blockState.getFluidState().getBlastResistance() > 0.0f){
                    add = Boolean.parseBoolean(whitelistItem.waterlogged) || add;
                }

                //blastResistance
                if(whitelistItem.blastResistance != null){
                    BlockState state = blockState;
                    Optional<Float> resistanceOpt = getBlastResistance(state, state.getFluidState());
                    if(resistanceOpt.isPresent()){
                        add = false;
                    }else {
                        String input = whitelistItem.blastResistance.trim();
                        String[] operators = {">=", "<=", "==", "!=", ">", "<"};
                        String operator = null;
                        String numberPart = null;
                        for (String op : operators) {
                            if (input.startsWith(op)) {
                                operator = op;
                                numberPart = input.substring(op.length()).trim();
                                break;
                            }
                        }
                        if (operator == null || numberPart.isEmpty()) {
                            // Неверный формат
                            References.LOGGER.warn("[add block] invalid blastResistance format: {}", whitelistItem.blastResistance);
                            return; // или throw, или пропустить
                        }
                        if (!isParsableToInt(numberPart)) {
                            References.LOGGER.warn("[add block] invalid number in blastResistance: {}", numberPart);
                            return;
                        }
                        int threshold = Integer.parseInt(numberPart);
                        if(getBlastResistance(state, state.getFluidState()).isPresent()){
                            float actualResistance = getBlastResistance(state, state.getFluidState()).get();
                            boolean matches = switch (operator) {
                                case "==" -> actualResistance == threshold;
                                case "!=" -> actualResistance != threshold;
                                case ">"  -> actualResistance >  threshold;
                                case "<"  -> actualResistance <  threshold;
                                case ">=" -> actualResistance >= threshold;
                                case "<=" -> actualResistance <= threshold;
                                default -> false;
                            };
                            add = matches || add;
                        }
                    }
                }

                //pistonBehavior
                if(whitelistItem.pistonBehavior != null){
                    String input = whitelistItem.pistonBehavior.trim();
                    String[] operators = {"==", "!="};
                    String operator = null;
                    String behaviorPart = null;
                    for (String op : operators) {
                        if (input.startsWith(op)) {
                            operator = op;
                            behaviorPart = input.substring(op.length()).trim();
                            break;
                        }
                    }
                    if (operator == null || behaviorPart.isEmpty()) {
                        // Неверный формат
                        References.LOGGER.warn("[add block] invalid pistonBehavior format: {}", whitelistItem.pistonBehavior);
                        return; // или throw, или пропустить
                    }
                    try {
                        PistonBehavior behavior = PistonBehavior.valueOf(behaviorPart);
                        PistonBehavior actual = blockState.getPistonBehavior();
                        boolean matches = switch (operator) {
                            case "==" -> actual == behavior;
                            case "!=" -> actual != behavior;
                            default -> false;
                        };
                        add = matches || add;
                    }catch (Exception e){
                        References.LOGGER.error("[add block]PistonBehavior is corrupted");
                        return;
                    }
                }
                if(add){
                    selectedBlocks.add(blockPos);
                }

            });
        }
        checkProcessing();
    }
    public static Optional<Float> getBlastResistance(BlockState blockState, FluidState fluidState) {
        return blockState.isAir() && fluidState.isEmpty() ? Optional.empty() : Optional.of(Math.max(blockState.getBlock().getBlastResistance(), fluidState.getBlastResistance()));
    }

}
