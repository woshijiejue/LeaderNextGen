package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.Random;

/**
 * Aim mathematics shared by the combat modules.
 *
 * <p>The whole point of this class is to describe aim the way the game actually encodes it:
 * the look direction is not a free floating pair of floats, it is a whole number of
 * "mouse dots" away from the previous direction. {@link #step(float[], float[], float)}
 * therefore works in dots instead of degrees, which keeps every delta we send consistent
 * with the mouse input the player really produces.</p>
 */
@UtilityClass
public class AimUtil implements IMinecraft {

    /** Shrink factor so an aim point is strictly inside the hitbox instead of on its edge. */
    private static final double SURFACE_INSET = 1.0E-4D;

    private static final Random RANDOM = new Random();

    /**
     * Degrees per mouse dot for the current sensitivity.
     * Sensitivity S becomes {@code (0.6S + 0.2)^3 * 8 * 0.15} degrees of yaw per dot.
     */
    public double mouseDivisor() {
        double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
        return sensitivity * sensitivity * sensitivity * 8.0D * 0.15D;
    }

    /**
     * Closest point of the hitbox to the eyes, pulled slightly inside the box so that a ray
     * aimed at it always intersects the real box (instead of grazing the boundary).
     */
    public Vec3 aimPoint(LivingEntity entity) {
        AABB box = entity.getBoundingBox().shrink(SURFACE_INSET, SURFACE_INSET, SURFACE_INSET);

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
    public float[] rotationTo(Vec3 point) {
        return RotationUtil.getRotations(point);
    }

    /** Shortest eye to hitbox distance, used for range filtering without any raycast cost. */
    public double distanceToBox(LivingEntity entity) {
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
    public double angleTo(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return 0.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 delta = entity.getBoundingBox().getCenter().subtract(eye);
        if (delta.lengthSquared() < 1.0E-6D) return 0.0D;

        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        double dot = look.dotProduct(delta.normalize());
        return Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0D, 1.0D)));
    }

    /**
     * Distance from the eyes to the first intersection of the given look direction with the
     * hitbox, or {@code -1} when the direction misses the box completely.
     */
    public double reach(LivingEntity entity, float[] rotation) {
        if (mc.player == null || rotation == null) return -1.0D;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.calculateViewVector(rotation[1], rotation[0]);
        Vec3 hit = intersect(entity.getBoundingBox(), eye, look);
        return hit == null ? -1.0D : eye.distanceTo(hit);
    }

    /** Slab method ray/AABB intersection, returns the entry point or {@code null}. */
    public Vec3 intersect(AABB box, Vec3 origin, Vec3 direction) {
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

        return origin.add(direction.multiply(near));
    }

    /** True when no collidable block sits between the eyes and the point. */
    public boolean isVisible(Vec3 point) {
        if (mc.player == null || mc.world == null) return false;

        Vec3 eye = mc.player.getEyePosition();
        ClipContext context = new ClipContext(
                eye, point, ClipContext.ShapeType.COLLIDER, ClipContext.FluidHandling.NONE, mc.player);

        return mc.world.raycast(context).getType() == HitResult.Type.MISS;
    }

    /**
     * Advance {@code current} towards {@code target} by at most {@code maxDegrees}, in whole dots.
     *
     * <p>Rounding to the dot grid before moving guarantees that the delta we send is a multiple
     * of the divisor the player's sensitivity produces, and that we never repeat a rotation we
     * already sent (a delta of zero dots means "no packet").</p>
     */
    public float[] step(float[] current, float[] target, float maxDegrees) {
        if (current == null || target == null) return current;

        double divisor = mouseDivisor();
        if (divisor < 1.0E-6D) {
            return new float[]{target[0], Mth.clamp(target[1], -90.0F, 90.0F)};
        }

        int limit = Math.max(1, (int) Math.floor(Math.max(1.0F, maxDegrees) / divisor));

        int yawDots = (int) Math.round(Mth.wrapDegrees(target[0] - current[0]) / divisor);
        int pitchDots = (int) Math.round((target[1] - current[1]) / divisor);

        yawDots = Mth.clamp(yawDots, -limit, limit);
        pitchDots = Mth.clamp(pitchDots, -limit, limit);

        // Slightly undershoot large corrections now and then: a human hand does not land
        // exactly on the target every single tick. Undershooting by whole dots keeps the
        // delta on the sensitivity grid, so it still looks like an ordinary mouse sweep.
        if (yawDots != 0 && Math.abs(yawDots) > 2 && RANDOM.nextDouble() < 0.28D) {
            yawDots -= Integer.signum(yawDots) * (RANDOM.nextBoolean() ? 1 : 2);
        }

        float yaw = (float) (current[0] + yawDots * divisor);
        float pitch = (float) (current[1] + pitchDots * divisor);

        return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
    }
}
