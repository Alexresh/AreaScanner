package ru.obabok.client.models;

import net.minecraft.core.BlockPos;

import java.util.Set;

public class BlockGroup {
    public final double centerX, centerY, centerZ;
    public final Set<BlockPos> blocks;
    public BlockGroup(Set<BlockPos> blocks) {
        this.blocks = blocks;
        double sumX = 0, sumY = 0, sumZ = 0;
        for (BlockPos pos : blocks) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }
        this.centerX = sumX / blocks.size() + 0.5;
        this.centerY = sumY / blocks.size() + 0.5;
        this.centerZ = sumZ / blocks.size() + 0.5;
    }
}
