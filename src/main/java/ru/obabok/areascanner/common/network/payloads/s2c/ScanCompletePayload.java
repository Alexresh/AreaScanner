package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanCompletePayload(long jobId) implements CustomPayload {
    public static final Id<ScanCompletePayload> ID = new Id<>(Identifier.of(References.MOD_ID, "scan_complete"));
    public static final PacketCodec<RegistryByteBuf, ScanCompletePayload> CODEC = PacketCodec.of(ScanCompletePayload::write, ScanCompletePayload::new);

    public ScanCompletePayload(RegistryByteBuf buf) {
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
