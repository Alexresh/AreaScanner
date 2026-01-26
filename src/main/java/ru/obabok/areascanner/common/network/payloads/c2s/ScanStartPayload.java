package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.server.network.ScanPacketUtils;

public record ScanStartPayload(
        long jobId,
        BlockBox range,
        String whitelistName,
//        boolean hasWhitelistData,
        String whitelistJson,
        String shareName
) implements CustomPayload {
    public static final Id<ScanStartPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "scan_start"));
    public static final PacketCodec<RegistryByteBuf, ScanStartPayload> CODEC =
            PacketCodec.of(ScanStartPayload::write, ScanStartPayload::new);

    public ScanStartPayload(RegistryByteBuf buf) {
        this(
                buf.readLong(),
                ScanPacketUtils.readBlockBox(buf),
                buf.readString(),
                //buf.readBoolean(),
                buf.readString(),
                buf.readString()
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeLong(jobId);
        ScanPacketUtils.writeBlockBox(buf, range);
        buf.writeString(whitelistName);
        //buf.writeBoolean(hasWhitelistData);
        buf.writeString(whitelistJson);
        buf.writeString(shareName == null ? "" : shareName);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
