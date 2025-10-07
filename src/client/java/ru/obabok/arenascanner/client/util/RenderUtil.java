package ru.obabok.arenascanner.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import ru.obabok.arenascanner.Config;
import ru.obabok.arenascanner.References;
import ru.obabok.arenascanner.client.ScanCommand;
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

        if(ScanCommand.getRange() != null){
            BlockPos pos1 = new BlockPos(ScanCommand.getRange().getMinX(),ScanCommand.getRange().getMinY(), ScanCommand.getRange().getMinZ());
            BlockPos pos2 = new BlockPos(ScanCommand.getRange().getMaxX(),ScanCommand.getRange().getMaxY(), ScanCommand.getRange().getMaxZ());
            renderAreaOutline(pos1, pos2, 2, Color4f.fromColor(Colors.RED), Color4f.fromColor(Colors.GREEN),Color4f.fromColor(Colors.BLUE), MinecraftClient.getInstance());
        }

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
        double dx = pos.getX() + 0.5 - cameraPos.x;
        double dz = pos.getZ() + 0.5 - cameraPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        double t = Math.max(0.0, Math.min(1.0, (distXZ - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue()) / (Config.Generic.SELECTED_BLOCKS_MOVE_MAX_DISTANCE.getIntegerValue() - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue())));


        double renderY = MathHelper.lerp(t, pos.getY() + 0.5, cameraPos.y);


        float farHeightScale = 8.0f;
        float scaleY = (float) MathHelper.lerp(t, baseScale, farHeightScale);


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
        final double MAX_DISTANCE = 15.0;

        double normalizedDistance = Math.min(distance / MAX_DISTANCE, 1.0);

        int color1 = getColorFromString(hexColor);
        int color2 = getColorFromString(targetHexColor);

        int r1 = (color1 >> 24) & 0xff;
        int g1 = (color1 >> 16) & 0xff;
        int b1 = (color1 >> 8) & 0xff;
        int a1 = color1 & 0xff;

        int r2 = (color2 >> 24) & 0xff;
        int g2 = (color2 >> 16) & 0xff;
        int b2 = (color2 >> 8) & 0xff;
        int a2 = color2 & 0xff;

        int r = (int) ((r1 * (1.0 - normalizedDistance)) + (r2 * normalizedDistance));
        int g = (int) ((g1 * (1.0 - normalizedDistance)) + (g2 * normalizedDistance));
        int b = (int) ((b1 * (1.0 - normalizedDistance)) + (b2 * normalizedDistance));
        int a = (int) ((a1 * (1.0 - normalizedDistance)) + (a2 * normalizedDistance));

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

    static void startDrawingLines()
    {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }
    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
                                         Color4f colorX, Color4f colorY, Color4f colorZ, MinecraftClient mc)
    {
        RenderSystem.lineWidth(lineWidth);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double minX = Math.min(pos1.getX(), pos2.getX()) - dx;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz;
        double maxX = Math.max(pos1.getX(), pos2.getX()) - dx + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) - dy + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) - dz + 1;

        drawBoundingBoxEdges((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, colorX, colorY, colorZ);
    }
    private static void drawBoundingBoxEdges(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        startDrawingLines();

        drawBoundingBoxLinesX(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorX);
        drawBoundingBoxLinesY(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorY);
        drawBoundingBoxLinesZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, colorZ);

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception ignored) { }
    }
    private static void drawBoundingBoxLinesX(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f color)
    {
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);

        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
    }

}
