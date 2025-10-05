package ru.obabok.arenascanner.client.util;

import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import ru.obabok.arenascanner.Config;
import ru.obabok.arenascanner.References;
import ru.obabok.arenascanner.client.mixin.WorldRendererAccessor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;



public class RenderUtil {
    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0, EnumSet.allOf(Direction.class));
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(Identifier.of(References.MOD_ID, "none1.png"));
    private static final List<BlockPos> renderBlocksList = new CopyOnWriteArrayList<>();
    private static final List<ChunkPos> renderChunksList = new CopyOnWriteArrayList<>();

    public static void renderAll(WorldRenderContext context) {
        if(!renderChunksList.isEmpty() || !renderBlocksList.isEmpty()){
            try {
                context.matrixStack().push();
                context.matrixStack().translate(-context.camera().getPos().x, -context.camera().getPos().y, -context.camera().getPos().z);

                for (ChunkPos unloadedPos : renderChunksList){
                    if(unloadedPos != null && context.camera().getPos().distanceTo(new Vec3d(unloadedPos.getCenterX(), context.camera().getBlockPos().getY(), unloadedPos.getCenterZ())) < Config.Generic.UNLOADED_CHUNK_MAX_DISTANCE.getIntegerValue()) {
                        renderBlock(unloadedPos.getCenterAtY(context.camera().getBlockPos().getY() + Config.Generic.UNLOADED_CHUNK_Y_OFFSET.getIntegerValue()), context.matrixStack(), ((WorldRendererAccessor)context.worldRenderer()).getBufferBuilders().getOutlineVertexConsumers(), Color4f.fromColor(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue()) , Config.Generic.UNLOADED_CHUNK_SCALE.getFloatValue());
                    }
                }
                for (BlockPos block : renderBlocksList){
                    float scale = (float) Math.min(1, context.camera().getPos().squaredDistanceTo(block.toCenterPos()) / 500);
                    scale = Math.max(scale, 0.05f);
                    if(context.camera().getPos().distanceTo(block.toCenterPos()) < Config.Generic.SELECTED_BLOCKS_MAX_DISTANCE.getIntegerValue() || Config.Generic.SELECTED_BLOCKS_MAX_DISTANCE.getIntegerValue() == -1){
                        renderBlock1(block, context.matrixStack(), ((WorldRendererAccessor)context.worldRenderer()).getBufferBuilders().getOutlineVertexConsumers(), Color4f.fromColor(Config.Generic.SELECTED_BLOCKS_COLOR.getIntegerValue()), scale);
                    }

                }
                context.matrixStack().pop();
            }catch (Exception ignored){

            }

        }
    }

    public static void lookRandomSelectedBlock(){
        try {
            Random random = new Random();
            Vec3d pos = renderBlocksList.get(random.nextInt(renderBlocksList.size())).toCenterPos();
            MinecraftClient.getInstance().player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, pos);
        }catch (Exception ignored){}
    }
    public static void clearRender(){
        renderBlocksList.clear();
        renderChunksList.clear();
    }

    //original
    private static void renderBlock(BlockPos pos, MatrixStack matrices, OutlineVertexConsumerProvider vertexConsumers, Color4f color, float scale){
        matrices.push();
        matrices.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        matrices.scale(scale, scale, scale);
        {
            matrices.push();
            matrices.translate(-0.5, -0.5, -0.5);
            CUBE.renderCuboid(matrices.peek(), setColorFromHex(vertexConsumers, color), 0, OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
        }
        matrices.pop();
    }
    //original


    private static void renderBlock1(BlockPos pos, MatrixStack matrices, OutlineVertexConsumerProvider vertexConsumers, Color4f color, float baseScale) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double dist = cameraPos.distanceTo(pos.toCenterPos());
        // Горизонтальное расстояние (игнорируем Y)
        double dx = pos.getX() + 0.5 - cameraPos.x;
        double dz = pos.getZ() + 0.5 - cameraPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        // Параметры плавного перехода


        double t = Math.max(0.0, Math.min(1.0, (distXZ - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue()) / (Config.Generic.SELECTED_BLOCKS_MOVE_MAX_DISTANCE.getIntegerValue() - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue())));

        // Y-позиция: интерполируем между blockY и cameraY
        double renderY = MathHelper.lerp(t, pos.getY() + 0.5, cameraPos.y);

        // Масштаб по Y: от baseScale до большого значения
        float farHeightScale = 8.0f;
        float scaleY = (float) MathHelper.lerp(t, baseScale, farHeightScale);

        // Масштаб по X и Z:
        // - Вблизи (t=0): = baseScale → куб
        // - Вдали (t=1): = уменьшенный, но не слишком
        float farWidthScale = Math.max(0.15f, 0.4f / (float)(1.0 + distXZ * 0.03));
        float scaleXZ = (float) MathHelper.lerp(t, baseScale, farWidthScale);

        matrices.push();

        matrices.translate(pos.getX() + 0.5, renderY, pos.getZ() + 0.5);
        matrices.scale(scaleXZ, scaleY, scaleXZ); // ← теперь X и Z одинаковые, Y — отдельно
        matrices.translate(-0.5, -0.5, -0.5);

        CUBE.renderCuboid(matrices.peek(), setColorFromHex(vertexConsumers, color), 0, OverlayTexture.DEFAULT_UV, 0);

        matrices.pop();
    }

    private static VertexConsumer setColorFromHex(OutlineVertexConsumerProvider vertexConsumers, Color4f hexColor) {
        vertexConsumers.setColor(hexColor.ri, hexColor.gi, hexColor.bi, hexColor.ai);
        return vertexConsumers.getBuffer(RENDER_LAYER);
    }
    private static VertexConsumer setColorFromHex(OutlineVertexConsumerProvider vertexConsumers, String hexColor, String targetHexColor, double distance) {
        // Максимальная дистанция, при которой цвет полностью меняется на targetHexColor
        final double MAX_DISTANCE = 15.0;

        // Нормализуем дистанцию (0.0 - 1.0)
        double normalizedDistance = Math.min(distance / MAX_DISTANCE, 1.0);

        // Получаем исходный и целевой цвета
        int color1 = getColorFromString(hexColor);
        int color2 = getColorFromString(targetHexColor);

        // Извлекаем компоненты цветов
        int r1 = (color1 >> 24) & 0xff;
        int g1 = (color1 >> 16) & 0xff;
        int b1 = (color1 >> 8) & 0xff;
        int a1 = color1 & 0xff;

        int r2 = (color2 >> 24) & 0xff;
        int g2 = (color2 >> 16) & 0xff;
        int b2 = (color2 >> 8) & 0xff;
        int a2 = color2 & 0xff;

        // Интерполируем компоненты цветов
        int r = (int) ((r1 * (1.0 - normalizedDistance)) + (r2 * normalizedDistance));
        int g = (int) ((g1 * (1.0 - normalizedDistance)) + (g2 * normalizedDistance));
        int b = (int) ((b1 * (1.0 - normalizedDistance)) + (b2 * normalizedDistance));
        int a = (int) ((a1 * (1.0 - normalizedDistance)) + (a2 * normalizedDistance));

        // Устанавливаем цвет
        vertexConsumers.setColor(r, g, b, a);
        return vertexConsumers.getBuffer(RENDER_LAYER);
    }

    public static int getColorFromString(String hexColor){
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }
        if (hexColor.length() == 6) {
            hexColor += "FF";
        }
        return (int) Long.parseLong(hexColor, 16);
    }
    public static void addAllRenderBlocks(HashSet<BlockPos> blocks) {
        if(Config.Generic.MAIN_RENDER.getBooleanValue()) renderBlocksList.addAll(blocks);
    }
    public static void addAllRenderChunks(HashSet<ChunkPos> chunkPos) {
        if(Config.Generic.MAIN_RENDER.getBooleanValue()) renderChunksList.addAll(chunkPos);
    }
}
