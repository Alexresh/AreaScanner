package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ScanListRequestPayload() implements CustomPayload {
    public static final Id<ScanListRequestPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "shared_scan_list_request"));
    public static final PacketCodec<RegistryByteBuf, ScanListRequestPayload> CODEC =
            PacketCodec.of(ScanListRequestPayload::write, ScanListRequestPayload::new);

    public ScanListRequestPayload(RegistryByteBuf buf) {
        this();
    }

    private void write(RegistryByteBuf buf) {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
