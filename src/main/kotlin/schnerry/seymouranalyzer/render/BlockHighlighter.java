package schnerry.seymouranalyzer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages and renders highlighted block positions in the world.
 * Renders through walls by temporarily disabling depth test during render.
 */
public class BlockHighlighter {
    private static BlockHighlighter instance;
    private final List<BlockPos> highlightedBlocks = new ArrayList<>();

    private BlockHighlighter() {
        // Use LAST event to render after everything else
        WorldRenderEvents.LAST.register(this::renderHighlights);
    }

    public static BlockHighlighter getInstance() {
        if (instance == null) {
            instance = new BlockHighlighter();
        }
        return instance;
    }

    public void addBlock(BlockPos pos) {
        if (!highlightedBlocks.contains(pos)) {
            highlightedBlocks.add(pos);
        }
    }

    public void addBlocks(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            addBlock(pos);
        }
    }

    public void clearAll() {
        highlightedBlocks.clear();
    }

    private void renderHighlights(WorldRenderContext context) {
        if (highlightedBlocks.isEmpty()) return;

        PoseStack matrices = context.matrices();
        if (matrices == null) return;

        MultiBufferSource.BufferSource immediate = context.consumers() instanceof MultiBufferSource.BufferSource imm
            ? imm : null;
        if (immediate == null) return;

        Vec3 cameraPos = context.camera().getPos();

        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f positionMatrix = matrices.last().pose();

        // Green color
        float red = 0.0f;
        float green = 1.0f;
        float blue = 0.0f;
        float alpha = 1.0f;

        // Get vertex consumer for lines
        VertexConsumer consumer = immediate.getBuffer(RenderTypes.lines());

        // Draw all highlights
        for (BlockPos pos : highlightedBlocks) {
            drawBoxOutline(consumer, positionMatrix, pos, red, green, blue, alpha);
        }

        // Flush the buffer with depth test disabled so it renders through walls
        // Save current state
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        // Disable depth test before drawing
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Force draw the buffered vertices
        immediate.endBatch(RenderTypes.lines());

        // Restore depth test state
        if (depthEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        matrices.popPose();
    }

    private void drawBoxOutline(VertexConsumer consumer, Matrix4f matrix, BlockPos pos,
                                 float red, float green, float blue, float alpha) {
        float minX = pos.getX();
        float minY = pos.getY();
        float minZ = pos.getZ();
        float maxX = pos.getX() + 1.0f;
        float maxY = pos.getY() + 1.0f;
        float maxZ = pos.getZ() + 1.0f;

        // Bottom edges
        line(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        line(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        // Top edges
        line(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        line(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        // Vertical edges
        line(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private void line(VertexConsumer consumer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float r, float g, float b, float a) {
        // Calculate normal for line
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) len = 1;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
