package ru.obabok.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import ru.obabok.client.gui.screens.VibeCodeAlertScreen;
import ru.obabok.client.render.HudRender;
import ru.obabok.client.render.RenderUtil;
import ru.obabok.client.util.ObsidianPredictor.AsyncObsidianPredictor;
import ru.obabok.common.model.BlockArea;
import ru.obabok.client.network.ClientNetwork;
import ru.obabok.client.util.*;
import ru.obabok.common.References;

public class AreaScannerClient implements ClientModInitializer {
	public static boolean isMaliLibLoaded;
	@Override
	public void onInitializeClient() {
		isMaliLibLoaded = FabricLoader.getInstance().isModLoaded("malilib");
		if(isMaliLibLoaded){
			AreaScannerMalilibHelper.initMalilib();
			ClientNetwork.register();
			ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> ScanCommand.register(commandDispatcher));

			ClientPlayerBlockBreakEvents.AFTER.register((clientWorld, clientPlayerEntity, blockPos, blockState) ->{
				if (Scan.isRemoteProcessing()) return;
				if(Scan.isProcessing()){
					ChunkScheduler.addChunkToProcess(ChunkPos.containing(blockPos));
				}
			});

			AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
				if(!world.isClientSide()) return InteractionResult.PASS;
				if (Scan.isRemoteProcessing()) return InteractionResult.PASS;
				if(Scan.isProcessing()) {
					ChunkScheduler.addChunkToProcess(ChunkPos.containing(blockPos));
				}
				return InteractionResult.PASS;
			});

			UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
				if(!world.isClientSide()) return InteractionResult.PASS;
				if (Scan.isRemoteProcessing()) return InteractionResult.PASS;
				if(Scan.isProcessing()) {
					ChunkScheduler.addChunkToProcess(ChunkPos.containing(blockHitResult.getBlockPos()));
				}
				return InteractionResult.PASS;
			});

			ClientChunkEvents.CHUNK_LOAD.register((clientWorld, worldChunk) -> {
				AsyncObsidianPredictor.onChunkLoaded(clientWorld, worldChunk.getPos());
				BlockArea area = Scan.getArea();
				if (area == null) return;

				ChunkPos chunkPos = worldChunk.getPos();
				if (!area.intersectsChunk(chunkPos)) return;
				if (Scan.isRemoteProcessing()) return;

				if (Scan.isProcessing()) {
					ChunkScheduler.addChunkToProcess(chunkPos);
				}
			});

			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				if (client.player != null && client.level != null) {
					AsyncObsidianPredictor.tick(client.level);
				}
			});

			HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(References.MOD_ID, "hud"), HudRender::render);
			LevelRenderEvents.END_MAIN.register(RenderUtil::render);
			ChunkScheduler.startProcessing();
			ClientLifecycleEvents.CLIENT_STOPPING.register(ChunkScheduler::stopProcessing);
			Config.Generic.MAIN_RENDER.setValueChangeCallback(_ -> {
				Scan.renderDirty = true;
			});

			Config.Generic.OBSIDIAN_PREDICTOR.setValueChangeCallback(config -> {
				if(config.getBooleanValue()){
					Minecraft.getInstance().gui.setScreen(new VibeCodeAlertScreen());
				}
			});
		}
	}
	//need to be here because this class no load malilib classes
	public static boolean shouldUpdateRealtime() {
		if (AreaScannerClient.isMaliLibLoaded) {
			//config class not load if isMaliLibLoaded = false and no crash occurred
			return Config.Generic.REALTIME_UPDATE.getBooleanValue();
		}
		return false;
	}
}