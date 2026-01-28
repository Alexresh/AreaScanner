package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record MaterialListRequestPayload(long jobId) implements CustomPayload {
    public static final Id<MaterialListRequestPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "material_list_request"));
    public static final PacketCodec<RegistryByteBuf, MaterialListRequestPayload> CODEC =
            PacketCodec.of(MaterialListRequestPayload::write, MaterialListRequestPayload::new);

    public MaterialListRequestPayload(RegistryByteBuf buf) {
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
