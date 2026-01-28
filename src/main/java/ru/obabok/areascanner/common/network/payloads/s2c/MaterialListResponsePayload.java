package ru.obabok.areascanner.common.network.payloads.s2c;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import ru.obabok.areascanner.common.References;

import java.util.Map;

public record MaterialListResponsePayload(long jobId, Map<Block, Integer> materials) implements CustomPayload {
    public static final Id<MaterialListResponsePayload> ID =
            new Id<>(Identifier.of(References.MOD_ID, "material_list_response"));

    public static final PacketCodec<RegistryByteBuf, MaterialListResponsePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, MaterialListResponsePayload::jobId,
            PacketCodecs.map(
                    Object2IntOpenHashMap::new,
                    PacketCodecs.registryValue(RegistryKeys.BLOCK),
                    PacketCodecs.VAR_INT
            ),
            MaterialListResponsePayload::materials,
            MaterialListResponsePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}