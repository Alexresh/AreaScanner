package ru.obabok.areascanner.server.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ScanPacketUtils {

    public static void writeBlockBox(RegistryByteBuf buf, BlockBox box) {
        buf.writeInt(box.getMinX());
        buf.writeInt(box.getMinY());
        buf.writeInt(box.getMinZ());
        buf.writeInt(box.getMaxX());
        buf.writeInt(box.getMaxY());
        buf.writeInt(box.getMaxZ());
    }

    public static BlockBox readBlockBox(RegistryByteBuf buf) {
        int minX = buf.readInt();
        int minY = buf.readInt();
        int minZ = buf.readInt();
        int maxX = buf.readInt();
        int maxY = buf.readInt();
        int maxZ = buf.readInt();
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void writeBlockPosList(RegistryByteBuf buf, List<BlockPos> list) {
        buf.writeVarInt(list.size());
        for (BlockPos pos : list) {
            buf.writeBlockPos(pos);
        }
    }

    public static List<BlockPos> readBlockPosList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readBlockPos());
        }
        return list;
    }
}
