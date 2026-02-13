package ru.obabok.areascanner.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.obabok.areascanner.client.Scan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AmethystAnalyzer {
    public static void start() {
        BlockBox range = Scan.getRange();
        if (range == null) return;

        List<BlockPos> budding = getBuddingAmethysts(range);
        if (budding.isEmpty()) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal("No budding amethysts found"), false);
            return;
        }

        int initCount = countAmethystsByBuddingBlocks(range, budding);
        MinecraftClient.getInstance().player.sendMessage(Text.literal("Initial count: " + initCount), false);

        // Изменяемые контейнеры для захвата в рекурсию
        final int[] maxCount = {initCount};
        final List<BlockPos>[] bestRemoval = new List[]{new ArrayList<>()};

        // Ограничение для безопасности (комбинаторный взрыв)
        final int MAX_REMOVALS = Math.min(2, budding.size());

        for (int k = 1; k <= MAX_REMOVALS; k++) {
            generateCombinations(budding, k, combination -> {
                List<BlockPos> remaining = new ArrayList<>(budding);
                remaining.removeAll(combination);

                int count = countAmethystsByBuddingBlocks(range, remaining);
                if (count > maxCount[0]) {
                    maxCount[0] = count;
                    bestRemoval[0] = new ArrayList<>(combination);

                    String blocksStr = combination.stream()
                            .map(BlockPos::toShortString)
                            .collect(Collectors.joining(", "));

                    MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("New max: " + count + blocksStr),
                            false
                    );

                    Scan.selectedBlocks.clear();
                    Scan.selectedBlocks.addAll(bestRemoval[0]);
                }
            });
        }

        if (!bestRemoval[0].isEmpty()) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Best result: " + maxCount[0] + " (removed " + bestRemoval[0].size() + " block(s))"),
                    false
            );
        }
    }

    private static void generateCombinations(List<BlockPos> items, int k, Consumer<List<BlockPos>> consumer) {
        combineHelper(items, k, 0, new ArrayList<>(), consumer);
    }

    private static void combineHelper(List<BlockPos> items, int k, int start,
                                      List<BlockPos> current, Consumer<List<BlockPos>> consumer) {
        if (current.size() == k) {
            consumer.accept(new ArrayList<>(current));
            return;
        }
        // Оптимизация: не заходим в ветки, где недостаточно элементов для завершения комбинации
        for (int i = start; i <= items.size() - k + current.size(); i++) {
            current.add(items.get(i));
            combineHelper(items, k, i + 1, current, consumer);
            current.remove(current.size() - 1);
        }
    }

    private static ArrayList<BlockPos> getBuddingAmethysts(BlockBox box){
        ClientWorld world = MinecraftClient.getInstance().world;
        if(box == null || world == null) return new ArrayList<>();
        ArrayList<BlockPos> buddingAmethysts = new ArrayList<>();

        for (int x = 0; x < box.getBlockCountX(); x++) {
            for (int y = 0; y < box.getBlockCountY(); y++) {
                for (int z = 0; z < box.getBlockCountZ(); z++) {
                    BlockPos pos = new BlockPos(box.getMinX() + x, box.getMinY() + y, box.getMinZ() + z);
                    if(world.getBlockState(pos).getBlock() == Blocks.BUDDING_AMETHYST){
                        buddingAmethysts.add(pos);
                    }
                }
            }
        }
        return buddingAmethysts;

    }

    private static int countAmethystsByBuddingBlocks(BlockBox range, List<BlockPos> buddingAmethysts){
        ArrayList<BlockPos> amethysts = new ArrayList<>();
        ClientWorld world = MinecraftClient.getInstance().world;
        if(range == null || world == null) return 0;

        for (int i = 0; i < buddingAmethysts.size(); i++) {
            for (int dir = 0; dir < Direction.values().length; dir++) {
                BlockPos dirPos = new BlockPos(buddingAmethysts.get(i).offset(Direction.values()[dir],1));
                if(!buddingAmethysts.contains(dirPos)){
                    amethysts.add(dirPos);
                }
            }
        }

        ArrayList<BlockPos> uniqueList = new ArrayList<>();
        for (BlockPos pos : amethysts) {
            if (!uniqueList.contains(pos)) {
                uniqueList.add(pos);
            }
        }
        amethysts = uniqueList;

        int count = 0;
        for (int am = 0; am < amethysts.size(); am++) {
            boolean addX = true;
            boolean addY = true;
            boolean addZ = true;
            for (int x = range.getMinX(); x < range.getMaxX(); x++) {
                BlockPos pos = new BlockPos(x, amethysts.get(am).getY(), amethysts.get(am).getZ());
                if(buddingAmethysts.contains(pos)){
                    addX = false;
                }
            }
            for (int y = range.getMinY(); y < range.getMaxY(); y++) {
                BlockPos pos = new BlockPos(amethysts.get(am).getX(), y, amethysts.get(am).getZ());
                if(buddingAmethysts.contains(pos)){
                    addY = false;
                }
            }
            for (int z = range.getMinZ(); z < range.getMaxZ(); z++) {
                BlockPos pos = new BlockPos(amethysts.get(am).getX(), amethysts.get(am).getY(), z);
                if(buddingAmethysts.contains(pos)){
                    addZ = false;
                }
            }
            if(addX || addY || addZ){
                count++;
            };
        }
        return count;

    }
}
