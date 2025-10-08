package ru.obabok.arenascanner.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.arenascanner.client.serializes.BlockBoxSerializer;
import ru.obabok.arenascanner.client.serializes.BlockPosSerializer;
import ru.obabok.arenascanner.client.serializes.BlockSerializer;
import ru.obabok.arenascanner.client.serializes.ChunkPosSerializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ScanSaveData {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosSerializer())
            .registerTypeAdapter(ChunkPos.class, new ChunkPosSerializer())
            .registerTypeAdapter(Block.class, new BlockSerializer())
            .registerTypeAdapter(BlockBox.class, new BlockBoxSerializer())
            .setPrettyPrinting()
            .create();

    public Set<BlockPos> selectedBlocks;
    public Set<ChunkPos> unloadedChunks;
    public List<Block> whitelist;
    public BlockBox range;
    public boolean worldEaterMode = false;
    public long allChunksCounter;
    public String currentFilename;

    private static final Path configPath = Path.of(WhitelistsManager.stringWhitelistsPath).getParent().resolve("savedScan.json");

    public ScanSaveData(Set<BlockPos> _selectedBlocks, Set<ChunkPos> _unloadedChunks, List<Block> _whitelist, BlockBox _range, boolean _worldEaterMode, long _allChunksCounter, String _currentFilename){
        this.selectedBlocks = _selectedBlocks;
        this.unloadedChunks = _unloadedChunks;
        this.whitelist = _whitelist;
        this.range = _range;
        this.worldEaterMode = _worldEaterMode;
        this.allChunksCounter = _allChunksCounter;
        this.currentFilename = _currentFilename;
    }

    public static void saveData(ScanSaveData data) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
        }
    }

    public static ScanSaveData loadData() {
        if (!Files.exists(configPath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, ScanSaveData.class);
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
            return null;
        }
    }
}
