package ru.obabok.arenascanner.client;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
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

        ClientPlayerBlockBreakEvents.AFTER.register((clientWorld, clientPlayerEntity, blockPos, blockState) -> {
            ChunkScheduler.addChunkToProcess(new ChunkPos(blockPos));
        });

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if(!world.isClient) return ActionResult.PASS;
            ChunkScheduler.addChunkToProcess(new ChunkPos(blockPos));
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if(!world.isClient) return ActionResult.PASS;
            ChunkScheduler.addChunkToProcess(new ChunkPos(blockHitResult.getBlockPos()));
            return ActionResult.PASS;
        });

        ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> {
            if(Scan.getRange() != null && Scan.unloadedChunks.contains(worldChunk.getPos()))
                ChunkScheduler.addChunkToProcess(worldChunk.getPos());
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> HudRender.render(drawContext));
        WorldRenderEvents.AFTER_ENTITIES.register(RenderUtil::renderAll);
        ChunkScheduler.startProcessing();


    }

}
