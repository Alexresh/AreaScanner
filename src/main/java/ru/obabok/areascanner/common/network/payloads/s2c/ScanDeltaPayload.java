package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.server.network.ScanPacketUtils;

import java.util.List;

public record ScanDeltaPayload(long jobId, boolean add, List<BlockPos> positions) implements CustomPayload {
    public static final Id<ScanDeltaPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_delta"));
    public static final PacketCodec<RegistryByteBuf, ScanDeltaPayload> CODEC =
            PacketCodec.of(ScanDeltaPayload::write, ScanDeltaPayload::new);

    public ScanDeltaPayload(RegistryByteBuf buf) {
        this(buf.readLong(), buf.readBoolean(), ScanPacketUtils.readBlockPosList(buf));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        buf.writeBoolean(add);
        ScanPacketUtils.writeBlockPosList(buf, positions);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
