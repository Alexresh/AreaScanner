package ru.obabok.arenascanner.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import com.google.common.collect.ImmutableList;
import ru.obabok.arenascanner.client.util.ChunkScheduler;
import ru.obabok.arenascanner.client.util.References;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Config implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = References.MOD_ID + ".json";
    private static final String GENERIC_KEY = References.MOD_ID+".config.generic";
    public static class Generic
    {
        public static final ConfigInteger UNLOADED_CHUNK_MAX_DISTANCE = new ConfigInteger("unloadedChunkMaxDistance", 300).apply(GENERIC_KEY);
        public static final ConfigInteger SELECTED_BLOCKS_MAX_DISTANCE = new ConfigInteger("selectedBlocksMaxDistance", -1).apply(GENERIC_KEY);
        public static final ConfigInteger UNLOADED_CHUNK_Y_OFFSET = new ConfigInteger("unloadedChunkYOffset", -50).apply(GENERIC_KEY);
        public static final ConfigFloat UNLOADED_CHUNK_SCALE = new ConfigFloat("unloadedChunkScale", 4.0f).apply(GENERIC_KEY);
        public static final ConfigColor UNLOADED_CHUNK_COLOR = new ConfigColor("unloadedChunkColor", "#00ffec59").apply(GENERIC_KEY);
        public static final ConfigColor SELECTED_BLOCKS_COLOR = new ConfigColor("selectedBlocksColor", "#FF4dd091").apply(GENERIC_KEY);
        public static final ConfigInteger PROCESS_COOLDOWN = new ConfigInteger("processCooldown", 20).apply(GENERIC_KEY);
        public static final ConfigInteger SELECTED_BLOCKS_MOVE_MAX_DISTANCE = new ConfigInteger("selectedBlocksMoveMaxDistance", 200).apply(GENERIC_KEY);
        public static final ConfigInteger SELECTED_BLOCKS_MOVE_MIN_DISTANCE = new ConfigInteger("selectedBlocksMoveMinDistance", 170).apply(GENERIC_KEY);
        public static final ConfigHotkey LOOK_RANDOM_SELECTED_BLOCK = new ConfigHotkey("lookRandomSelectedBlock", "").apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed MAIN_RENDER = new ConfigBooleanHotkeyed("mainRender", true, "").apply(GENERIC_KEY);
        public static final ConfigHotkey MAIN = new ConfigHotkey("mainHotkey", "LEFT_ALT,RIGHT_BRACKET").apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                UNLOADED_CHUNK_MAX_DISTANCE,
                SELECTED_BLOCKS_MAX_DISTANCE,
                SELECTED_BLOCKS_MOVE_MAX_DISTANCE,
                SELECTED_BLOCKS_MOVE_MIN_DISTANCE,
                UNLOADED_CHUNK_Y_OFFSET,
                UNLOADED_CHUNK_SCALE,
                UNLOADED_CHUNK_COLOR,
                SELECTED_BLOCKS_COLOR,
                PROCESS_COOLDOWN,
                LOOK_RANDOM_SELECTED_BLOCK,
                MAIN_RENDER,
                MAIN

        );
    }

    private static final String HUD_KEY = References.MOD_ID+".config.hud";
    public static class Hud
    {
        public static final ConfigBooleanHotkeyed HUD_ENABLE = new ConfigBooleanHotkeyed("hudEnable", true, "").apply(HUD_KEY);
        public static final ConfigInteger HUD_POS_X = new ConfigInteger("hudPosX", 0).apply(HUD_KEY);
        public static final ConfigInteger HUD_POS_Y = new ConfigInteger("hudPosY", 0).apply(HUD_KEY);
        public static final ConfigColor HUD_SELECTED_BLOCKS_COLOR = new ConfigColor("selectedBlocksHudColor", "#00FFFFFF").apply(HUD_KEY);
        public static final ConfigColor HUD_UNCHECKED_CHUNKS_COLOR = new ConfigColor("uncheckedChunksHudColor", "#00FFFFFF").apply(HUD_KEY);
        public static final ConfigFloat HUD_SCALE = new ConfigFloat("hudScale", 1.0f).apply(HUD_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HUD_ENABLE,
                HUD_POS_X,
                HUD_POS_Y,
                HUD_SELECTED_BLOCKS_COLOR,
                HUD_UNCHECKED_CHUNKS_COLOR,
                HUD_SCALE
        );
    }

    public static final List<IHotkey> HOTKEYS = ImmutableList.of(Generic.MAIN,Generic.MAIN_RENDER, Generic.LOOK_RANDOM_SELECTED_BLOCK, Hud.HUD_ENABLE);
    @Override
    public void onConfigsChanged() {
        saveToFile();
        loadFromFile();
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }

    public void loadFromFile() {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            JsonElement element = JsonUtils.parseJsonFileAsPath(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "HUD", Hud.OPTIONS);
                ChunkScheduler.updatePeriod(Config.Generic.PROCESS_COOLDOWN.getIntegerValue());
            }
            else
            {
                References.LOGGER.error("loadFromFile(): Failed to load config file '{}'.", configFile.toAbsolutePath());
            }
        }
    }

    public void saveToFile() {
        Path dir = FileUtils.getConfigDirectoryAsPath();

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir))
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hud", Hud.OPTIONS);

            JsonUtils.writeJsonToFileAsPath(root, dir.resolve(CONFIG_FILE_NAME));
        }
        else
        {
            References.LOGGER.error("saveToFile(): Config Folder '{}' does not exist!", dir.toAbsolutePath());
        }
    }
}
