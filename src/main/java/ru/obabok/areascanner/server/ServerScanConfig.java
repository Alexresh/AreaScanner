package ru.obabok.areascanner.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import ru.obabok.areascanner.common.References;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ServerScanConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("AreaScanner/server_config.json");

    public static final Map<String, Integer> VALUES = new HashMap<>();

    // Имена параметров (для suggest и валидации)
    public static final String[] KEYS = {
            "budget", "nbt", "chunks", "packets", "positions", "deltapos", "pendingnbt"
    };

//    public static final int SCAN_BUDGET_MS = 30;
//    public static final int MAX_NBT_READS_PER_TICK = 20;
//    public static final int MAX_CHUNK_LOADS_PER_TICK = 20;
//    public static final int MAX_PACKETS_PER_TICK = 20;
//    public static final int MAX_POSITIONS_PER_PACKET = 1024;
//    public static final int MAX_DELTA_POSITIONS_PER_PACKET = 1024;
//    public static final int MAX_PENDING_NBT = 192;

    private static final Map<String, Integer> DEFAULTS = Map.of(
            "budget", 30,
            "nbt", 20,
            "chunks", 20,
            "packets", 20,
            "positions", 1024,
            "deltapos", 1024,
            "pendingnbt", 192
    );

    // Ограничения: min, max
    public static final Map<String, int[]> BOUNDS = Map.of(
            "budget", new int[]{1, 100},
            "nbt", new int[]{1, 1000},
            "chunks", new int[]{1, 1000},
            "packets", new int[]{1, 1000},
            "positions", new int[]{1, 8192},
            "deltapos", new int[]{1, 8192},
            "pendingnbt", new int[]{1, 1024}
    );

    static {
        resetToDefaults();
    }

    private ServerScanConfig() {}

    public static void resetToDefaults() {
        VALUES.clear();
        VALUES.putAll(DEFAULTS);
    }

    public static int getBudgetMs() {
        return VALUES.get("budget");
    }

    public static int getMaxNbtReadsPerTick() {
        return VALUES.get("nbt");
    }

    public static int getMaxChunkLoadsPerTick() {
        return VALUES.get("chunks");
    }

    public static int getMaxPacketsPerTick() {
        return VALUES.get("packets");
    }

    public static int getMaxPositionsPerPacket() {
        return VALUES.get("positions");
    }

    public static int getMaxDeltaPositionsPerPacket() {
        return VALUES.get("deltapos");
    }

    public static int getMaxPendingNbt() {
        return VALUES.get("pendingnbt");
    }

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }
            String json = Files.readString(CONFIG_PATH);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                for (String key : KEYS) {
                    if (loaded.containsKey(key)) {
                        int val = clamp(loaded.get(key), BOUNDS.get(key)[0], BOUNDS.get(key)[1]);
                        VALUES.put(key, val);
                    }
                }
            }
            //References.LOGGER.info("ServerScanConfig loaded");
        } catch (Exception e) {
            References.LOGGER.error("Failed to load config", e);
            resetToDefaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(CONFIG_PATH, gson.toJson(VALUES));
            References.LOGGER.info("ServerScanConfig saved");
        } catch (IOException e) {
            References.LOGGER.error("Failed to save config", e);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
