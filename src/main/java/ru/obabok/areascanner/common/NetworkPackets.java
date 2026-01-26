package ru.obabok.areascanner.common;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.obabok.areascanner.common.network.payloads.c2s.*;
import ru.obabok.areascanner.common.network.payloads.s2c.*;

public class NetworkPackets {
    private static boolean registered = false;
    public static void registerPayloads() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.playS2C().register(ServerVersionPayload.ID, ServerVersionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanChunkSummaryPayload.ID, ScanChunkSummaryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanAcceptedPayload.ID, ScanAcceptedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanRejectedPayload.ID, ScanRejectedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanCompletePayload.ID, ScanCompletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanDeltaPayload.ID, ScanDeltaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanChunkDataPayload.ID, ScanChunkDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanListResponsePayload.ID, ScanListResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanFullCompletedPayload.ID, ScanFullCompletedPayload.CODEC);


        PayloadTypeRegistry.playC2S().register(ClientVersionPayload.ID, ClientVersionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanStartPayload.ID, ScanStartPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanStopPayload.ID, ScanStopPayload.CODEC);
        //PayloadTypeRegistry.playC2S().register(ScanDeletePayload.ID, ScanDeletePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanJoinPayload.ID, ScanJoinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanListRequestPayload.ID, ScanListRequestPayload.CODEC);



        /*
        PayloadTypeRegistry.playC2S().register(ScanStartPayload.ID, ScanStartPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanStopPayload.ID, ScanStopPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanCreditsPayload.ID, ScanCreditsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SharedScanListRequestPayload.ID, SharedScanListRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SharedScanJoinPayload.ID, SharedScanJoinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SharedScanDeletePayload.ID, SharedScanDeletePayload.CODEC);

        PayloadTypeRegistry.playS2C().register(ScanAcceptedPayload.ID, ScanAcceptedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanRejectedPayload.ID, ScanRejectedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanChunkSummaryPayload.ID, ScanChunkSummaryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanChunkDataPayload.ID, ScanChunkDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanDeltaPayload.ID, ScanDeltaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScanCompletePayload.ID, ScanCompletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SharedScanListPayload.ID, SharedScanListPayload.CODEC);
        */
    }

}
