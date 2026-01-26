package ru.obabok.areascanner.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import ru.obabok.areascanner.server.network.SendQueue;
import ru.obabok.areascanner.server.network.ServerNetwork;

public class AreaScannerServer implements ModInitializer {


    @Override
    public void onInitialize() {
        ServerNetwork.register();
        ServerScanConfig.load();

        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> ScanConfigCommand.register(commandDispatcher));

//        ServerPlayNetworking.registerGlobalReceiver(ScanDeletePayload.ID, (payload, context) -> {
//            ServerPlayerEntity player = context.player();
//            ServerScanManager.getInstance().stopJob(player, payload.scanId());
//            ServerPlayNetworking.send(player, new ScanListResponsePayload(ServerScanManager.getInstance().getJobs()));
//        }); unused maybe

        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> {
            ServerScanManager.getInstance().tick();
            SendQueue.tick();
        });
    }



}
