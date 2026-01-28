package ru.obabok.areascanner.common.network.payloads.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

public record DebugInfoPayload(int networkQueueSize) implements CustomPayload {
    public static final Id<DebugInfoPayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "debug_info"));
    public static final PacketCodec<RegistryByteBuf, DebugInfoPayload> CODEC =
            PacketCodec.of(DebugInfoPayload::write, DebugInfoPayload::new);

    public DebugInfoPayload(RegistryByteBuf buf) {
        this(buf.readInt());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeInt(networkQueueSize);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}