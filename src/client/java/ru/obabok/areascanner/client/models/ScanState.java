package ru.obabok.areascanner.client.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.areascanner.client.serializes.BlockBoxSerializer;
import ru.obabok.areascanner.client.serializes.BlockPosSerializer;
import ru.obabok.areascanner.client.serializes.BlockSerializer;
import ru.obabok.areascanner.client.serializes.ChunkPosSerializer;
import ru.obabok.areascanner.client.util.WhitelistManager;
import ru.obabok.areascanner.client.util.References;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class ScanState {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, new BlockPosSerializer())
            .registerTypeAdapter(ChunkPos.class, new ChunkPosSerializer())
            .registerTypeHierarchyAdapter(Block.class, new BlockSerializer())
            .registerTypeAdapter(BlockBox.class, new BlockBoxSerializer())
            .setPrettyPrinting()
            .create();

    public Set<BlockPos> selectedBlocks;
    public Set<ChunkPos> unloadedChunks;
    public Whitelist whitelist;
    public BlockBox range;
    public long allChunksCounter;
    public String currentFilename;

    private static final Path configPath = Path.of(WhitelistManager.stringWhitelistsPath).getParent().resolve("savedScan.json");

    public ScanState(Set<BlockPos> _selectedBlocks, Set<ChunkPos> _unloadedChunks, Whitelist _whitelist, BlockBox _range, long _allChunksCounter, String _currentFilename){
        this.selectedBlocks = _selectedBlocks;
        this.unloadedChunks = _unloadedChunks;
        this.whitelist = _whitelist;
        this.range = _range;
        this.allChunksCounter = _allChunksCounter;
        this.currentFilename = _currentFilename;
    }

    public static void saveData(ScanState data) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
        }
    }

    public static ScanState loadData() {
        if (!Files.exists(configPath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, ScanState.class);
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
            return null;
        }
    }
}
