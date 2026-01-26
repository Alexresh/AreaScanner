package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanJoinPayload(long jobId) implements CustomPayload {
    public static final Id<ScanJoinPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "shared_scan_join"));
    public static final PacketCodec<RegistryByteBuf, ScanJoinPayload> CODEC =
            PacketCodec.of(ScanJoinPayload::write, ScanJoinPayload::new);

    public ScanJoinPayload(RegistryByteBuf buf) {
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
