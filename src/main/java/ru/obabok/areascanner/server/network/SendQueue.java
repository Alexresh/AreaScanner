package ru.obabok.areascanner.server.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.obabok.areascanner.server.ServerScanConfig;

import java.util.ArrayDeque;

public class SendQueue {
    private static final ArrayDeque<Packet> queue = new ArrayDeque<>();

    public static void addPacket(ServerPlayerEntity player, CustomPayload payload){
        queue.addLast(new Packet(player, payload));
    }

    public static void tick(){
        int sended = 0;
        while (!queue.isEmpty() && sended <= ServerScanConfig.getMaxPacketsPerTick()){
            Packet packet = queue.pollFirst();
            if(packet != null){
                ServerPlayNetworking.send(packet.player, packet.payload);
            }
            sended++;
        }
    }

    private record Packet(ServerPlayerEntity player, CustomPayload payload){}
}
