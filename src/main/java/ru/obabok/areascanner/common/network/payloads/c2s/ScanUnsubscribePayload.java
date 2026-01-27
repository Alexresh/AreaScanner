package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanUnsubscribePayload(long jobId) implements CustomPayload {
    public static final Id<ScanUnsubscribePayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_unsubscribe"));
    public static final PacketCodec<RegistryByteBuf, ScanUnsubscribePayload> CODEC =
            PacketCodec.of(ScanUnsubscribePayload::write, ScanUnsubscribePayload::new);

    public ScanUnsubscribePayload(RegistryByteBuf buf) {
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
