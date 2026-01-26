package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanAcceptedPayload(long jobId, long totalChunks) implements CustomPayload {
    public static final Id<ScanAcceptedPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_accepted"));
    public static final PacketCodec<RegistryByteBuf, ScanAcceptedPayload> CODEC =
            PacketCodec.of(ScanAcceptedPayload::write, ScanAcceptedPayload::new);

    public ScanAcceptedPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readLong());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeLong(totalChunks);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
