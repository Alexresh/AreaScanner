package ru.obabok.arenascanner.client.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class FileSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder){
        File directory = new File(WhitelistsManager.stringWhitelistsPath);

        if(directory.exists() || directory.mkdirs()){
            File[] files = directory.listFiles();
            if(files != null){
                for (File file : files) {
                    builder.suggest(file.getName().substring(0, file.getName().length()-4));
                }
            }
        }
        return builder.buildFuture();
    }
}
