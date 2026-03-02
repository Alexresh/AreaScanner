package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanChunkSummaryPayload(long jobId, long chunkCount) implements CustomPayload {
    public static final Id<ScanChunkSummaryPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_chunk_summary"));
    public static final PacketCodec<RegistryByteBuf, ScanChunkSummaryPayload> CODEC =
            PacketCodec.of(ScanChunkSummaryPayload::write, ScanChunkSummaryPayload::new);

    public ScanChunkSummaryPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readLong());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeLong(chunkCount);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
