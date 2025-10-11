package ru.obabok.arenascanner.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.obabok.arenascanner.client.Config;
import ru.obabok.arenascanner.client.Scan;
import ru.obabok.arenascanner.client.util.ChunkScheduler;

@Environment(EnvType.CLIENT)
@Mixin(WorldChunk.class)
public class WorldChunkMixin {

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void setBlock(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir){
        if(Config.Generic.REALTIME_UPDATE.getBooleanValue()){
            BlockState oldState = cir.getReturnValue();
            if (oldState != null && oldState != state) {
                World world = ((WorldChunk) (Object) this).getWorld();
                if (world instanceof ClientWorld) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    if(Scan.getProcessing() && !ChunkScheduler.getChunkQueue().contains(chunkPos))
                        ChunkScheduler.addChunkToProcess(chunkPos);
                }
            }
        }
    }

}
