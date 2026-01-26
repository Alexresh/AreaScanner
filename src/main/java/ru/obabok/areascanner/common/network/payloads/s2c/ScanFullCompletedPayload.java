package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanFullCompletedPayload(long jobId, String cause, boolean restart) implements CustomPayload{
    public static final CustomPayload.Id<ScanFullCompletedPayload> ID = new CustomPayload.Id<>(Identifier.of(References.MOD_ID, "scan_full_complete"));
    public static final PacketCodec<RegistryByteBuf, ScanFullCompletedPayload> CODEC = PacketCodec.of(ScanFullCompletedPayload::write, ScanFullCompletedPayload::new);

    public ScanFullCompletedPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readString(), buf.readBoolean());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeString(cause);
        buf.writeBoolean(restart);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
