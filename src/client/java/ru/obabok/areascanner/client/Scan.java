package ru.obabok.areascanner.client;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jme.JmePlatform;
import ru.obabok.areascanner.client.models.ScanState;
import ru.obabok.areascanner.client.models.Whitelist;
import ru.obabok.areascanner.client.models.WhitelistItem;
import ru.obabok.areascanner.client.util.*;

import java.util.*;

import static net.minecraft.block.PistonBlock.EXTENDED;

public class Scan {
    public static HashSet<BlockPos> selectedBlocks = new HashSet<>();
    public static HashSet<ChunkPos> unloadedChunks = new HashSet<>();
    private static Whitelist whitelist;
    private static BlockBox range;
    private static boolean processing = false;
    private static long allChunksCounter;
    private static String currentFilename;
    public static final List<String> comparisonOperatorsValues = List.of("=", "≠", ">", "<", "≥", "≤");
    public static final List<String> equalsOperatorsValues = List.of("=", "≠");
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
        selectedBlocks.clear();
        unloadedChunks.clear();
        allChunksCounter = 0;
        range = null;
        currentFilename = null;
        ChunkScheduler.clearQueue();
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
        if(!processing || range == null || world == null || whitelist == null || chunkPos == null) return;
        if(!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) return;
        if((chunkPos.x >= range.getMinX() >> 4) && (chunkPos.x <= range.getMaxX() >> 4) && (chunkPos.z >= range.getMinZ() >> 4) && (chunkPos.z <= range.getMaxZ() >> 4)){
            //delete destroyed blocks from selected blocks
            updateChunk(chunkPos, world);
            //add new blocks to selected blocks
            for (int x = 0; x < 16; x++) {
                for (int y = range.getMinY(); y <= range.getMaxY(); y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos blockPos = new BlockPos(chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
                        processBlock(blockPos, world.getBlockState(blockPos), world);
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
                if(!checkBlock(world.getBlockState(blockPos), world, blockPos)){
                    iterator.remove();
                }
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
        if(processing && selectedBlocks.isEmpty() && unloadedChunks.isEmpty()) {
            if(MinecraftClient.getInstance().player != null){
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Scan finished"),false);
                MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            stopScan();
        }
    }
    //test lua
    private static LuaFunction filterFunc;
    public static void initLua(){
        String script = "function luaShouldInclude(blockInfo)\n" +
                "    if blockInfo.pistonBehavior ~= \"DESTROY\" and blockInfo.blastResistance ~= nil and blockInfo.blastResistance > 9 then\n" +
                "        return true\n" +
                "    end\n" +
                "    return false\n" +
                "end";
        Globals globals = JmePlatform.standardGlobals();
        globals.load(script).call();
        filterFunc = (LuaFunction) globals.get(LuaValue.valueOf("luaShouldInclude"));
    }

    //test lua
    public static boolean luaShouldInclude(BlockState blockState, World world, BlockPos pos) {
        LuaTable args = LuaValue.tableOf();
        String blockName = Registries.BLOCK.getId(blockState.getBlock()).toString();
        args.set("block", LuaValue.valueOf(blockName));
        args.set("x", LuaValue.valueOf(pos.getX()));
        args.set("y", LuaValue.valueOf(pos.getY()));
        args.set("z", LuaValue.valueOf(pos.getZ()));

        args.set("waterlogged", LuaValue.valueOf(blockState.get(Properties.WATERLOGGED, false)));

        Optional<Float> resistanceOpt = getBlastResistance(blockState, blockState.getFluidState());
        resistanceOpt.ifPresent(aFloat -> args.set("blastResistance", LuaValue.valueOf(aFloat)));

        PistonBehavior actual = isMovable(blockState, world, pos);
        args.set("pistonBehavior", LuaValue.valueOf(actual.toString()));

        LuaValue result = filterFunc.call(args);
        return result.checkboolean();
    }

    private static boolean checkBlock(BlockState blockState, World world, BlockPos pos){
        boolean meet = false;
        for(WhitelistItem whitelistItem : whitelist.whitelist){
            boolean insideMeet = true;
            if(whitelistItem.block != null && whitelistItem.block != blockState.getBlock()){
                insideMeet = false;
            }

            //waterlogged
            if (whitelistItem.waterlogged != null) {
                boolean waterlogged = blockState.get(Properties.WATERLOGGED, false);
                if (Boolean.parseBoolean(whitelistItem.waterlogged) != waterlogged) {
                    insideMeet = false;
                }
            }
            //blastResistance
            if(whitelistItem.blastResistance != null){
                BlockState state = blockState;
                Optional<Float> resistanceOpt = getBlastResistance(state, state.getFluidState());
                if(resistanceOpt.isPresent()){
                    String input = whitelistItem.blastResistance.trim();
                    String operator = null;
                    String numberPart = null;
                    for (String op : comparisonOperatorsValues) {
                        if (input.startsWith(op)) {
                            operator = op;
                            numberPart = input.substring(op.length()).trim();
                            break;
                        }
                    }
                    if (operator == null || numberPart.isEmpty()) {
                        References.LOGGER.warn("invalid blastResistance format: {}", whitelistItem.blastResistance);
                        return false;
                    }
                    if (!isParsableToInt(numberPart)) {
                        References.LOGGER.warn("invalid number in blastResistance: {}", numberPart);
                        return false;
                    }
                    int threshold = Integer.parseInt(numberPart);

                    float actualResistance = resistanceOpt.get();
                    boolean matches = switch (operator) {
                        case "=" -> actualResistance == threshold;
                        case "≠" -> actualResistance != threshold;
                        case ">"  -> actualResistance >  threshold;
                        case "<"  -> actualResistance <  threshold;
                        case "≥" -> actualResistance >= threshold;
                        case "≤" -> actualResistance <= threshold;
                        default -> false;
                    };
                    if (!matches) {
                        insideMeet = false;
                    }
                }else{
                    insideMeet = false;
                }
            }
            //pistonBehavior
            if(whitelistItem.pistonBehavior != null){
                String input = whitelistItem.pistonBehavior.trim();

                String operator = null;
                String behaviorPart = null;
                for (String op : equalsOperatorsValues) {
                    if (input.startsWith(op)) {
                        operator = op;
                        behaviorPart = input.substring(op.length()).trim();
                        break;
                    }
                }
                if (operator == null || behaviorPart.isEmpty()) {
                    // Неверный формат
                    References.LOGGER.warn("invalid pistonBehavior format: {}", whitelistItem.pistonBehavior);
                    insideMeet = false;// или throw, или пропустить
                }
                try {
                    PistonBehavior behavior = PistonBehavior.valueOf(behaviorPart);
                    PistonBehavior actual = isMovable(blockState, world, pos);
                    boolean matches = switch (operator) {
                        case "=" -> behavior == actual;
                        case "≠" -> behavior != actual;
                        default -> false;
                    };
                    if (!matches) {
                        insideMeet = false;
                    }

                }catch (Exception e){
                    References.LOGGER.error("PistonBehavior is corrupted");
                    insideMeet = false;
                }
            }
            meet = meet || insideMeet;
        }
        return meet;
    }

    //adds blocks to selected blocks
    public static void processBlock(BlockPos blockPos, BlockState blockState, World world){
        if(range == null || whitelist == null) return;
        if(blockPos.getX() <= range.getMaxX() && blockPos.getX() >= range.getMinX() &&
                blockPos.getY() <= range.getMaxY() && blockPos.getY() >= range.getMinY() &&
                blockPos.getZ() <= range.getMaxZ() && blockPos.getZ() >= range.getMinZ()){
//            if(luaShouldInclude(blockState, world, blockPos)){
//                selectedBlocks.add(blockPos);
//            }
            if(checkBlock(blockState, world, blockPos)){
                selectedBlocks.add(blockPos);
            }
        }
        checkProcessing();
    }
    public static Optional<Float> getBlastResistance(BlockState blockState, FluidState fluidState) {
        return blockState.isAir() && fluidState.isEmpty() ? Optional.empty() : Optional.of(Math.max(blockState.getBlock().getBlastResistance(), fluidState.getBlastResistance()));
    }


    public static PistonBehavior isMovable(BlockState state, World world, BlockPos pos) {
        if (state.isAir()) {
            return PistonBehavior.NORMAL;
        } else if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.CRYING_OBSIDIAN) && !state.isOf(Blocks.RESPAWN_ANCHOR) && !state.isOf(Blocks.REINFORCED_DEEPSLATE)) {

                if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.STICKY_PISTON)) {
                    if (state.getHardness(world, pos) == -1.0F) {
                        return PistonBehavior.IMMOVABLE;
                    }

                    switch (state.getPistonBehavior()) {
                        case BLOCK -> {
                            return PistonBehavior.IMMOVABLE;
                        }
                        case DESTROY -> {
                            return PistonBehavior.DESTROY;
                        }
                        case PUSH_ONLY -> {
                            return PistonBehavior.NORMAL;
                        }
                    }
                } else if (state.get(EXTENDED)) {
                    return PistonBehavior.IMMOVABLE;
                }

                return (state.hasBlockEntity() ? PistonBehavior.IMMOVABLE : PistonBehavior.NORMAL);

        } else {
            return PistonBehavior.IMMOVABLE;
        }
    }
}
