package ru.obabok.areascanner.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ScanConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("scanset")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestKeys(builder))
                        .executes(ScanConfigCommand::showCurrentValue)
                        .then(argument("value", IntegerArgumentType.integer())
                                .executes(ScanConfigCommand::setValue)))
                .then(literal("load").executes(ctx -> {
                    ServerScanConfig.load();
                    ctx.getSource().sendFeedback(() -> Text.literal("Config reloaded."), true);
                    return 1;
                }))
                .then(literal("save").executes(ctx -> {
                    ServerScanConfig.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("Config saved."), true);
                    return 1;
                }))
                .then(literal("reset").executes(ctx -> {
                    ServerScanConfig.resetToDefaults();
                    ctx.getSource().sendFeedback(() -> Text.literal("Config reset to defaults."), true);
                    return 1;
                }))
        );
    }

    private static CompletableFuture<Suggestions> suggestKeys(SuggestionsBuilder builder) {
        for (String key : ServerScanConfig.KEYS) {
            builder.suggest(key);
        }
        return builder.buildFuture();
    }

    private static int showCurrentValue(CommandContext<ServerCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        if (!ServerScanConfig.VALUES.containsKey(key)) {
            ctx.getSource().sendError(Text.literal("Unknown config key: " + key));
            return 0;
        }
        int value = ServerScanConfig.VALUES.get(key);
        ctx.getSource().sendFeedback(() -> Text.literal(key + " = " + value), false);
        return 1;
    }

    private static int setValue(CommandContext<ServerCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        int value = IntegerArgumentType.getInteger(ctx, "value");

        if (!ServerScanConfig.VALUES.containsKey(key)) {
            ctx.getSource().sendError(Text.literal("Unknown config key: " + key));
            return 0;
        }

        int[] bounds = ServerScanConfig.BOUNDS.get(key);
        if (value < bounds[0] || value > bounds[1]) {
            ctx.getSource().sendError(Text.literal(
                    "Value for '" + key + "' must be between " + bounds[0] + " and " + bounds[1]
            ));
            return 0;
        }

        ServerScanConfig.VALUES.put(key, value);
        ctx.getSource().sendFeedback(() -> Text.literal("Set " + key + " = " + value), true);
        return 1;
    }
}
