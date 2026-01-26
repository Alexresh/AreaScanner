package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanDeletePayload(long jobId) implements CustomPayload {
    public static final Id<ScanDeletePayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "shared_scan_delete"));
    public static final PacketCodec<RegistryByteBuf, ScanDeletePayload> CODEC =
            PacketCodec.of(ScanDeletePayload::write, ScanDeletePayload::new);

    public ScanDeletePayload(RegistryByteBuf buf) {
        this(buf.readLong());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
