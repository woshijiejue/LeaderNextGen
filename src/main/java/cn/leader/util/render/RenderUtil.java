package cn.leader.util.render;

import cn.leader.util.IMinecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class RenderUtil implements IMinecraft {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    public @NotNull Vec3 worldSpaceToScreenSpace(@NotNull Vec3 pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Window window = mc.getWindow();

        int width = window.getWidth();
        int height = window.getHeight();
        int[] viewport = {0, 0, width, height};

        Vec3 cameraPos = camera.position();
        Vector3f target = new Vector3f();
        Vector4f transformed = new Vector4f(
                (float) (pos.x - cameraPos.x),
                (float) (pos.y - cameraPos.y),
                (float) (pos.z - cameraPos.z),
                1.0F).mul(lastWorldSpaceMatrix);

        Matrix4f combined = new Matrix4f(lastProjMat).mul(lastModMat);
        combined.project(transformed.x(), transformed.y(), transformed.z(), viewport, target);

        return new Vec3(target.x, height - target.y, target.z);
    }

    public @NotNull Vec3[] getVectors(@NotNull Entity ent) {
        float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double x = ent.xo + (ent.getX() - ent.xo) * delta;
        double y = ent.yo + (ent.getY() - ent.yo) * delta;
        double z = ent.zo + (ent.getZ() - ent.zo) * delta;

        AABB box = ent.getBoundingBox();
        AABB interpolated = new AABB(
                box.minX - ent.getX() + x - 0.05, box.minY - ent.getY() + y, box.minZ - ent.getZ() + z - 0.05,
                box.maxX - ent.getX() + x + 0.05, box.maxY - ent.getY() + y + 0.15, box.maxZ - ent.getZ() + z + 0.05);

        return new Vec3[]{
                new Vec3(interpolated.minX, interpolated.minY, interpolated.minZ),
                new Vec3(interpolated.minX, interpolated.maxY, interpolated.minZ),
                new Vec3(interpolated.maxX, interpolated.minY, interpolated.minZ),
                new Vec3(interpolated.maxX, interpolated.maxY, interpolated.minZ),
                new Vec3(interpolated.minX, interpolated.minY, interpolated.maxZ),
                new Vec3(interpolated.minX, interpolated.maxY, interpolated.maxZ),
                new Vec3(interpolated.maxX, interpolated.minY, interpolated.maxZ),
                new Vec3(interpolated.maxX, interpolated.maxY, interpolated.maxZ)
        };
    }

    public static void drawRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        graphics.fill(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), color);
    }

    /**
     * 26.2 only offers a vertical gradient, so the horizontal case is emulated with strips.
     */
    public static void drawGradientRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                 int startColor, int endColor, boolean horizontal) {
        if (!horizontal) {
            graphics.fillGradient(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), startColor, endColor);
            return;
        }

        int strips = Math.max(1, Math.min(64, Math.round(Math.abs(width))));
        for (int index = 0; index < strips; index++) {
            float ratio = strips == 1 ? 0.0F : (float) index / (strips - 1);
            int color = ColorUtil.interpolate(startColor, endColor, ratio);

            int left = Math.round(x + width * index / strips);
            int right = Math.round(x + width * (index + 1) / strips);
            graphics.fill(left, Math.round(y), right, Math.round(y + height), color);
        }
    }

    private RenderUtil() {
    }

}
