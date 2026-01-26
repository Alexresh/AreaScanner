package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.common.model.JobInfo;

import java.util.ArrayList;
import java.util.List;

public record ScanListResponsePayload(List<JobInfo> scans) implements CustomPayload {
    public static final Id<ScanListResponsePayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "shared_scan_list"));
    public static final PacketCodec<RegistryByteBuf, ScanListResponsePayload> CODEC =
            PacketCodec.of(ScanListResponsePayload::write, ScanListResponsePayload::new);

    public ScanListResponsePayload(RegistryByteBuf buf) {
        this(readList(buf));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(scans.size());
        for (JobInfo info : scans) {
            info.write(buf);
        }
    }

    private static List<JobInfo> readList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<JobInfo> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(JobInfo.read(buf));
        }
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
