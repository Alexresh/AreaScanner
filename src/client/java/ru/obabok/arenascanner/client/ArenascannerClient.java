package ru.obabok.arenascanner.client;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import ru.obabok.arenascanner.client.handlers.InitHandler;
import ru.obabok.arenascanner.client.util.HudRender;
import ru.obabok.arenascanner.client.util.RenderUtil;
import ru.obabok.arenascanner.client.util.ChunkScheduler;
import ru.obabok.arenascanner.client.util.ScanCommand;


public class ArenascannerClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        ClientCommandRegistrationCallback.EVENT.register(ScanCommand::register);


        ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> {
            if(Scan.getRange() != null && Scan.unloadedChunks.contains(worldChunk.getPos()))
                ChunkScheduler.addChunkToProcess(worldChunk.getPos());
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> HudRender.render(drawContext));
        WorldRenderEvents.AFTER_ENTITIES.register(RenderUtil::renderAll);
        ChunkScheduler.startProcessing();

    }

}
