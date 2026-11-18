package ru.obabok.client.util.Lavobsidian;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class ObsidianScanner {
    public static final int WATER_SOURCE = 1;
    public static final int LAVA_SOURCE = 1 << 1;
    public static final int AIRLIKE_BLOCK = 1 << 2;
    public static final int LOW_TERRAIN = 1 << 3;
    public static final int BLAST_RESISTANT = 1 << 4;
    public static final int WATER_BLOCK = 1 << 5;
    public static final int BUBBLE_COLUMN = 1 << 6;
    public static final int WATERLOGGED = 1 << 7;

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Set<Block> AIRLIKE = loadAirlikeBlocks();
    private static final Set<Block> TOUGH_BLOCKS = blocks(
            "minecraft:obsidian", "minecraft:vault", "minecraft:trial_spawner",
            "minecraft:crying_obsidian", "minecraft:ender_chest", "minecraft:reinforced_deepslate",
            "minecraft:end_portal_frame", "minecraft:anvil", "minecraft:chipped_anvil",
            "minecraft:damaged_anvil", "minecraft:enchanting_table", "minecraft:ancient_debris",
            "minecraft:netherite_block", "minecraft:heavy_core", "minecraft:respawn_anchor",
            "minecraft:creaking_heart"
    );
    private static final Set<Block> LOW_BLAST_RES_TERRAIN = blocks(
            "minecraft:dirt", "minecraft:grass_block", "minecraft:podzol", "minecraft:coarse_dirt",
            "minecraft:mycelium", "minecraft:rooted_dirt", "minecraft:moss_block", "minecraft:mud",
            "minecraft:muddy_mangrove_roots", "minecraft:crimson_nylium", "minecraft:warped_nylium",
            "minecraft:netherrack", "minecraft:sand", "minecraft:red_sand", "minecraft:gravel",
            "minecraft:soul_sand", "minecraft:soul_soil", "minecraft:calcite", "minecraft:clay",
            "minecraft:dripstone_block", "minecraft:red_sandstone", "minecraft:sandstone"
    );
    private static final Set<Block> FRAGILE_BLOCKS = blocks(
            "minecraft:glow_lichen", "minecraft:mangrove_leaves", "minecraft:small_dripleaf",
            "minecraft:big_dripleaf", "minecraft:big_dripleaf_stem", "minecraft:pointed_dripstone",
            "minecraft:sculk_vein", "minecraft:tube_coral", "minecraft:brain_coral",
            "minecraft:bubble_coral", "minecraft:fire_coral", "minecraft:horn_coral",
            "minecraft:tube_coral_fan", "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan",
            "minecraft:fire_coral_fan", "minecraft:horn_coral_fan", "minecraft:tube_coral_wall_fan",
            "minecraft:brain_coral_wall_fan", "minecraft:bubble_coral_wall_fan",
            "minecraft:fire_coral_wall_fan", "minecraft:horn_coral_wall_fan"
    );

    public static class ScanOutput {
        public final List<LavaMarker> waypoints;
        public final Set<BlockPos> predictedObsidian;
        public final Set<BlockPos> waterFlowPoints;

        public ScanOutput(List<LavaMarker> waypoints, Set<BlockPos> predictedObsidian, Set<BlockPos> waterFlowPoints) {
            this.waypoints = waypoints;
            this.predictedObsidian = predictedObsidian;
            this.waterFlowPoints = waterFlowPoints;
        }
    }

    public static byte classify(BlockState state) {
        int flags = 0;
        boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
        boolean bubbleColumn = state.is(Blocks.BUBBLE_COLUMN);

        if (state.getFluidState().getType() == Fluids.WATER && state.getFluidState().isSource()
                || bubbleColumn || waterlogged) flags |= WATER_SOURCE;
        if (state.getFluidState().getType() == Fluids.LAVA && state.getFluidState().isSource()) flags |= LAVA_SOURCE;
        if (AIRLIKE.contains(state.getBlock())) flags |= AIRLIKE_BLOCK;
        if (LOW_BLAST_RES_TERRAIN.contains(state.getBlock())) flags |= LOW_TERRAIN;
        if (TOUGH_BLOCKS.contains(state.getBlock())) flags |= BLAST_RESISTANT;
        if (state.is(Blocks.WATER)) flags |= WATER_BLOCK;
        if (bubbleColumn) flags |= BUBBLE_COLUMN;
        if (waterlogged) flags |= WATERLOGGED;
        return (byte) flags;
    }

    public static boolean isBlastResistantState(BlockState state) {
        if (state == null) return false;
        return TOUGH_BLOCKS.contains(state.getBlock());
    }

    public static LavaMarker.Type getBlastResistantType(BlockState state) {
        if (state == null) return LavaMarker.Type.TOUGH_BLOCK;
        Block block = state.getBlock();
        if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK) {
            return LavaMarker.Type.ANCIENT_DEBRIS;
        }
        if (block == Blocks.TRIAL_SPAWNER || block == Blocks.VAULT) {
            return LavaMarker.Type.TRIAL_SPAWNER_VAULT;
        }
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return LavaMarker.Type.OBSIDIAN_BLOCK;
        }
        return LavaMarker.Type.TOUGH_BLOCK;
    }

    public static ScanOutput scan(byte[] blockData, int minX, int minY, int minZ, int width, int height, int depth,
                                  Map<BlockPos, LavaMarker.Type> specialBlocks,
                                  Consumer<Integer> progressCallback) {
        if (blockData == null || width <= 0 || height <= 0 || depth <= 0) {
            return new ScanOutput(Collections.emptyList(), Collections.emptySet(), Collections.emptySet());
        }
        int maxY = minY + height - 1;
        short[] layerMatrix = new short[Math.multiplyExact(width, depth)];
        Arrays.fill(layerMatrix, (short) 9);
        Map<Integer, Set<Long>> floodedLava = new HashMap<>();
        Map<Integer, Set<Long>> focusedFloodedLava = new HashMap<>();
        Set<BlockPos> blastResistantBlocks = new HashSet<>();
        List<LavaMarker> waypoints = new ArrayList<>();
        Set<BlockPos> predictedObsidian = new HashSet<>();
        Set<BlockPos> waterFlowPoints = new HashSet<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            Set<Point2> sources = new HashSet<>();
            List<Point2> flows = new ArrayList<>();

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    mutable.set(minX + x, y, minZ + z);
                    byte state = getBlock(blockData, minX, minY, minZ, width, depth, x + minX, y, z + minZ);
                    int index = x * depth + z;

                    if (hasFlag(state, WATER_SOURCE)) {
                        sources.add(new Point2(x, z));
                    } else if ((hasFlag(state, AIRLIKE_BLOCK) || hasFlag(state, LOW_TERRAIN)) && layerMatrix[index] < 9) {
                        flows.add(new Point2(x, z));
                    }

                    if (hasFlag(state, BLAST_RESISTANT)) {
                        blastResistantBlocks.add(mutable.immutable());
                    }
                    layerMatrix[index] = 9;
                }
            }

            for (Point2 source : sources) {
                if (isEnclosedWaterSource(source, sources)) {
                    layerMatrix[source.x * depth + source.z] = 0;
                } else {
                    simulateWater(blockData, source.x, y, source.z, minX, minY, minZ, width, depth,
                            layerMatrix, 0, false, floodedLava, focusedFloodedLava, waterFlowPoints);
                }
            }

            for (Point2 flow : flows) {
                simulateWater(blockData, flow.x, y, flow.z, minX, minY, minZ, width, depth,
                        layerMatrix, 8, true, floodedLava, focusedFloodedLava, waterFlowPoints);
            }

            resolveLavaMarkers(y, minX, minZ, depth, floodedLava, focusedFloodedLava, waypoints, predictedObsidian);
            if (progressCallback != null) progressCallback.accept(y);
        }

        findBlastResistantMarkers(blastResistantBlocks, specialBlocks, waypoints);
        return new ScanOutput(waypoints, predictedObsidian, waterFlowPoints);
    }

    public static ScanOutput scanLocal(byte[] blockData, int minX, int minY, int minZ,
                                       int width, int height, int depth,
                                       Set<BlockPos> rememberedFlows,
                                       Map<BlockPos, LavaMarker.Type> specialBlocks) {
        if (blockData == null || width <= 0 || height <= 0 || depth <= 0) {
            return new ScanOutput(Collections.emptyList(), Collections.emptySet(), Collections.emptySet());
        }
        int maxY = minY + height - 1;
        short[] layerMatrix = new short[Math.multiplyExact(width, depth)];
        Arrays.fill(layerMatrix, (short) 9);
        Map<Integer, Set<Long>> floodedLava = new HashMap<>();
        Map<Integer, Set<Long>> focusedFloodedLava = new HashMap<>();
        Set<BlockPos> blastResistantBlocks = new HashSet<>();
        List<LavaMarker> waypoints = new ArrayList<>();
        Set<BlockPos> predictedObsidian = new HashSet<>();
        Set<BlockPos> waterFlowPoints = new HashSet<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            Set<Point2> sources = new HashSet<>();
            List<Point2> flows = new ArrayList<>();

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    int worldX = minX + x;
                    int worldZ = minZ + z;
                    mutable.set(worldX, y, worldZ);
                    byte state = getBlock(blockData, minX, minY, minZ, width, depth, worldX, y, worldZ);
                    int index = x * depth + z;

                    boolean isRememberedFlow = rememberedFlows != null && rememberedFlows.contains(mutable);

                    if (hasFlag(state, WATER_SOURCE)) {
                        sources.add(new Point2(x, z));
                    } else if (hasFlag(state, AIRLIKE_BLOCK) || hasFlag(state, LOW_TERRAIN)) {
                        if (layerMatrix[index] < 9) {
                            flows.add(new Point2(x, z));
                        } else if (isRememberedFlow && y == maxY) {
                            // Ingress flow from above remembered from earlier scan!
                            flows.add(new Point2(x, z));
                        }
                    }

                    if (hasFlag(state, BLAST_RESISTANT)) {
                        blastResistantBlocks.add(mutable.immutable());
                    }
                    layerMatrix[index] = 9;
                }
            }

            for (Point2 source : sources) {
                if (isEnclosedWaterSource(source, sources)) {
                    layerMatrix[source.x * depth + source.z] = 0;
                } else {
                    simulateWater(blockData, source.x, y, source.z, minX, minY, minZ, width, depth,
                            layerMatrix, 0, false, floodedLava, focusedFloodedLava, waterFlowPoints);
                }
            }

            for (Point2 flow : flows) {
                simulateWater(blockData, flow.x, y, flow.z, minX, minY, minZ, width, depth,
                        layerMatrix, 8, true, floodedLava, focusedFloodedLava, waterFlowPoints);
            }

            resolveLavaMarkers(y, minX, minZ, depth, floodedLava, focusedFloodedLava, waypoints, predictedObsidian);
        }

        findBlastResistantMarkers(blastResistantBlocks, specialBlocks, waypoints);
        return new ScanOutput(waypoints, predictedObsidian, waterFlowPoints);
    }

    private static void simulateWater(byte[] blockData, int startX, int y, int startZ,
                                      int minX, int minY, int minZ, int width, int depth,
                                      short[] layerMatrix, int initialLevel, boolean checkCollisions,
                                      Map<Integer, Set<Long>> floodedLava,
                                      Map<Integer, Set<Long>> focusedFloodedLava,
                                      Set<BlockPos> waterFlowPoints) {
        Deque<FlowNode> queue = new ArrayDeque<>();
        queue.add(new FlowNode(startX, startZ, initialLevel));
        int terminalWaterLevel = 7;
        boolean foundLava = false;

        while (!queue.isEmpty()) {
            FlowNode node = queue.removeFirst();
            if (node.x < 0 || node.x >= width || node.z < 0 || node.z >= depth) continue;
            int index = node.x * depth + node.z;
            if (node.level >= layerMatrix[index]) continue;

            int worldX = minX + node.x;
            int worldZ = minZ + node.z;
            byte state = getBlock(blockData, minX, minY, minZ, width, depth, worldX, y, worldZ);
            if (hasFlag(state, LAVA_SOURCE)) {
                floodedLava.computeIfAbsent(y, ignored -> new HashSet<>()).add(pointKey(node.x, node.z));
            }
            if (checkCollisions && !hasFlag(state, AIRLIKE_BLOCK)) continue;

            layerMatrix[index] = (short) node.level;
            if (waterFlowPoints != null) {
                waterFlowPoints.add(new BlockPos(worldX, y, worldZ));
            }
            if (node.level == terminalWaterLevel || y <= minY) continue;

            int nextLevel = (node.level + 1) % 8;
            byte below = getBlock(blockData, minX, minY, minZ, width, depth, worldX, y - 1, worldZ);
            if (node.level == 0 || !attractsVerticalFlow(below)) {
                for (Direction direction : HORIZONTAL) {
                    queue.addLast(new FlowNode(node.x + direction.getStepX(), node.z + direction.getStepZ(), nextLevel));
                }
            } else if (checkCollisions) {
                terminalWaterLevel = node.level;
            }

            if (hasFlag(below, LAVA_SOURCE)) {
                Set<Long> belowLayer = floodedLava.computeIfAbsent(y - 1, ignored -> new HashSet<>());
                if (belowLayer.add(pointKey(node.x, node.z)) && !foundLava) {
                    focusedFloodedLava.computeIfAbsent(y - 1, ignored -> new HashSet<>()).add(pointKey(node.x, node.z));
                    foundLava = true;
                }
            }
        }
    }

    private static void resolveLavaMarkers(int y, int minX, int minZ, int depth,
                                           Map<Integer, Set<Long>> floodedLava,
                                           Map<Integer, Set<Long>> focusedFloodedLava,
                                           List<LavaMarker> waypoints,
                                           Set<BlockPos> allPredictedObsidian) {
        Set<Long> componentPoints = floodedLava.remove(y);
        if (componentPoints == null || componentPoints.isEmpty()) return;
        Set<Long> focusedPoints = focusedFloodedLava.remove(y);

        while (!componentPoints.isEmpty()) {
            long initial = componentPoints.iterator().next();
            componentPoints.remove(initial);
            Deque<Long> queue = new ArrayDeque<>();
            queue.addLast(initial);
            List<Point2> selected = new ArrayList<>();
            Set<BlockPos> clusterBlocks = new HashSet<>();
            Point2 initialPt = pointFromKey(initial);
            clusterBlocks.add(new BlockPos(minX + initialPt.x, y, minZ + initialPt.z));

            while (!queue.isEmpty()) {
                long current = queue.removeFirst();
                Point2 point = pointFromKey(current);
                for (Direction direction : HORIZONTAL) {
                    long neighbor = pointKey(point.x + direction.getStepX(), point.z + direction.getStepZ());
                    if (componentPoints.remove(neighbor)) {
                        queue.addLast(neighbor);
                        Point2 nPt = pointFromKey(neighbor);
                        clusterBlocks.add(new BlockPos(minX + nPt.x, y, minZ + nPt.z));
                    }
                    if (focusedPoints != null && focusedPoints.contains(neighbor) && isIsolated(neighbor, selected)) {
                        selected.add(pointFromKey(neighbor));
                    }
                }
            }

            if (selected.isEmpty()) selected.add(pointFromKey(initial));
            allPredictedObsidian.addAll(clusterBlocks);

            for (Point2 point : selected) {
                BlockPos waypointPos = new BlockPos(minX + point.x, y, minZ + point.z);
                waypoints.add(new LavaMarker(waypointPos, LavaMarker.Type.LAVA_WAYPOINT,
                        LavaMarker.Type.LAVA_WAYPOINT.defaultColor, clusterBlocks, clusterBlocks));
            }
        }
    }

    private static void findBlastResistantMarkers(Set<BlockPos> blocks, Map<BlockPos, LavaMarker.Type> specialBlocks, List<LavaMarker> waypoints) {
        for (BlockPos pos : blocks) {
            LavaMarker.Type type = specialBlocks != null && specialBlocks.containsKey(pos)
                    ? specialBlocks.get(pos) : LavaMarker.Type.TOUGH_BLOCK;
            waypoints.add(new LavaMarker(pos, type));
        }
    }

    private static boolean isEnclosedWaterSource(Point2 point, Set<Point2> sources) {
        for (Direction direction : HORIZONTAL) {
            if (!sources.contains(new Point2(point.x + direction.getStepX(), point.z + direction.getStepZ()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIsolated(long point, List<Point2> selected) {
        Point2 candidate = pointFromKey(point);
        for (Point2 existing : selected) {
            if (Math.abs(candidate.x - existing.x) + Math.abs(candidate.z - existing.z) < 8) return false;
        }
        return true;
    }

    private static boolean attractsVerticalFlow(byte state) {
        return hasFlag(state, WATER_BLOCK) || hasFlag(state, BUBBLE_COLUMN)
                || hasFlag(state, AIRLIKE_BLOCK) || hasFlag(state, WATERLOGGED);
    }

    private static boolean isBlastResistant(Block block, boolean waterlogged) {
        return waterlogged ? !FRAGILE_BLOCKS.contains(block) : TOUGH_BLOCKS.contains(block);
    }

    private static byte getBlock(byte[] blockData, int minX, int minY, int minZ,
                                 int width, int depth, int x, int y, int z) {
        int index = ((y - minY) * width + (x - minX)) * depth + (z - minZ);
        return blockData[index];
    }

    private static boolean hasFlag(byte state, int flag) {
        return (state & flag) != 0;
    }

    private static Set<Block> loadAirlikeBlocks() {
        try (InputStream stream = ObsidianScanner.class.getResourceAsStream("/airlike_blocks.json")) {
            if (stream == null) throw new IllegalStateException("Missing airlike_blocks.json resource");
            String[] ids = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), String[].class);
            return blocks(ids);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Set<Block> blocks(String... ids) {
        Set<Block> blocks = new HashSet<>();
        for (String id : ids) {
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier != null && BuiltInRegistries.BLOCK.containsKey(identifier)) {
                blocks.add(BuiltInRegistries.BLOCK.getValue(identifier));
            }
        }
        return blocks;
    }

    private static long pointKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static Point2 pointFromKey(long key) {
        return new Point2((int) (key >> 32), (int) key);
    }

    private record Point2(int x, int z) { }

    private record FlowNode(int x, int z, int level) { }
}
