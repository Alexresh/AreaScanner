package ru.obabok.areascanner.server.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockBox;
import ru.obabok.areascanner.common.NetworkPackets;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.common.model.JobInfo;
import ru.obabok.areascanner.common.model.Whitelist;
import ru.obabok.areascanner.common.network.payloads.c2s.*;
import ru.obabok.areascanner.common.network.payloads.s2c.*;
import ru.obabok.areascanner.common.serializers.WhitelistCodec;
import ru.obabok.areascanner.server.ServerScanManager;

public class ServerNetwork {

    public static void register(){
        NetworkPackets.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            if(FabricLoader.getInstance().getModContainer(References.MOD_ID).isPresent()){
                packetSender.sendPacket(new ServerVersionPayload(FabricLoader.getInstance().getModContainer(References.MOD_ID).get().getMetadata().getVersion().getFriendlyString()));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, minecraftServer) -> {
            Clients.playersWithMod.remove(serverPlayNetworkHandler.player.getUuid());
        });

        ServerPlayNetworking.registerGlobalReceiver(ClientVersionPayload.ID, (clientVersionPayload, context) -> {
            Clients.playersWithMod.add(clientVersionPayload.uuid());
            //References.LOGGER.info("[server] player " + context.server().getPlayerManager().getPlayer(clientVersionPayload.uuid()).getName().getString() + " has areascanner mod");
        });

        ServerPlayNetworking.registerGlobalReceiver(ScanStopPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if(player.getPermissionLevel() >= 2){
                ServerScanManager.getInstance().stopOPJob(payload.jobId());
            }else {
                ServerScanManager.getInstance().stopJob(player, payload.jobId(), payload.cause());
            }

            //new
            ServerPlayNetworking.send(player, new ScanListResponsePayload(ServerScanManager.getInstance().getJobs()));
            //SharedScanManager.getInstance().stopSubscription(context.player(), payload.jobId());
        });

        ServerPlayNetworking.registerGlobalReceiver(ScanStartPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            handleStart(player, payload);
        });



        ServerPlayNetworking.registerGlobalReceiver(ScanListRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerPlayNetworking.send(player, new ScanListResponsePayload(ServerScanManager.getInstance().getJobs()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ScanSubscribePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String reason = ServerScanManager.getInstance().subscribe(player, payload.jobId());
            if (reason != null) {
                ServerPlayNetworking.send(player, new ScanRejectedPayload(payload.jobId(), reason));
                return;
            }
            JobInfo info = ServerScanManager.getInstance().getJobInfo(payload.jobId());
            if (info == null) {
                ServerPlayNetworking.send(player, new ScanRejectedPayload(payload.jobId(), "Shared scan not found"));
                return;
            }
            long totalChunks = info.totalChunks();
            ServerPlayNetworking.send(player, new ScanAcceptedPayload(payload.jobId(), totalChunks));
        });

        ServerPlayNetworking.registerGlobalReceiver(ScanUnsubscribePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerScanManager.getInstance().unsubscribe(player, payload.jobId());
            ServerPlayNetworking.send(player, new ScanFullCompletedPayload(payload.jobId(), "Unsubscribed", false));
        });
    }

    private static void handleStart(ServerPlayerEntity player, ScanStartPayload payload) {
        Whitelist whitelist = WhitelistCodec.fromJson(payload.whitelistJson());
        if (whitelist == null) {
            ServerPlayNetworking.send(player, new ScanRejectedPayload(payload.jobId(), "Whitelist not found"));
            return;
        }
        BlockBox range = payload.range();
        long totalChunks = getTotalChunks(range);
        if (totalChunks <= 0) {
            ServerPlayNetworking.send(player, new ScanRejectedPayload(payload.jobId(), "Invalid range"));
            return;
        }

        if (payload.shareName() != null && !payload.shareName().isEmpty() && payload.whitelistName() != null) {
            ServerScanManager.getInstance().startJob(player, payload.jobId(), range, payload.whitelistName(), whitelist, payload.shareName());
        }else{
            ServerPlayNetworking.send(player, new ScanRejectedPayload(payload.jobId(), "Share name is empty"));
        }
        ServerPlayNetworking.send(player, new ScanAcceptedPayload(payload.jobId(), totalChunks));
    }

    private static long getTotalChunks(BlockBox range) {
        int startChunkX = range.getMinX() >> 4;
        int startChunkZ = range.getMinZ() >> 4;
        int endChunkX = range.getMaxX() >> 4;
        int endChunkZ = range.getMaxZ() >> 4;
        long xCount = (long) endChunkX - startChunkX + 1;
        long zCount = (long) endChunkZ - startChunkZ + 1;
        return xCount * zCount;
    }
}
