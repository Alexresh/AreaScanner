package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanRejectedPayload(long jobId, String reason) implements CustomPayload {
    public static final Id<ScanRejectedPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_rejected"));
    public static final PacketCodec<RegistryByteBuf, ScanRejectedPayload> CODEC =
            PacketCodec.of(ScanRejectedPayload::write, ScanRejectedPayload::new);

    public ScanRejectedPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readString());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeString(reason);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
