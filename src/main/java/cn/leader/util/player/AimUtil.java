package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;


/**
 * Aim helpers shared by the combat modules: where to point at a hitbox, how far it is, and whether
 * the point is reachable. Rotation stepping itself lives in {@code RotationManager}.
 */
public final class AimUtil implements IMinecraft {

    /** Shrink factor so an aim point is strictly inside the hitbox instead of on its edge. */
    private static final double SURFACE_INSET = 1.0E-4D;

    /**
     * Closest point of the hitbox to the eyes, pulled slightly inside the box so that a ray
     * aimed at it always intersects the real box (instead of grazing the boundary).
     */
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

    /** Yaw/pitch pair that points at the given world position. */
    public static float[] rotationTo(Vec3 point) {
        return RotationUtil.getRotations(point);
    }

    /** Shortest eye to hitbox distance, used for range filtering without any raycast cost. */
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

    /** Angle in degrees between the given look direction and the direction to the entity centre. */
    public static double angleTo(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return 0.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 delta = entity.getBoundingBox().getCenter().subtract(eye);
        if (delta.lengthSqr() < 1.0E-6D) return 0.0D;

        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        double dot = look.dot(delta.normalize());
        return Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0D, 1.0D)));
    }

    /**
     * Distance from the eyes to the first intersection of the given look direction with the
     * hitbox, or {@code -1} when the direction misses the box completely.
     */
    public static double reach(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return -1.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        Vec3 hit = intersect(entity.getBoundingBox(), eye, look);
        return hit == null ? -1.0D : eye.distanceTo(hit);
    }

    /** Slab method ray/AABB intersection, returns the entry point or {@code null}. */
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

    /** True when no collidable block sits between the eyes and the point. */
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
