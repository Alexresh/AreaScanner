package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanSubscribePayload(long jobId) implements CustomPayload {
    public static final Id<ScanSubscribePayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_subscribe"));
    public static final PacketCodec<RegistryByteBuf, ScanSubscribePayload> CODEC =
            PacketCodec.of(ScanSubscribePayload::write, ScanSubscribePayload::new);

    public ScanSubscribePayload(RegistryByteBuf buf) {
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
