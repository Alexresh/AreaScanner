package ru.obabok.areascanner.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import ru.obabok.areascanner.client.Scan;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AmethystAnalyzer {

    private static final int MAX_REMOVALS = 3;
    private static final int PROGRESS_INTERVAL = 10; // вывод прогресса каждые 10%
    private static volatile boolean isRunning = false;
    private static ExecutorService executor;

    public static void start() {
        if (isRunning) {
            sendMessage("Analysis already in progress!");
            return;
        }

        BlockBox range = Scan.getRange();
        if (range == null) {
            sendMessage("No scan range defined!");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.world == null || client.player == null) {
                sendMessage("No active world!");
                return;
            }

            List<BlockPos> budding = collectBuddingAmethysts(client.world, range);
            if (budding.isEmpty()) {
                sendMessage("No budding amethysts found");
                return;
            }

            WorldSnapshot snapshot = new WorldSnapshot(range, client.world, budding);
            startAsyncAnalysis(client, budding, snapshot);
        });
    }

    private static void startAsyncAnalysis(
            MinecraftClient client,
            List<BlockPos> budding,
            WorldSnapshot snapshot
    ) {
        isRunning = true;
        sendMessage("Starting analysis (" + budding.size() + " budding blocks)...");

        int initCount = countAmethysts(snapshot, new HashSet<>(budding));
        sendMessage("Initial count: " + initCount);

        AtomicInteger maxCount = new AtomicInteger(initCount);
        AtomicReference<List<BlockPos>> bestRemoval = new AtomicReference<>(Collections.emptyList());
        AtomicInteger processed = new AtomicInteger(0);

        // Собираем все комбинации
        List<List<BlockPos>> allCombinations = new ArrayList<>();
        int maxK = Math.min(MAX_REMOVALS, budding.size());
        for (int k = 1; k <= maxK; k++) {
            generateCombinations(budding, k, allCombinations::add);
        }

        int total = allCombinations.size();
        if (total == 0) {
            client.execute(() -> {
                isRunning = false;
                sendMessage("✓ No blocks to remove (only 1 budding block)");
            });
            return;
        }

        // Параллельная обработка
        executor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors(), 4),
                r -> {
                    Thread t = new Thread(r, "AmethystAnalyzer");
                    t.setDaemon(true);
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                }
        );

        List<CompletableFuture<Void>> futures = new ArrayList<>(total);
        for (List<BlockPos> combination : allCombinations) {
            futures.add(CompletableFuture.runAsync(() -> {
                if (client.world == null) return; // отмена при выходе из мира

                Set<BlockPos> remaining = new HashSet<>(budding);
                remaining.removeAll(combination);
                int count = countAmethysts(snapshot, remaining);

                // Атомарное обновление максимума
                while (true) {
                    int currentMax = maxCount.get();
                    if (count <= currentMax) break;
                    if (maxCount.compareAndSet(currentMax, count)) {
                        bestRemoval.set(new ArrayList<>(combination));
                        break;
                    }
                }

                // Прогресс
                int done = processed.incrementAndGet();
                if (total > 10 && done % Math.max(1, total / (100 / PROGRESS_INTERVAL)) == 0) {
                    int progress = done * 100 / total;
                    if (progress % PROGRESS_INTERVAL == 0) {
                        client.execute(() ->
                                sendMessage("Progress: " + progress + "% (" + done + "/" + total + ")")
                        );
                    }
                }

            }, executor));
        }

        // Завершение
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    executor.shutdown();
                    isRunning = false;

                    client.execute(() -> {
                        List<BlockPos> result = bestRemoval.get();
                        int finalMax = maxCount.get();

                        if (!result.isEmpty() && finalMax > initCount) {
                            Scan.selectedBlocks.clear();
                            Scan.selectedBlocks.addAll(result);

                            String blocksStr = result.stream()
                                    .map(BlockPos::toShortString)
                                    .limit(5)
                                    .collect(Collectors.joining(", ")) +
                                    (result.size() > 5 ? " (...+" + (result.size() - 5) + ")" : "");

                            sendMessage("✓ Best result: " + finalMax +
                                    " (removed " + result.size() + " block(s): " + blocksStr + ")");
                        } else {
                            sendMessage("✓ No improvement found (max: " + finalMax + ")");
                        }
                    });
                });
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private static List<BlockPos> collectBuddingAmethysts(World world, BlockBox box) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
            for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).isOf(Blocks.BUDDING_AMETHYST)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    private static class WorldSnapshot {
        final BlockBox range;
        final Set<BlockPos> buddingSet; // предварительно вычисленное множество

        WorldSnapshot(BlockBox range, World world, List<BlockPos> budding) {
            this.range = range;
            this.buddingSet = new HashSet<>(budding);
        }
    }

    private static int countAmethysts(WorldSnapshot snapshot, Set<BlockPos> budding) {
        Set<BlockPos> crystalCandidates = new HashSet<>();
        for (BlockPos bud : budding) {
            for (Direction dir : Direction.values()) {
                BlockPos crystalPos = bud.offset(dir);
                if (snapshot.range.contains(crystalPos) && !budding.contains(crystalPos)) {
                    crystalCandidates.add(crystalPos);
                }
            }
        }

        int count = 0;
        for (BlockPos crystal : crystalCandidates) {
            boolean visibleX = true, visibleY = true, visibleZ = true;

            for (int x = snapshot.range.getMinX(); x <= snapshot.range.getMaxX(); x++) {
                if (budding.contains(new BlockPos(x, crystal.getY(), crystal.getZ()))) {
                    visibleX = false;
                    break;
                }
            }

            for (int y = snapshot.range.getMinY(); y <= snapshot.range.getMaxY(); y++) {
                if (budding.contains(new BlockPos(crystal.getX(), y, crystal.getZ()))) {
                    visibleY = false;
                    break;
                }
            }

            for (int z = snapshot.range.getMinZ(); z <= snapshot.range.getMaxZ(); z++) {
                if (budding.contains(new BlockPos(crystal.getX(), crystal.getY(), z))) {
                    visibleZ = false;
                    break;
                }
            }

            if (visibleX || visibleY || visibleZ) {
                count++;
            }
        }

        return count;
    }

    private static void generateCombinations(
            List<BlockPos> items,
            int k,
            Consumer<List<BlockPos>> consumer
    ) {
        if (k == 0) {
            consumer.accept(Collections.emptyList());
            return;
        }
        int[] indices = new int[k];
        for (int i = 0; i < k; i++) indices[i] = i;

        while (true) {
            List<BlockPos> combination = new ArrayList<>(k);
            for (int i = 0; i < k; i++) {
                combination.add(items.get(indices[i]));
            }
            consumer.accept(combination);

            int i = k - 1;
            while (i >= 0 && indices[i] == items.size() - k + i) i--;
            if (i < 0) break;

            indices[i]++;
            for (int j = i + 1; j < k; j++) {
                indices[j] = indices[j - 1] + 1;
            }
        }
    }

    private static void sendMessage(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            client.player.sendMessage(Text.literal("[Amethyst] " + text), false);
        }
    }

    public static void cancel() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            isRunning = false;
            sendMessage("Analysis cancelled");
        }
    }
}