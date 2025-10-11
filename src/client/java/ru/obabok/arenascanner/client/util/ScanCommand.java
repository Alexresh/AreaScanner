package ru.obabok.arenascanner.client.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.math.BlockBox;
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
                                                .executes(context -> Scan.executeAsync(context.getSource().getWorld(), BlockBox.create(
                                                                CBlockPosArgument.getBlockPos(context, "from"),
                                                                CBlockPosArgument.getBlockPos(context, "to")),
                                                        StringArgumentType.getString(context, "whitelist")))))))
                .then(literal("stop").executes(context -> {
                    Scan.stopScan();
                    return 1;
                }))
                .then(literal("whitelists")
                        .then(literal("create")
                                .then(argument("whitelist", StringArgumentType.string())
                                        .executes(context -> WhitelistManager.createWhitelist(StringArgumentType.getString(context, "whitelist")) ? 1 : 0)))
                        .then(literal("delete")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .executes(context -> WhitelistManager.deleteWhitelist(StringArgumentType.getString(context, "whitelist")) ? 1 : 0)))
                        .then(literal("print")
                                .then(argument("whitelist", StringArgumentType.string()).suggests(new FileSuggestionProvider())
                                        .executes(context -> WhitelistManager.printWhitelist(context.getSource().getPlayer(), StringArgumentType.getString(context, "whitelist")))))));

    }
}
