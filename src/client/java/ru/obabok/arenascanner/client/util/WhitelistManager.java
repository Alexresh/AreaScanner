package ru.obabok.arenascanner.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import ru.obabok.arenascanner.client.models.Whitelist;
import ru.obabok.arenascanner.client.models.WhitelistItem;
import ru.obabok.arenascanner.client.serializes.BlockSerializer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WhitelistManager {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Block.class, new BlockSerializer())
            //.registerTypeAdapter(Optional.class, new OptionalTypeSerializer())
            .setPrettyPrinting()
            .create();
    public static final String stringWhitelistsPath = "config/ArenaScanner/scan_whitelists";

    public static final Path pathToWhitelists = Path.of(stringWhitelistsPath);


    public static void saveData(Whitelist data, String filename) {
        try {
            Files.createDirectories(pathToWhitelists);
            try (Writer writer = Files.newBufferedWriter(pathToWhitelists.resolve(filename), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
        }
    }

    public static Whitelist loadData(String filename) {
        if (!Files.exists(pathToWhitelists)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(pathToWhitelists.resolve(filename), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, Whitelist.class);
        } catch (IOException e) {
            References.LOGGER.error(e.getMessage());
            return null;
        }
    }

    public static void removeFromWhitelist(String filename, WhitelistItem item) {
        Whitelist whitelist = loadData(filename);
        whitelist.whitelist.remove(item);
        saveData(whitelist, filename);
    }

    public static boolean createWhitelist(String name) {
        Path toFile = Path.of(stringWhitelistsPath, name + ".json");
        File file = toFile.toFile();
        Path.of(stringWhitelistsPath).toFile().mkdirs();
        try {
            return file.createNewFile();
        }catch (Exception ex){
            return false;
        }
    }

    public static boolean deleteWhitelist(String filename) {
        try{
            Path toFile = pathToWhitelists.resolve(filename);
            File file = toFile.toFile();
            return file.delete();
        }catch (Exception e){
            return false;
        }

    }

    public static int printWhitelist(ClientPlayerEntity player, String whitelist) {
        Whitelist printWhitelist = loadData(whitelist);
        player.sendMessage(Text.literal("Whitelist " + whitelist), false);
        for (WhitelistItem whitelistItem : printWhitelist.whitelist) {
            player.sendMessage(Text.literal(whitelistItem.toString()), false);
        }
        return 1;
    }
}
