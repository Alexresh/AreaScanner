package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.areascanner.common.References;

public record ScanChunkSummaryPayload(long jobId, ChunkPos chunkPos) implements CustomPayload {
    public static final Id<ScanChunkSummaryPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_chunk_summary"));
    public static final PacketCodec<RegistryByteBuf, ScanChunkSummaryPayload> CODEC =
            PacketCodec.of(ScanChunkSummaryPayload::write, ScanChunkSummaryPayload::new);

    public ScanChunkSummaryPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readChunkPos());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeChunkPos(chunkPos);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
