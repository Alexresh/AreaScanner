package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.server.network.ScanPacketUtils;

import java.util.List;

public record ScanChunkDataPayload(long jobId, List<BlockPos> positions) implements CustomPayload {
    public static final Id<ScanChunkDataPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_chunk_data"));
    public static final PacketCodec<RegistryByteBuf, ScanChunkDataPayload> CODEC =
            PacketCodec.of(ScanChunkDataPayload::write, ScanChunkDataPayload::new);

    public ScanChunkDataPayload(RegistryByteBuf buf) {
        this(buf.readLong(), ScanPacketUtils.readBlockPosList(buf));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        ScanPacketUtils.writeBlockPosList(buf, positions);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
