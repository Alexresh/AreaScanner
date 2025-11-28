package ru.obabok.areascanner.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.RenderUtils;
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
import org.joml.Matrix4f;
import ru.obabok.areascanner.client.Config;
import ru.obabok.areascanner.client.Scan;
import ru.obabok.areascanner.client.mixin.WorldRendererAccessor;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;



public class RenderUtil {
    private static final ModelPart.Cuboid CUBE = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 16, 16, 0, 0, 0, false, 0, 0, EnumSet.allOf(Direction.class));
    private static final RenderLayer RENDER_LAYER = RenderLayer.getOutline(Identifier.of(References.MOD_ID, "none1.png"));
    private static final List<BlockPos> renderBlocksList = new CopyOnWriteArrayList<>();
    private static final List<ChunkPos> renderChunksList = new CopyOnWriteArrayList<>();

    public static void renderAll(WorldRenderContext context) {
        if(!Config.Generic.MAIN_RENDER.getBooleanValue()) return;
        if(MinecraftClient.getInstance().options.hudHidden) return;
        BlockBox scanRange = Scan.getRange();
        if(scanRange != null){
            BlockPos pos1 = new BlockPos(scanRange.getMinX(), scanRange.getMinY(), scanRange.getMinZ());
            BlockPos pos2 = new BlockPos(scanRange.getMaxX(), scanRange.getMaxY(), scanRange.getMaxZ());
            renderAreaOutline(pos1, pos2, 2, Color4f.fromColor(Colors.RED), Color4f.fromColor(Colors.GREEN),Color4f.fromColor(Colors.BLUE), MinecraftClient.getInstance());
            if(Config.Generic.AREA_EDGE_RENDER.getBooleanValue())
                renderAreaEdges(context, pos1, pos2);
        }

        //test render process scheduler
        if (!ChunkScheduler.getChunkQueue().isEmpty()){
            Queue<ChunkPos> queue = ChunkScheduler.getChunkQueue();
            queue.forEach(chunkPos -> {
                renderAreaEdges(context, chunkPos.getStartPos(), chunkPos.getStartPos().add(16,100,16));
            });
        }

        if(!renderChunksList.isEmpty() || !renderBlocksList.isEmpty()){
            try {
                context.matrixStack().push();
                context.matrixStack().translate(-context.camera().getPos().x, -context.camera().getPos().y, -context.camera().getPos().z);

                for (ChunkPos unloadedPos : renderChunksList){
                    if(unloadedPos != null && context.camera().getPos().distanceTo(new Vec3d(unloadedPos.getCenterX(), context.camera().getBlockPos().getY(), unloadedPos.getCenterZ())) < Config.Generic.UNLOADED_CHUNK_MAX_DISTANCE.getIntegerValue()) {
                        if(Config.Generic.OLD_CHUNK_RENDER.getBooleanValue()){
                            renderChunk(unloadedPos.getCenterAtY(context.camera().getBlockPos().getY() + Config.Generic.UNLOADED_CHUNK_Y_OFFSET.getIntegerValue()),
                                    context.matrixStack(),
                                    ((WorldRendererAccessor)context.worldRenderer()).getBufferBuilders().getOutlineVertexConsumers(),
                                    Color4f.fromColor(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue()),
                                    Config.Generic.UNLOADED_CHUNK_SCALE.getFloatValue());
                        }else{
                            drawPlane(context, unloadedPos);
                        }
                    }
                }
                for (BlockPos block : renderBlocksList){
                    float scale = (float) Math.min(1, context.camera().getPos().squaredDistanceTo(block.toCenterPos()) / 500);
                    scale = Math.max(scale, 0.05f);
                    if(context.camera().getPos().distanceTo(block.toCenterPos()) < Config.Generic.SELECTED_BLOCKS_MAX_DISTANCE.getIntegerValue() || Config.Generic.SELECTED_BLOCKS_MAX_DISTANCE.getIntegerValue() == -1){
                        if(Config.Generic.OLD_BLOCK_RENDER.getBooleanValue()){
                            oldRenderBlock(block, context.matrixStack(),
                                    ((WorldRendererAccessor)context.worldRenderer()).getBufferBuilders().getOutlineVertexConsumers(),
                                    Color4f.fromColor(Config.Generic.SELECTED_BLOCKS_COLOR.getIntegerValue()),
                                    scale);
                        }else{
                            maliRenderBlock(block, Color4f.fromColor(Config.Generic.SELECTED_BLOCKS_COLOR.getIntegerValue()));
                        }
                    }

                }

            }catch (Exception ignored){

            }finally {
                context.matrixStack().pop();
            }

        }
    }

    private static void drawPlane(WorldRenderContext context, ChunkPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        double startX = pos.getStartX();
        double startZ = pos.getStartZ();
        double endX = startX + 16;
        double endZ = startZ + 16;
        double planeY = client.player.getY() + Config.Generic.UNLOADED_CHUNK_Y_OFFSET.getIntegerValue();

        // Сохраняем предыдущие настройки рендеринга
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        MatrixStack matrices = context.matrixStack();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();


        // Вершины плоскости - используй чистые мировые координаты БЕЗ смещения камеры
        buffer.vertex(positionMatrix, (float)startX, (float)planeY, (float)startZ)
                .color(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)endX, (float)planeY, (float)startZ)
                .color(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)endX, (float)planeY, (float)endZ)
                .color(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)startX, (float)planeY, (float)endZ)
                .color(Config.Generic.UNLOADED_CHUNK_COLOR.getIntegerValue());

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Восстанавливаем настройки рендеринга
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void lookRandomSelectedBlock(){
        if(MinecraftClient.getInstance().player == null) return;
        Random random = new Random();
        Vec3d pos = renderBlocksList.get(random.nextInt(renderBlocksList.size())).toCenterPos();
        MinecraftClient.getInstance().player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, pos);
    }
    public static void clearRender(){
        renderBlocksList.clear();
        renderChunksList.clear();
    }

    //original
    private static void renderChunk(BlockPos pos, MatrixStack matrices, OutlineVertexConsumerProvider vertexConsumers, Color4f color, float scale){
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
    private static void oldRenderBlock(BlockPos pos, MatrixStack matrices, OutlineVertexConsumerProvider vertexConsumers, Color4f color, float baseScale) {
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
        matrices.scale(scaleXZ, scaleY, scaleXZ);
        matrices.translate(-0.5, -0.5, -0.5);

        CUBE.renderCuboid(matrices.peek(), setColorFromHex(vertexConsumers, color), 0, OverlayTexture.DEFAULT_UV, 0);

        matrices.pop();
    }

    private static void maliRenderBlock(BlockPos pos, Color4f color) {

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double dx = pos.getX() + 0.5 - cameraPos.x;
        double dz = pos.getZ() + 0.5 - cameraPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        double t = Math.max(0.0, Math.min(1.0, (distXZ - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue()) / (Config.Generic.SELECTED_BLOCKS_MOVE_MAX_DISTANCE.getIntegerValue() - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue())));
        double renderY = MathHelper.lerp(t, pos.getY() + 0.5, cameraPos.y);
        float farHeightScale = 8.0f;
        float scaleY = distXZ - Config.Generic.SELECTED_BLOCKS_MOVE_MIN_DISTANCE.getIntegerValue() < 0 ? 0 : farHeightScale;
        int renderYInt = MathHelper.floor(renderY);
        renderAreaSidesBatched(pos.withY(renderYInt), pos.withY(renderYInt).offset(Direction.Axis.Y, (int)scaleY), color, 0.002, buffer, MinecraftClient.getInstance());

        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception ignored) { }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color,
                                              double expand, BufferBuilder buffer, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;
        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;

        RenderUtils.drawBoxAllSidesBatchedQuads((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, fi.dy.masa.malilib.util.Color4f.fromColor(color.getIntValue()) , buffer);
    }

    private static VertexConsumer setColorFromHex(OutlineVertexConsumerProvider vertexConsumers, Color4f hexColor) {
        vertexConsumers.setColor(hexColor.ri, hexColor.gi, hexColor.bi, hexColor.ai);
        return vertexConsumers.getBuffer(RENDER_LAYER);
    }
    
    public static void addAllRenderBlocks(HashSet<BlockPos> blocks) {
        if(Config.Generic.MAIN_RENDER.getBooleanValue()) renderBlocksList.addAll(blocks);
    }
    public static void addAllRenderChunks(HashSet<ChunkPos> chunkPos) {
        if(Config.Generic.MAIN_RENDER.getBooleanValue()) renderChunksList.addAll(chunkPos);
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

    private static void renderAreaEdges(WorldRenderContext context, BlockPos pos1, BlockPos pos2) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Сохраняем текущее состояние матриц
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        // Откатываем преобразование камеры - получаем чистую мировую матрицу
        Camera camera = context.camera();
        matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // Нижняя грань (пол)
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        // Верхняя грань (потолок)
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        // Передняя грань (север - Z min)
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        // Задняя грань (юг - Z max)
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        // Левая грань (запад - X min)
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)minX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        // Правая грань (восток - X max)
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)minZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)maxY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());
        buffer.vertex(positionMatrix, (float)maxX, (float)minY, (float)maxZ).color(Config.Generic.AREA_EDGE_COLOR.getIntegerValue());

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Восстанавливаем матрицы
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    private static void drawBoundingBoxEdges(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

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
