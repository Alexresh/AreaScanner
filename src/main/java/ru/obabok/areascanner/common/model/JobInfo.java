package ru.obabok.areascanner.common.model;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockBox;

import java.util.UUID;

public record JobInfo(
        long id,
        String name,
        String owner,
        UUID ownerUUID,
        String dimension,
        BlockBox range,
        String whitelistName,
        long totalChunks,
        long processedChunks,
        int selectedBlocks,
        boolean completedScan
) {
    public static JobInfo read(RegistryByteBuf buf) {
        long id = buf.readLong();
        String name = buf.readString();
        String owner = buf.readString();
        UUID ownerUUID = buf.readUuid();
        String dimension = buf.readString();
        BlockBox range = readBlockBox(buf);
        String whitelistName = buf.readString();
        long totalChunks = buf.readLong();
        long processedChunks = buf.readLong();
        int selectedBlocks = buf.readInt();
        boolean completeScan = buf.readBoolean();
        return new JobInfo(id, name, owner, ownerUUID, dimension, range, whitelistName, totalChunks, processedChunks, selectedBlocks, completeScan);
    }

    public void write(RegistryByteBuf buf) {
        buf.writeLong(id);
        buf.writeString(name == null ? "" : name);
        buf.writeString(owner == null ? "" : owner);
        buf.writeUuid(ownerUUID);
        buf.writeString(dimension == null ? "" : dimension);
        writeBlockBox(buf, range);
        buf.writeString(whitelistName == null ? "" : whitelistName);
        buf.writeLong(totalChunks);
        buf.writeLong(processedChunks);
        buf.writeInt(selectedBlocks);
        buf.writeBoolean(completedScan);
    }

    private static void writeBlockBox(RegistryByteBuf buf, BlockBox box) {
        buf.writeInt(box.getMinX());
        buf.writeInt(box.getMinY());
        buf.writeInt(box.getMinZ());
        buf.writeInt(box.getMaxX());
        buf.writeInt(box.getMaxY());
        buf.writeInt(box.getMaxZ());
    }

    private static BlockBox readBlockBox(RegistryByteBuf buf) {
        int minX = buf.readInt();
        int minY = buf.readInt();
        int minZ = buf.readInt();
        int maxX = buf.readInt();
        int maxY = buf.readInt();
        int maxZ = buf.readInt();
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
