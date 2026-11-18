package ru.obabok.client.util.Lavobsidian;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class PendingObsidianScan {
    final Level world;
    final List<RegionSnapshot> regions;
    final int totalChunks;
    int capturedChunks;

    PendingObsidianScan(Level world, List<AABB> ranges) {
        this.world = world;
        this.regions = ranges.stream().map(RegionSnapshot::new).toList();
        this.totalChunks = regions.stream().mapToInt(region -> region.requiredChunks.size()).sum();
    }

    void captureLoadedChunks() {
        for (RegionSnapshot region : regions) {
            for (int chunkX = region.minX >> 4; chunkX <= region.maxX >> 4; chunkX++) {
                for (int chunkZ = region.minZ >> 4; chunkZ <= region.maxZ >> 4; chunkZ++) {
                    if (captureChunk(region, chunkX, chunkZ)) capturedChunks++;
                }
            }
        }
    }

    boolean captureChunk(ChunkPos chunkPos) {
        int chunkX = chunkPos.getMinBlockX() >> 4;
        int chunkZ = chunkPos.getMinBlockZ() >> 4;
        boolean captured = false;
        for (RegionSnapshot region : regions) {
            if (captureChunk(region, chunkX, chunkZ)) {
                capturedChunks++;
                captured = true;
            }
        }
        return captured;
    }

    boolean allChunksCaptured() {
        return capturedChunks == totalChunks;
    }

    Set<ChunkPos> getRequiredChunks() {
        Set<ChunkPos> chunks = new HashSet<>();
        for (RegionSnapshot region : regions) {
            for (long key : region.requiredChunks) chunks.add(chunkPos(key));
        }
        return chunks;
    }

    Set<ChunkPos> getCapturedChunks() {
        Set<ChunkPos> chunks = new HashSet<>();
        for (ChunkPos chunk : getRequiredChunks()) {
            if (isChunkCaptured(chunk)) chunks.add(chunk);
        }
        return chunks;
    }

    boolean isChunkCaptured(ChunkPos chunkPos) {
        long key = chunkKey(chunkPos.x, chunkPos.z);
        boolean required = false;
        for (RegionSnapshot region : regions) {
            if (region.requiredChunks.contains(key)) {
                required = true;
                if (!region.capturedChunks.contains(key)) return false;
            }
        }
        return required;
    }

    private boolean captureChunk(RegionSnapshot region, int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        if (!region.requiredChunks.contains(key) || region.capturedChunks.contains(key)) return false;
        net.minecraft.world.level.chunk.ChunkAccess chunk = world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) return false;

        int minX = Math.max(region.minX, chunkX << 4);
        int maxX = Math.min(region.maxX, (chunkX << 4) + 15);
        int minZ = Math.max(region.minZ, chunkZ << 4);
        int maxZ = Math.min(region.maxZ, (chunkZ << 4) + 15);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = region.minY; y <= region.maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    region.blocks[region.index(x, y, z)] = ObsidianScanner.classify(state);
                    if (ObsidianScanner.isBlastResistantState(state)) {
                        region.specialBlocks.put(pos.immutable(), ObsidianScanner.getBlastResistantType(state));
                    }
                }
            }
        }
        region.capturedChunks.add(key);
        return true;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static ChunkPos chunkPos(long key) {
        return new ChunkPos((int) (key >> 32), (int) key);
    }

    static final class RegionSnapshot {
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;
        final int width;
        final int height;
        final int depth;
        final byte[] blocks;
        final Set<Long> requiredChunks = new HashSet<>();
        final Set<Long> capturedChunks = new HashSet<>();
        final Map<BlockPos, LavaMarker.Type> specialBlocks = new ConcurrentHashMap<>();

        private RegionSnapshot(AABB range) {
            minX = (int) Math.floor(range.minX);
            minY = (int) Math.floor(range.minY);
            minZ = (int) Math.floor(range.minZ);
            maxX = (int) Math.floor(range.maxX);
            maxY = (int) Math.floor(range.maxY);
            maxZ = (int) Math.floor(range.maxZ);
            width = maxX - minX + 1;
            height = maxY - minY + 1;
            depth = maxZ - minZ + 1;
            if (width <= 0 || height <= 0 || depth <= 0) throw new IllegalArgumentException("Invalid scan area");
            long volume = Math.multiplyExact(Math.multiplyExact((long) width, height), depth);
            if (volume > Integer.MAX_VALUE) throw new IllegalArgumentException("Scan area is too large");
            blocks = new byte[(int) volume];
            for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                    requiredChunks.add(chunkKey(chunkX, chunkZ));
                }
            }
        }

        int index(int x, int y, int z) {
            return ((y - minY) * width + x - minX) * depth + z - minZ;
        }
    }
}
