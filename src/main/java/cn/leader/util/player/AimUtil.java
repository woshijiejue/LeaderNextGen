package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public final class AimUtil implements IMinecraft {

    private static final double SURFACE_INSET = 1.0E-4D;

    public static Vec3 aimPoint(LivingEntity entity) {
        AABB box = entity.getBoundingBox().deflate(SURFACE_INSET);

        if (mc.player == null) {
            return box.getCenter();
        }

        Vec3 eye = mc.player.getEyePosition();
        return new Vec3(
                Mth.clamp(eye.x, box.minX, box.maxX),
                Mth.clamp(eye.y, box.minY, box.maxY),
                Mth.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    public static float[] rotationTo(Vec3 point) {
        return RotationUtil.getRotations(point);
    }

    public static double distanceToBox(LivingEntity entity) {
        if (mc.player == null) return Double.MAX_VALUE;

        AABB box = entity.getBoundingBox();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 nearest = new Vec3(
                Mth.clamp(eye.x, box.minX, box.maxX),
                Mth.clamp(eye.y, box.minY, box.maxY),
                Mth.clamp(eye.z, box.minZ, box.maxZ)
        );
        return eye.distanceTo(nearest);
    }

    public static double angleTo(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return 0.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 delta = entity.getBoundingBox().getCenter().subtract(eye);
        if (delta.lengthSqr() < 1.0E-6D) return 0.0D;

        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        double dot = look.dot(delta.normalize());
        return Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0D, 1.0D)));
    }

    public static double reach(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return -1.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        Vec3 hit = intersect(entity.getBoundingBox(), eye, look);
        return hit == null ? -1.0D : eye.distanceTo(hit);
    }

    public static Vec3 intersect(AABB box, Vec3 origin, Vec3 direction) {
        double[] o = {origin.x, origin.y, origin.z};
        double[] d = {direction.x, direction.y, direction.z};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};

        double near = 0.0D;
        double far = Double.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < 1.0E-8D) {
                if (o[axis] < min[axis] || o[axis] > max[axis]) return null;
                continue;
            }

            double t1 = (min[axis] - o[axis]) / d[axis];
            double t2 = (max[axis] - o[axis]) / d[axis];
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }

            near = Math.max(near, t1);
            far = Math.min(far, t2);

            if (near > far) return null;
        }

        return origin.add(direction.scale(near));
    }

    public static boolean isVisible(Vec3 point) {
        if (mc.player == null || mc.level == null) return false;

        Vec3 eye = mc.player.getEyePosition();
        ClipContext context = new ClipContext(
                eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);

        return mc.level.clip(context).getType() == HitResult.Type.MISS;
    }

    private AimUtil() {
    }

}
