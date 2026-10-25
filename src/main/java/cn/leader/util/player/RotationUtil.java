package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import cn.injection.accessor.EntityAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import net.minecraft.world.level.ClipContext;

public final class RotationUtil implements IMinecraft {

    public static float[] nearestRotation(final AABB box) {
        if (mc.player == null) return null;

        final Vec3 targetPoint = getNearestPointBB(box);
        return getRotations(targetPoint);
    }

    public static Vec3 getNearestPointBB(AABB box) {
        if (mc.player == null) return null;

        Vec3 eyePos = mc.player.getEyePosition();

        double x = Mth.clamp(eyePos.x, box.minX, box.maxX);
        double y = Mth.clamp(eyePos.y, box.minY, box.maxY);
        double z = Mth.clamp(eyePos.z, box.minZ, box.maxZ);
        Vec3 nearestPoint = new Vec3(x, y, z);

        if (isVisible(nearestPoint)) {
            return nearestPoint;
        }

        AABB scanBox = box.deflate(0.002);

        double stepX = scanBox.getXsize();
        double stepY = scanBox.getYsize();
        double stepZ = scanBox.getZsize();

        Vec3 bestPoint = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (double sX = scanBox.minX; sX <= scanBox.maxX; sX += stepX / 2.0) {
            for (double sY = scanBox.minY; sY <= scanBox.maxY; sY += stepY / 2.0) {
                for (double sZ = scanBox.minZ; sZ <= scanBox.maxZ; sZ += stepZ / 2.0) {
                    Vec3 currentPoint = new Vec3(sX, sY, sZ);

                    if (isVisible(currentPoint)) {
                        double distSq = eyePos.distanceToSqr(currentPoint);
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq;
                            bestPoint = currentPoint;
                        }
                    }
                }
            }
        }

        return (bestPoint != null) ? bestPoint : nearestPoint;
    }

    public static boolean isVisible(Vec3 targetVec) {
        if (mc.player == null || mc.level == null) return false;

        Vec3 eyePos = mc.player.getEyePosition();
        if (eyePos.distanceToSqr(targetVec) > 4096.0) return false;
        Vec3 lookVec = mc.player.getViewVector(1);
        Vec3 toTarget = targetVec.subtract(eyePos).normalize();
        if (lookVec.dot(toTarget) < 0) return false;
        ClipContext context = new ClipContext(eyePos, targetVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        return mc.level.clip(context).getType() == HitResult.Type.MISS;
    }

    public static float getDistanceToEntity(LivingEntity target) {
        if (mc.player == null || target == null) return 0.0f;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 nearestPoint = getNearestPointBB(target.getBoundingBox());
        return (float) eyePos.distanceTo(nearestPoint);
    }

    public static double getRotationDifference(LivingEntity entity) {
        if (mc.player == null || entity == null) return 0;

        Vec3 targetPos = entity.position();
        float[] neededRotations = RotationUtil.getRotations(targetPos);
        if (neededRotations == null) return 180.0;

        float yawDiff = Math.abs(Mth.wrapDegrees(mc.player.getYRot() - neededRotations[0]));
        float pitchDiff = Math.abs(Mth.wrapDegrees(mc.player.getXRot() - neededRotations[1]));

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    public static float[] getRotations(Vec3 target) {
        if (mc.player == null) return null;
        return getRotations(mc.player.getEyePosition(), target);
    }

    public static float[] getRotations(Vec3 origin, Vec3 target) {
        return getRotations(target.x, target.y, target.z, origin.x, origin.y, origin.z);
    }

    public static float[] getRotations(double rotX, double rotY, double rotZ, double startX, double startY, double startZ) {
        double diffX = rotX - startX;
        double diffY = rotY - startY;
        double diffZ = rotZ - startZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        return new float[]{yaw, pitch};
    }

    public static float[] getSmoothRotation(float[] lastRotation, float[] targetRotation, double speed) {
        float yaw = targetRotation[0];
        float pitch = targetRotation[1];
        float lastYaw = lastRotation[0];
        float lastPitch = lastRotation[1];

        if (speed != 0) {
            float rotationSpeed = (float) speed;

            double deltaYaw = Mth.wrapDegrees(targetRotation[0] - lastRotation[0]);
            double deltaPitch = pitch - lastPitch;

            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            double distributionYaw = Math.abs(deltaYaw / distance);
            double distributionPitch = Math.abs(deltaPitch / distance);

            double maxYaw = rotationSpeed * distributionYaw;
            double maxPitch = rotationSpeed * distributionPitch;

            float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            yaw = lastYaw + moveYaw;
            pitch = lastPitch + movePitch;
        }

        boolean randomise = Math.random() > 0.8;

        for (int i = 1; i <= (int) (2 + Math.random() * 2); ++i) {
            if (randomise) {
                yaw += (float) ((Math.random() - 0.5) / 100000000);
                pitch -= (float) (Math.random() / 200000000);
            }

            // Fixing GCD
            float[] rotations = new float[]{yaw, pitch};
            float[] fixedRotations = applySensitivityPatch(rotations);

            // Setting rotations
            if (fixedRotations != null) {
                yaw = fixedRotations[0];
                pitch = Mth.clamp(fixedRotations[1], -90.0F, 90.0F);
            }
        }

        return new float[]{yaw, pitch};
    }

    public static float[] applySensitivityPatch(float[] rotations) {
        if (mc.player == null) return null;
        EntityAccessor accessor = (EntityAccessor) mc.player;
        float[] prevRotations = new float[]{accessor.getPrevYaw(), accessor.getPrevPitch()};
        return applySensitivityPatch(rotations, prevRotations);
    }

    public static float[] applySensitivityPatch(float[] rotation, float[] previousRotation) {
        float mouseSensitivity = (float) (mc.options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        float yaw = previousRotation[0] + (float) (Math.round((rotation[0] - previousRotation[0]) / multiplier) * multiplier);
        float pitch = previousRotation[1] + (float) (Math.round((rotation[1] - previousRotation[1]) / multiplier) * multiplier);
        return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    private RotationUtil() {
    }

}
