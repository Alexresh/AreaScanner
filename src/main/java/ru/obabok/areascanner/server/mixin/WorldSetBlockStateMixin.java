package ru.obabok.areascanner.server.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.obabok.areascanner.server.ServerScanManager;

@Mixin(World.class)
public abstract class WorldSetBlockStateMixin {
    @Redirect(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"
            )
    )
    private BlockState areaScannerOnSetBlockState(WorldChunk chunk, BlockPos pos, BlockState state, boolean moved) {
        BlockState oldState = chunk.getBlockState(pos);
        BlockState result = chunk.setBlockState(pos, state, moved);
        if (result == null) {
            return null;
        }
        World world = (World) (Object) this;
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            if (!oldState.equals(state)) {
                ServerScanManager.getInstance().onBlockStateChange(serverWorld, pos, oldState, state);
                //SharedScanManager.getInstance().onBlockStateChange(serverWorld, pos, oldState, state);
            }
        }
        return result;
    }
}
