package ru.obabok.client.util.Lavobsidian;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import ru.obabok.client.serializes.BlockBoxSerializer;
import ru.obabok.client.serializes.BlockPosSerializer;
import ru.obabok.client.serializes.ChunkPosSerializer;
import ru.obabok.client.util.WhitelistManager;
import ru.obabok.common.References;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LavaScanState {
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lavobsidian-Save-Worker");
        t.setDaemon(true);
        return t;
    });

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosSerializer())
            .registerTypeAdapter(ChunkPos.class, new ChunkPosSerializer())
            .registerTypeAdapter(BlockBox.class, new BlockBoxSerializer())
            .setPrettyPrinting()
            .create();

    private static final Path configPath = Path.of(WhitelistManager.stringWhitelistsPath).getParent().resolve("savedLavaScan.json");

    public String dimension;
    public List<LavaMarkerDto> waypoints = new ArrayList<>();
    public Set<BlockPos> predictedObsidian = new HashSet<>();
    public Set<BlockPos> rememberedFlows = new HashSet<>();

    public static class LavaMarkerDto {
        public BlockPos pos;
        public LavaMarker.Type type;
        public Set<BlockPos> trackedLava;
        public Set<BlockPos> predictedObsidian;

        public LavaMarkerDto() {}

        public LavaMarkerDto(LavaMarker marker) {
            this.pos = marker.pos;
            this.type = marker.type;
            this.trackedLava = marker.trackedLavaBlocks;
            this.predictedObsidian = marker.predictedObsidianBlocks;
        }

        public LavaMarker toMarker() {
            LavaMarker.Type markerType = type != null ? type : LavaMarker.Type.LAVA_WAYPOINT;
            Set<BlockPos> lava = trackedLava != null ? trackedLava : Collections.emptySet();
            Set<BlockPos> obsidian = predictedObsidian != null ? predictedObsidian : Collections.emptySet();
            return new LavaMarker(pos, markerType, markerType.defaultColor, lava, obsidian);
        }
    }

    public static boolean hasSavedScan() {
        return Files.exists(configPath);
    }

    public static void save(String dimension, List<LavaMarker> markerList, Set<BlockPos> predictedObsidianSet, Set<BlockPos> flows) {
        if (markerList == null || markerList.isEmpty()) {
            delete();
            return;
        }
        LavaScanState state = new LavaScanState();
        state.dimension = dimension;
        state.predictedObsidian = new HashSet<>(predictedObsidianSet);
        state.rememberedFlows = new HashSet<>(flows);
        for (LavaMarker m : markerList) {
            state.waypoints.add(new LavaMarkerDto(m));
        }

        SAVE_EXECUTOR.submit(() -> {
            try {
                Files.createDirectories(configPath.getParent());
                try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(state, writer);
                }
            } catch (IOException e) {
                References.LOGGER.error("Failed to save lava scan: " + e.getMessage());
            }
        });
    }

    public static LavaScanState load() {
        if (!Files.exists(configPath)) return null;
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, LavaScanState.class);
        } catch (Exception e) {
            References.LOGGER.error("Failed to load lava scan: " + e.getMessage());
            return null;
        }
    }

    public static void delete() {
        SAVE_EXECUTOR.submit(() -> {
            try {
                Files.deleteIfExists(configPath);
            } catch (IOException ignored) {}
        });
    }
}
