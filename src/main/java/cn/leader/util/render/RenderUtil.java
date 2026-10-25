package cn.leader.util.render;

import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@UtilityClass
public final class RenderUtil implements IMinecraft {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    public @NotNull Vec3 worldSpaceToScreenSpace(@NotNull Vec3 pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = {0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight()};
        Vector3f target = new Vector3f();
        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;
        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.0f).mul(lastWorldSpaceMatrix);
        Matrix4f combinedMatrix = new Matrix4f(lastProjMat).mul(lastModMat);
        combinedMatrix.project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
        double scaleFactor = mc.getWindow().getScaleFactor();
        return new Vec3(target.x / scaleFactor, (displayHeight - target.y) / scaleFactor, target.z);
    }

    public @NotNull Vec3[] getVectors(@NotNull Entity ent) {
        double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getRenderTickCounter().getTickDelta(true);
        double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getRenderTickCounter().getTickDelta(true);
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getRenderTickCounter().getTickDelta(true);
        AABB axisAlignedBB2 = ent.getBoundingBox();
        AABB axisAlignedBB = new AABB(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
        return new Vec3[]{new Vec3(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
    }

    public void drawRect(GuiGraphics context, float x, float y, float width, float height, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        context.draw(vertexConsumers -> {
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.getGui());
            buffer.vertex(matrix, 0, 0, 0).color(color);
            buffer.vertex(matrix, 0, height, 0).color(color);
            buffer.vertex(matrix, width, height, 0).color(color);
            buffer.vertex(matrix, width, 0, 0).color(color);
        });

        context.getMatrices().pop();
    }

    public void drawGradientRect(GuiGraphics context, float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        context.draw(vertexConsumers -> {
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.getGui());
            if (horizontal) {
                buffer.vertex(matrix, 0, 0, 0).color(startColor);
                buffer.vertex(matrix, 0, height, 0).color(startColor);
                buffer.vertex(matrix, width, height, 0).color(endColor);
                buffer.vertex(matrix, width, 0, 0).color(endColor);
            } else {
                buffer.vertex(matrix, 0, 0, 0).color(startColor);
                buffer.vertex(matrix, 0, height, 0).color(endColor);
                buffer.vertex(matrix, width, height, 0).color(endColor);
                buffer.vertex(matrix, width, 0, 0).color(startColor);
            }
        });

        context.getMatrices().pop();
    }
}