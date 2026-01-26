package ru.obabok.areascanner.common.network.payloads.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

import java.util.UUID;

public record ClientVersionPayload(UUID uuid) implements CustomPayload {
    public static final CustomPayload.Id<ClientVersionPayload> ID = new CustomPayload.Id<>(Identifier.of(References.MOD_ID, "client_hello"));
    public static final PacketCodec<RegistryByteBuf, ClientVersionPayload> CODEC = PacketCodec.of(ClientVersionPayload::write, ClientVersionPayload::new);

    public ClientVersionPayload(RegistryByteBuf buf){
        this(buf.readUuid());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(uuid);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
