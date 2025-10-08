package ru.obabok.arenascanner.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.obabok.arenascanner.client.serializes.BlockSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class WhitelistsManager {

    public static final String stringWhitelistsPath = "config/ArenaScanner/scan_whitelists";

    public static final Path pathToWhitelists = Path.of(stringWhitelistsPath);







    public static int addToWhitelist(ClientPlayerEntity player, String filename, Block block){
        ArrayList<Block> blocks = loadWhitelist(filename);
        if(blocks == null){
            player.sendMessage(Text.literal("blocks is null!"),false);
            return 0;
        }
        if(!blocks.contains(block)){
            blocks.add(block);
            saveWhitelist(player, blocks, filename);
            player.sendMessage(Text.literal("Added"),false);
        }else{
            player.sendMessage(Text.literal("Already in list"),false);
        }


        return 1;
    }
    public static int deleteWhitelist(ClientPlayerEntity player, String filename){
        Path toFile = Path.of(stringWhitelistsPath, filename + ".txt");
        File file = toFile.toFile();
        player.sendMessage(Text.literal(file.delete() ? "Deleted" : "File was not deleted"), false);
        return 1;
    }
    public static int createWhitelist(ClientPlayerEntity player, String filename){
        Path toFile = Path.of(stringWhitelistsPath, filename + ".txt");
        File file = toFile.toFile();
        Path.of(stringWhitelistsPath).toFile().mkdirs();
        try {
            if(file.createNewFile()){
                player.sendMessage(Text.literal("Created"),false);
            }
        }catch (Exception ex){
            player.sendMessage(Text.literal("Exception during write file: " + ex.getMessage() + file),false);
            return 0;
        }
        return 1;
    }
    public static int removeFromWhitelist(ClientPlayerEntity player, String filename, Block block){
        ArrayList<Block> blocks = loadWhitelist(filename);
        if(blocks == null){
            player.sendMessage(Text.literal("blocks is null!"),false);
            return 0;
        }
        blocks.remove(block);
        saveWhitelist(player, blocks, filename);
        player.sendMessage(Text.literal("Removed"),false);
        return 1;
    }

    public static int printWhitelist(ClientPlayerEntity player, String filename){
        ArrayList<Block> blocks = loadWhitelist(filename);
        if(blocks == null){
            player.sendMessage(Text.literal("blocks is null!"),false);
            return 0;
        }
        for (Block block : blocks) {
            player.sendMessage(block.getName(), false);
        }
        return 1;
    }

    public static void saveWhitelist(ClientPlayerEntity player, ArrayList<Block> values, String filename){
        Path toFile = Path.of(stringWhitelistsPath, filename + ".txt");
        try {
            FileWriter writer = new FileWriter(toFile.toString());
            for (Block block : values) {
                writer.write(Registries.BLOCK.getId(block)+ "\n");
            }
            writer.close();
        }catch (Exception ex){
            player.sendMessage(Text.literal("Exception during write file: " + ex.getMessage()),false);

        }
    }
    public static ArrayList<Block> loadWhitelist(String filename){
        Path toFile = Path.of(stringWhitelistsPath, filename  + ".txt");
        ArrayList<Block> whitelist = new ArrayList<>();

        if(filename.isEmpty()) return whitelist;

        try{
            FileReader reader = new FileReader(toFile.toString());
            Scanner scan = new Scanner(reader);
            while(scan.hasNextLine()){
                String fileString = scan.nextLine();
                //WARNING TEST NEEDED
                Identifier blockId = Identifier.of(fileString);
                Block block = Registries.BLOCK.get(blockId);
                if(Registries.BLOCK.getId(block).toString().equals(fileString)){
                    whitelist.add(block);
                }else {
                    References.LOGGER.error("Unknown block:{}", fileString);
                }
            }
            reader.close();
            return whitelist;
        }catch (Exception ex){
            MinecraftClient.getInstance().player.sendMessage(Text.literal("Exception during read file: " + ex.getMessage()),false);
        }
        return null;
    }

}
