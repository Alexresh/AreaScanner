package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanDeltaPayload(long jobId, boolean add, long[] positions) implements CustomPayload {
    public static final Id<ScanDeltaPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_delta"));
    public static final PacketCodec<RegistryByteBuf, ScanDeltaPayload> CODEC =
            PacketCodec.of(ScanDeltaPayload::write, ScanDeltaPayload::new);

    public ScanDeltaPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readBoolean(), buf.readLongArray());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeBoolean(add);
        buf.writeLongArray(positions);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
