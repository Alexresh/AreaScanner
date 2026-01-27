package ru.obabok.areascanner.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import ru.obabok.areascanner.client.Scan;
import ru.obabok.areascanner.client.gui.SharedScansScreen;
import ru.obabok.areascanner.client.util.WhitelistManager;
import ru.obabok.areascanner.common.NetworkPackets;
import ru.obabok.areascanner.common.References;
import ru.obabok.areascanner.common.model.JobInfo;
import ru.obabok.areascanner.common.model.Whitelist;
import ru.obabok.areascanner.common.network.payloads.c2s.*;
import ru.obabok.areascanner.common.network.payloads.s2c.*;
import ru.obabok.areascanner.common.serializers.WhitelistCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static fi.dy.masa.malilib.gui.GuiBase.openGui;

public class ClientNetwork {
    private static long activeJobId = 0;
    private static List<JobInfo> jobList = List.of();
    private static final Map<Long, PendingStart> pendingStarts = new HashMap<>();
    private static boolean requested;
    private static SharedScansScreen sharedScansScreen;

    public static void register(){

        if (!requested) {
            ClientNetwork.requestSharedList();
            requested = true;
        }
        NetworkPackets.registerPayloads();

        ClientPlayNetworking.registerGlobalReceiver(ServerVersionPayload.ID, (payload, context) -> {
            if(MinecraftClient.getInstance().player != null){
                ClientPlayNetworking.send(new ClientVersionPayload(MinecraftClient.getInstance().player.getUuid()));
                if(FabricLoader.getInstance().getModContainer(References.MOD_ID).isPresent()){
                    if(!FabricLoader.getInstance().getModContainer(References.MOD_ID).get().getMetadata().getVersion().getFriendlyString().equals(payload.version())){
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("[" + References.MOD_ID + "] server version is: " + payload.version() + " but you in " + FabricLoader.getInstance().getModContainer(References.MOD_ID).get().getMetadata().getVersion().getFriendlyString() + ". Operation is not guaranteed"), false);
                    }
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanListResponsePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                jobList = List.copyOf(payload.scans());
                if(sharedScansScreen != null){
                    sharedScansScreen.drawJobs();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanAcceptedPayload.ID, (payload, context) -> {
            PendingStart pending = pendingStarts.remove(payload.jobId());
            if (pending == null) {
                return;
            }
            activeJobId = payload.jobId();
            Scan.startRemoteScan(pending.range, pending.whitelistName, payload.totalChunks());
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanRejectedPayload.ID, (payload, context) -> {
            pendingStarts.remove(payload.jobId());
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Scan rejected: " + payload.reason()), false);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanChunkSummaryPayload.ID, (payload, context) -> {
            if (payload.jobId() != activeJobId) return;
            Scan.markRemoteChunkProcessed(payload.chunkPos());
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanFullCompletedPayload.ID, (payload, context) -> {
            if (payload.jobId() != activeJobId) return;
            activeJobId = 0;
            if(payload.restart()){
                BlockBox range = Scan.getRange();
                Scan.stopScan();
                Scan.setRange(range);
            }else {
                Scan.stopScan();
            }
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Scan finished, cause: " + payload.cause()),false);
                MinecraftClient.getInstance().player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanChunkDataPayload.ID, (payload, context) -> {
            if (payload.jobId() != activeJobId) return;
            Scan.applyRemoteChunkData(payload.positions());
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanDeltaPayload.ID, (payload, context) -> {
            if (payload.jobId() != activeJobId) return;
            Scan.applyRemoteDelta(payload.positions(), payload.add());
        });

        ClientPlayNetworking.registerGlobalReceiver(ScanCompletePayload.ID, (payload, context) -> {
            if (payload.jobId() != activeJobId) return;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Server scan finished"), false);
            }
        });
    }

    public static void openSharedScansScreen(Screen parent){
        sharedScansScreen = new SharedScansScreen(0, parent);
        openGui(sharedScansScreen);
    }

    public static boolean requestScan(BlockBox range, String whitelistName, String shareName) {
        if (!canUseServerScan()) {
            return false;
        }
        long jobId = ThreadLocalRandom.current().nextLong();
        Whitelist whitelist = WhitelistManager.loadData(whitelistName);
        String json = WhitelistCodec.toJson(whitelist);
        pendingStarts.put(jobId, new PendingStart(range, whitelistName));
        ClientPlayNetworking.send(new ScanStartPayload(jobId, range, whitelistName, json, shareName));
        return true;
    }


    public static void stopScan() {
        if (activeJobId == 0) return;
        ClientPlayNetworking.send(new ScanStopPayload(activeJobId, "stopped"));
        activeJobId = 0;
    }

    public static long getActiveJobId(){
        return activeJobId;
    }

    public static void requestSharedList() {
        if (!ClientPlayNetworking.canSend(ScanListRequestPayload.ID)) {
            clearList();
            return;
        }
        ClientPlayNetworking.send(new ScanListRequestPayload());
    }

    public static void subscribeToScan(JobInfo info) {
        if (!ClientPlayNetworking.canSend(ScanSubscribePayload.ID)) {
            return;
        }
        BlockBox range = info.range();
        String whitelistName = info.whitelistName();
        pendingStarts.put(info.id(), new PendingStart(range, whitelistName));
        ClientPlayNetworking.send(new ScanSubscribePayload(info.id()));
    }

    public static void deleteScan(long jobId) {
        ClientPlayNetworking.send(new ScanStopPayload(jobId, "delete"));
    }

    public static void clearList() {
        jobList = List.of();
    }
    public static List<JobInfo> getJobList() {
        return new ArrayList<>(jobList);
    }

    public static boolean canUseServerScan() {
        return ClientPlayNetworking.canSend(ScanStartPayload.ID);
    }

    public static void unsubscribeFromScan(long jobId) {
        if(ClientPlayNetworking.canSend(ScanUnsubscribePayload.ID)){
            ClientPlayNetworking.send(new ScanUnsubscribePayload(jobId));
        }
    }

    private record PendingStart(BlockBox range, String whitelistName) {
    }
}
