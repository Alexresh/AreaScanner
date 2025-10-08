package ru.obabok.arenascanner.client.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import dev.xpple.clientarguments.arguments.CBlockStateArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.BlockBox;
import ru.obabok.arenascanner.client.NewScan;
import ru.obabok.arenascanner.client.Scan;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;


public class ScanCommand {


    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        dispatcher.register(literal("scan")
                .then(argument("from", CBlockPosArgument.blockPos())
                        .then(argument("to", CBlockPosArgument.blockPos())
                                .then(literal("whitelist")
                                        .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                                .executes(context -> {
                                                    NewScan.worldEaterMode = false;
                                                    return NewScan.executeAsync(context.getSource().getWorld(), BlockBox.create(
                                                                    CBlockPosArgument.getBlockPos(context, "from"),
                                                                    CBlockPosArgument.getBlockPos(context, "to")),
                                                            StringArgumentType.getString(context, "whitelist"));
                                                })))
                                .then(literal("worldEaterMode")
                                        .executes(context -> {
                                            NewScan.worldEaterMode = true;
                                            return NewScan.executeAsync(context.getSource().getWorld(), BlockBox.create(
                                                            CBlockPosArgument.getBlockPos(context, "from"),
                                                            CBlockPosArgument.getBlockPos(context, "to")),
                                                    "");
                                        }))))
                .then(literal("stop").executes(context -> {
                    NewScan.stopScan();
                    return 1;
                }))
                .then(literal("whitelists")
                        .then(literal("create")
                                .then(argument("whitelist", StringArgumentType.string())
                                        .executes(context -> WhitelistsManager.createWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist")))))
                        .then(literal("delete")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .executes(context -> WhitelistsManager.deleteWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist")))))
                        .then(literal("add_block")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .then(argument("block", CBlockStateArgument.blockState(commandRegistryAccess))
                                                .executes(context -> WhitelistsManager.addToWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist"), CBlockStateArgument.getBlockState(context, "block").getState().getBlock())))))
                        .then(literal("remove_block")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .then(argument("block", CBlockStateArgument.blockState(commandRegistryAccess))
                                                .executes(context -> WhitelistsManager.removeFromWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist"), CBlockStateArgument.getBlockState(context, "block").getState().getBlock())))))
                        .then(literal("print")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .executes(context -> WhitelistsManager.printWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist")))))));

    }
}
