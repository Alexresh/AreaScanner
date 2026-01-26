package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanStopPayload(long jobId, String cause) implements CustomPayload {
    public static final Id<ScanStopPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_stop"));
    public static final PacketCodec<RegistryByteBuf, ScanStopPayload> CODEC =
            PacketCodec.of(ScanStopPayload::write, ScanStopPayload::new);

    public ScanStopPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readString());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeString(cause);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
