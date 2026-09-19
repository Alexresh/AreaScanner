package ru.obabok.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import ru.obabok.client.models.BlockGroup;

import java.util.*;

public class ConnectedBlockFinder {
    public static List<BlockGroup> findGroups(List<BlockPos> blocks) {
        Set<BlockPos> unvisited = new HashSet<>(blocks);
        List<BlockGroup> groups = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            BlockPos start = unvisited.iterator().next();
            Set<BlockPos> group = floodFill(start, unvisited);
            groups.add(new BlockGroup(group));
        }

        return groups;
    }

    private static Set<BlockPos> floodFill(BlockPos start, Set<BlockPos> unvisited) {
        Set<BlockPos> group = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(start);
        unvisited.remove(start);
        group.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                if (unvisited.remove(neighbor)) {
                    group.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return group;
    }
}
