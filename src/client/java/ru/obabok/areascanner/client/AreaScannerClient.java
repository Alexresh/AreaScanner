package ru.obabok.areascanner.client;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import ru.obabok.areascanner.client.handlers.InitHandler;
import ru.obabok.areascanner.client.util.*;

public class AreaScannerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        ClientCommandRegistrationCallback.EVENT.register(ScanCommand::register);
        ClientPlayerBlockBreakEvents.AFTER.register((clientWorld, clientPlayerEntity, blockPos, blockState) -> ChunkScheduler.addChunkToProcess(new ChunkPos(blockPos)));

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
            BlockBox range = Scan.getRange();
            if (range != null) {
                ChunkPos chunkPos = worldChunk.getPos();
                int chunkMinX = chunkPos.getStartX();
                int chunkMaxX = chunkPos.getEndX();
                int chunkMinZ = chunkPos.getStartZ();
                int chunkMaxZ = chunkPos.getEndZ();
                if (range.intersectsXZ(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)) {
                    ChunkScheduler.addChunkToProcess(chunkPos);
                }
            }
        });

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.MISC_OVERLAYS, Identifier.of(References.MOD_ID, "hud"), HudRender::render));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RenderUtil::renderAll);
        ChunkScheduler.startProcessing();
    }

}
