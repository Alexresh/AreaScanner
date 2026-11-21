package ru.obabok.client.util.ObsidianPredictor;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import ru.obabok.client.Config;
import ru.obabok.client.Scan;
import ru.obabok.client.render.RenderUtil;
import ru.obabok.common.model.BlockArea;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncObsidianPredictor {
    private static final AtomicBoolean running = new AtomicBoolean();
    private static final AtomicLong generation = new AtomicLong();

    private static final List<LavaMarker> waypoints = new CopyOnWriteArrayList<>();
    private static final Set<BlockPos> predictedObsidianBlocks = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> rememberedWaterFlows = ConcurrentHashMap.newKeySet();
    private static final Set<ChunkPos> unloadedChunks = ConcurrentHashMap.newKeySet();

    private static volatile PendingObsidianScan pendingScan;
    private static String markerDimension;
    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final AtomicBoolean isRechecking = new AtomicBoolean(false);
    private static final Map<BlockPos, Long> recentRechecks = new ConcurrentHashMap<>();

    public static void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            loadFromDisk();
        }
    }

    public static void loadFromDisk() {
        LavaScanState saved = LavaScanState.load();
        if (saved != null && saved.waypoints != null && !saved.waypoints.isEmpty()) {
            waypoints.clear();
            for (LavaScanState.LavaMarkerDto dto : saved.waypoints) {
                if (dto.pos != null) {
                    waypoints.add(dto.toMarker());
                }
            }
            predictedObsidianBlocks.clear();
            if (saved.predictedObsidian != null) {
                predictedObsidianBlocks.addAll(saved.predictedObsidian);
            }
            rememberedWaterFlows.clear();
            if (saved.rememberedFlows != null) {
                rememberedWaterFlows.addAll(saved.rememberedFlows);
            }
            markerDimension = saved.dimension;
        }
    }

    public static void saveCurrentState() {
        if (markerDimension != null && !waypoints.isEmpty()) {
            LavaScanState.save(markerDimension, waypoints, predictedObsidianBlocks, rememberedWaterFlows);
        } else {
            LavaScanState.delete();
        }
    }

    public static void startAsyncScan(Level world, AABB range) {
        if (range == null) return;
        startAsyncScan(world, List.of(range));
    }

    public static void startAsyncScan(Level world, BlockArea area) {
        if (area == null || area.isEmpty()) return;
        List<AABB> ranges = new ArrayList<>(area.size());
        for (BlockBox box : area.getBoxes()) {
            ranges.add(new AABB(
                    box.min().getX(), box.min().getY(), box.min().getZ(),
                    box.max().getX(), box.max().getY(), box.max().getZ()
            ));
        }
        startAsyncScan(world, ranges);
    }

    private static void startAsyncScan(Level world, List<AABB> ranges) {
        if (world == null || ranges.isEmpty()) return;
        if (!running.compareAndSet(false, true)) {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("[Obsidian Predictor] A scan is already running"));
                }
            });
            return;
        }
        long runId = generation.incrementAndGet();

        try {
            pendingScan = new PendingObsidianScan(world, ranges);
            unloadedChunks.clear();
            unloadedChunks.addAll(pendingScan.getRequiredChunks());
            Scan.renderDirty = true;
            pendingScan.captureLoadedChunks();
            unloadedChunks.removeAll(pendingScan.getCapturedChunks());
            Scan.renderDirty = true;
            if (pendingScan.allChunksCaptured()) {
                beginScan(pendingScan, runId);
            } else {
                report("[Obsidian Predictor] Waiting for chunks in the scan area. Visit the area to load them.");
            }
        } catch (RuntimeException exception) {
            if (generation.get() != runId) return;
            pendingScan = null;
            unloadedChunks.clear();
            Scan.renderDirty = true;
            running.set(false);
            report("[Obsidian Predictor] Could not prepare scan: " + exception.getMessage());
        }
    }

    public static void onChunkLoaded(Level world, ChunkPos chunkPos) {
        if(!Config.Generic.OBSIDIAN_PREDICTOR.getBooleanValue()) return;

        PendingObsidianScan request = pendingScan;
        long runId = generation.get();
        if (request == null || chunkPos == null) return;
        if (Minecraft.getInstance().level != request.world) {
            pendingScan = null;
            unloadedChunks.clear();
            Scan.renderDirty = true;
            running.set(false);
            report("[Obsidian Predictor] Pending scan cancelled because the world changed");
            return;
        }
        if (world != request.world || !request.captureChunk(chunkPos)) return;
        if (request.isChunkCaptured(chunkPos)) {
            unloadedChunks.remove(chunkPos);
            Scan.renderDirty = true;
        }
        if (request.allChunksCaptured()) beginScan(request, runId);
    }

    private static void beginScan(PendingObsidianScan request, long runId) {
        if (pendingScan != request || generation.get() != runId) return;
        pendingScan = null;
        unloadedChunks.clear();
        Scan.renderDirty = true;
        report("[Obsidian Predictor] All scan chunks captured. Starting prediction.");

        int totalLayers = request.regions.stream().mapToInt(region -> region.height).sum();
        AtomicInteger processedLayers = new AtomicInteger();
        AtomicInteger lastReportedPercent = new AtomicInteger(-1);

        Thread scanThread = new Thread(() -> {
            List<LavaMarker> scannedWaypoints = new ArrayList<>();
            Set<BlockPos> scannedObsidian = new HashSet<>();
            Set<BlockPos> scannedFlows = new HashSet<>();

            try {
                for (PendingObsidianScan.RegionSnapshot region : request.regions) {
                    int layerOffset = processedLayers.get();
                    int maxY = region.minY + region.height - 1;

                    ObsidianScanner.ScanOutput output = ObsidianScanner.scan(
                            region.blocks, region.minX, region.minY, region.minZ,
                            region.width, region.height, region.depth, region.specialBlocks,
                            currentY -> {
                                int layersProcessed = layerOffset + maxY - currentY + 1;
                                int percent = (int) (layersProcessed * 100.0 / totalLayers);
                                int roundedPercent = Math.min(100, (percent / 10) * 10);
                                int previousPercent = lastReportedPercent.get();
                                if (roundedPercent > previousPercent
                                        && lastReportedPercent.compareAndSet(previousPercent, roundedPercent)) {
                                    report("[Obsidian Predictor] Progress: " + roundedPercent + "%");
                                }
                            }
                    );

                    scannedWaypoints.addAll(output.waypoints);
                    scannedObsidian.addAll(output.predictedObsidian);
                    scannedFlows.addAll(output.waterFlowPoints);
                    processedLayers.addAndGet(region.height);
                }
            } catch (RuntimeException exception) {
                Minecraft.getInstance().execute(() -> {
                    if (generation.get() != runId) return;
                    running.set(false);
                    unloadedChunks.clear();
                    Scan.renderDirty = true;
                    report("[Obsidian Predictor] Scan failed: " + exception.getMessage());
                });
                return;
            }

            Minecraft.getInstance().execute(() -> {
                try {
                    if (generation.get() != runId) return;
                    waypoints.clear();
                    predictedObsidianBlocks.clear();
                    rememberedWaterFlows.clear();

                    waypoints.addAll(scannedWaypoints);
                    predictedObsidianBlocks.addAll(scannedObsidian);
                    rememberedWaterFlows.addAll(scannedFlows);
                    markerDimension = request.world.dimension().identifier().toString();
                    saveCurrentState();
                    report("[Obsidian Predictor] Scan complete. Found " + waypoints.size() + " blocks to remove");
                } finally {
                    if (generation.get() == runId) {
                        unloadedChunks.clear();
                        running.set(false);
                    }
                }
            });
        }, "Obsidian-Predictor");

        scanThread.setDaemon(true);
        scanThread.start();
    }

    public static void tick(Level world) {
        if(!Config.Generic.OBSIDIAN_PREDICTOR.getBooleanValue()) return;

        ensureInitialized();
        if (world == null || (waypoints.isEmpty() && predictedObsidianBlocks.isEmpty())) return;

        String currentDim = world.dimension().identifier().toString();
        if (markerDimension != null && !markerDimension.equals(currentDim)) {
            // In another dimension; pause ticking without clearing markers
            return;
        }
        if (markerDimension == null) {
            markerDimension = currentDim;
        }

        tickCounter++;
        if (tickCounter % 4 != 0) return;

        BlockPos playerPos = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.blockPosition() : null;
        if (playerPos == null) return;

        List<LavaMarker> toRemove = new ArrayList<>();
        for (LavaMarker marker : waypoints) {
            if (marker.type == LavaMarker.Type.LAVA_WAYPOINT) {
                if (isLavaCleared(world, marker, playerPos)) {
                    toRemove.add(marker);
                }
            } else {
                if (isBlastResistantCleared(world, marker.pos, playerPos)) {
                    toRemove.add(marker);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            List<BlockPos> recheckPositions = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (LavaMarker marker : toRemove) {
                waypoints.remove(marker);
                predictedObsidianBlocks.removeAll(marker.predictedObsidianBlocks);

                int remainingNearby = countNearbyWaypoints(marker.pos, 16.0, 10.0);
                if (remainingNearby > 0) {
                    reportActionBar("[Obsidian Predictor] Cleared waypoint (" + remainingNearby + " nearby remaining)");
                } else {
                    // All nearby waypoints in this area have been removed!
                    if (now - recentRechecks.getOrDefault(marker.pos, 0L) >= 4000L) {
                        boolean alreadyCovered = false;
                        for (BlockPos center : recheckPositions) {
                            if (center.closerThan(marker.pos, 14.0)) {
                                alreadyCovered = true;
                                break;
                            }
                        }
                        if (!alreadyCovered) {
                            recheckPositions.add(marker.pos);
                            recentRechecks.put(marker.pos, now);
                        }
                    } else {
                        reportActionBar("[Obsidian Predictor] Cleared all nearby waypoints");
                    }
                }
            }

            saveCurrentState();

            if (!recheckPositions.isEmpty() && isRechecking.compareAndSet(false, true)) {
                recheckLocalArea(world, recheckPositions.get(0));
            }
        }
    }

    private static int countNearbyWaypoints(BlockPos pos, double radiusXZ, double radiusY) {
        double radiusSq = radiusXZ * radiusXZ;
        int count = 0;
        for (LavaMarker m : waypoints) {
            double dx = m.pos.getX() - pos.getX();
            double dz = m.pos.getZ() - pos.getZ();
            double dy = Math.abs(m.pos.getY() - pos.getY());
            if (dx * dx + dz * dz <= radiusSq && dy <= radiusY) {
                count++;
            }
        }
        return count;
    }

    private static boolean isLavaCleared(Level world, LavaMarker marker, BlockPos playerPos) {
        if (marker.trackedLavaBlocks.isEmpty() || playerPos == null) return true;

        // The player must be within reasonable interaction proximity of this waypoint!
        // If the player is far away, they cannot have cleared this lava formation.
        long dx = (long) marker.pos.getX() - playerPos.getX();
        long dy = (long) marker.pos.getY() - playerPos.getY();
        long dz = (long) marker.pos.getZ() - playerPos.getZ();
        if (dx * dx + dy * dy + dz * dz > 20 * 20) {
            return false;
        }

        int lastCx = Integer.MIN_VALUE;
        int lastCz = Integer.MIN_VALUE;
        net.minecraft.world.level.chunk.ChunkAccess chunk = null;

        List<BlockPos> clearedBlocks = new ArrayList<>();
        for (BlockPos pos : marker.trackedLavaBlocks) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            if (cx != lastCx || cz != lastCz) {
                lastCx = cx;
                lastCz = cz;
                chunk = world.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    return false;
                }
            }

            FluidState fluid = chunk.getFluidState(pos);
            if (!fluid.is(Fluids.LAVA)) {
                clearedBlocks.add(pos);
            }
        }

        if (!clearedBlocks.isEmpty()) {
            marker.trackedLavaBlocks.removeAll(clearedBlocks);
        }

        if (marker.trackedLavaBlocks.isEmpty()) {
            return true; // All lava blocks in this formation are cleared!
        }

        // If the main waypoint block was cleared, update marker.pos to one of the remaining lava blocks!
        if (!marker.trackedLavaBlocks.contains(marker.pos)) {
            marker.pos = marker.trackedLavaBlocks.iterator().next();
        }

        return false;
    }

    private static boolean isBlastResistantCleared(Level world, BlockPos pos, BlockPos playerPos) {
        if (playerPos == null) return false;

        // Player must be within mining proximity
        long dx = (long) pos.getX() - playerPos.getX();
        long dy = (long) pos.getY() - playerPos.getY();
        long dz = (long) pos.getZ() - playerPos.getZ();
        if (dx * dx + dy * dy + dz * dz > 10 * 10) {
            return false;
        }

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        net.minecraft.world.level.chunk.ChunkAccess chunk = world.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }

        BlockState state = chunk.getBlockState(pos);
        return state.isAir() || !ObsidianScanner.isBlastResistantState(state);
    }

    private static void recheckLocalArea(Level world, BlockPos center) {
        int radiusXZ = 8;
        int radiusY = 5;
        int minX = center.getX() - radiusXZ;
        int maxX = center.getX() + radiusXZ;
        int minY = Math.max(world.getMinY(), center.getY() - radiusY);
        int maxY = Math.min(world.getMaxY() - 1, center.getY() + radiusY);
        int minZ = center.getZ() - radiusXZ;
        int maxZ = center.getZ() + radiusXZ;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int numChunksX = maxChunkX - minChunkX + 1;
        int numChunksZ = maxChunkZ - minChunkZ + 1;
        net.minecraft.world.level.chunk.ChunkAccess[] chunks = new net.minecraft.world.level.chunk.ChunkAccess[numChunksX * numChunksZ];

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                net.minecraft.world.level.chunk.ChunkAccess chunk = world.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    reportActionBar("[Obsidian Predictor] Cleared all nearby waypoints");
                    return;
                }
                chunks[(cx - minChunkX) * numChunksZ + (cz - minChunkZ)] = chunk;
            }
        }

        reportActionBar("[Obsidian Predictor] Cleared all nearby waypoints. Rechecking area...");

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        byte[] blockData = new byte[width * height * depth];
        Map<BlockPos, LavaMarker.Type> specialBlocks = new HashMap<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int cxIdx = (x >> 4) - minChunkX;
                for (int z = minZ; z <= maxZ; z++) {
                    int czIdx = (z >> 4) - minChunkZ;
                    net.minecraft.world.level.chunk.ChunkAccess chunk = chunks[cxIdx * numChunksZ + czIdx];
                    mpos.set(x, y, z);
                    BlockState state = chunk.getBlockState(mpos);
                    int idx = ((y - minY) * width + (x - minX)) * depth + (z - minZ);
                    blockData[idx] = ObsidianScanner.classify(state);
                    if (ObsidianScanner.isBlastResistantState(state)) {
                        specialBlocks.put(mpos.immutable(), ObsidianScanner.getBlastResistantType(state));
                    }
                }
            }
        }

        long runId = generation.get();
        Set<BlockPos> localFlows = new HashSet<>();
        BlockPos.MutableBlockPos flowPos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                flowPos.set(x, maxY, z);
                if (rememberedWaterFlows.contains(flowPos)) {
                    localFlows.add(flowPos.immutable());
                }
            }
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return ObsidianScanner.scanLocal(blockData, minX, minY, minZ, width, height, depth,
                        localFlows, specialBlocks);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(output -> {
            Minecraft.getInstance().execute(() -> {
                try {
                    if (output == null || generation.get() != runId || Minecraft.getInstance().level != world) return;

                    BlockPos currentPPos = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.blockPosition() : null;
                    int newFormationsCount = 0;
                    for (LavaMarker newMarker : output.waypoints) {
                        if (newMarker.type == LavaMarker.Type.LAVA_WAYPOINT) {
                            boolean alreadyCovered = false;
                            for (LavaMarker existing : waypoints) {
                                if (existing.pos.equals(newMarker.pos) || !Collections.disjoint(existing.trackedLavaBlocks, newMarker.trackedLavaBlocks)) {
                                    alreadyCovered = true;
                                    break;
                                }
                            }
                            if (!alreadyCovered && !isLavaCleared(world, newMarker, currentPPos)) {
                                waypoints.add(newMarker);
                                predictedObsidianBlocks.addAll(newMarker.predictedObsidianBlocks);
                                newFormationsCount++;
                            }
                        }
                    }
                    rememberedWaterFlows.addAll(output.waterFlowPoints);
                    saveCurrentState();

                    if (newFormationsCount > 0) {
                        reportActionBar("[Obsidian Predictor] Recheck found " + newFormationsCount + " new obsidian formation(s)!");
                    } else {
                        reportActionBar("[Obsidian Predictor] Area clear (no new obsidian formed)");
                    }
                } finally {
                    isRechecking.set(false);
                }
            });
        }).exceptionally(ex -> {
            isRechecking.set(false);
            return null;
        });
    }

    public static boolean hasRenderableMarkers() {
        ensureInitialized();
        return !waypoints.isEmpty();
    }

    public static void render(LevelRenderContext context) {
        if (!hasRenderableMarkers()) return;
        if (Minecraft.getInstance().level != null && markerDimension != null) {
            String currentDim = Minecraft.getInstance().level.dimension().identifier().toString();
            if (!markerDimension.equals(currentDim)) {
                return;
            }
        }
        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        Quaternionf orientation = context.levelState().cameraRenderState.orientation;
        Vec3 lookDirection = RenderUtil.getLookDirection(orientation);

        // Render markers for blocks to remove that cause generation or obstruct with full LOD support
        for (LavaMarker marker : waypoints) {
            double relX = marker.pos.getX() + 0.5 - camera.x;
            double relZ = marker.pos.getZ() + 0.5 - camera.z;
            double distXZ = Math.sqrt(relX * relX + relZ * relZ);
            Color4f color = LavaMarker.getMarkerColor(marker.type);
            if (marker.type == LavaMarker.Type.LAVA_WAYPOINT && marker.trackedLavaBlocks.size() > 1 && distXZ <= 32.0) {
                // When nearby, highlight all remaining lava blocks that belong to this formation
                for (BlockPos lavaPos : marker.trackedLavaBlocks) {
                    RenderUtil.renderWaypointWithLOD(matrices.last().pose(), lavaPos, camera, lookDirection, color);
                }
            } else {
                RenderUtil.renderWaypointWithLOD(matrices.last().pose(), marker.pos, camera, lookDirection, color);
            }
        }
    }

    private static void report(String message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
            }
        });
    }

    private static void reportActionBar(String message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendOverlayMessage(Component.literal(message));
            }
        });
    }

    public static Set<ChunkPos> getUnloadedChunks() {
        return new HashSet<>(unloadedChunks);
    }

    public static List<LavaMarker> getMarkers() {
        ensureInitialized();
        return waypoints;
    }

    public static int getMarkerCount() {
        ensureInitialized();
        return waypoints.size();
    }

    public static BlockPos getNearestMarker(BlockPos playerPos) {
        ensureInitialized();
        if (waypoints.isEmpty()) return null;
        if (playerPos == null) return waypoints.get(0).pos;

        LavaMarker nearest = null;
        long minDistSq = Long.MAX_VALUE;
        for (LavaMarker marker : waypoints) {
            long dx = (long) marker.pos.getX() - playerPos.getX();
            long dy = (long) marker.pos.getY() - playerPos.getY();
            long dz = (long) marker.pos.getZ() - playerPos.getZ();
            long distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = marker;
            }
        }
        return nearest != null ? nearest.pos : null;
    }

    public static boolean canStopOrClear() {
        ensureInitialized();
        return running.get() || !waypoints.isEmpty() || !predictedObsidianBlocks.isEmpty() || !unloadedChunks.isEmpty() || LavaScanState.hasSavedScan();
    }

    public static void stopAndClear() {
        generation.incrementAndGet();
        pendingScan = null;
        unloadedChunks.clear();
        running.set(false);
        int removed = waypoints.size() + predictedObsidianBlocks.size();
        waypoints.clear();
        predictedObsidianBlocks.clear();
        rememberedWaterFlows.clear();
        markerDimension = null;
        LavaScanState.delete();
        Scan.renderDirty = true;
        if (removed > 0) reportActionBar("[Obsidian Predictor] Cleared all lava markers");
    }
}
