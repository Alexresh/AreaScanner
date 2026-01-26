package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record ServerVersionPayload(String version) implements CustomPayload {
    public static final CustomPayload.Id<ServerVersionPayload> ID = new CustomPayload.Id<>(Identifier.of(References.MOD_ID, "server_hello"));
    public static final PacketCodec<RegistryByteBuf, ServerVersionPayload> CODEC = PacketCodec.of(ServerVersionPayload::write, ServerVersionPayload::new);

    public ServerVersionPayload(RegistryByteBuf buf){
        this(buf.readString());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(version);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
