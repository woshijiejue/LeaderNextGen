package cn.leader.util.player;

import cn.leader.event.impl.MoveInputEvent;
import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class MovementUtil implements IMinecraft {

    public void strafe() {
        strafe(getSpeed());
    }

    public void strafe(double speed) {
        if (mc.player == null || !isMoving()) return;

        double yaw = getDirection();
        mc.player.setVelocity(-Mth.sin((float) yaw) * speed, mc.player.getVelocity().y, Mth.cos((float) yaw) * speed);
    }

    public boolean isMoving() {
        if (mc.player == null) return false;
        return mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f;
    }

    public double getDirection() {
        if (mc.player == null) return 0;
        return getDirection(mc.player.getYaw(), mc.player.input.movementForward, mc.player.input.movementSideways);
    }

    public double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public double getSpeed() {
        if (mc.player == null) return 0;

        double x = mc.player.getVelocity().x;
        double z = mc.player.getVelocity().z;

        return Math.sqrt(x * x + z * z);
    }

    public void fixMovement(MoveInputEvent event, float yaw) {
        if (mc.player == null) return;

        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = Mth.wrapDegrees(Math.toDegrees(getDirection(mc.player.getYaw(), forward, strafe)));

        if (forward == 0 && strafe == 0) return;

        float closestForward = 0, closestStrafe = 0;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                double predictedAngle = Mth.wrapDegrees(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
            float f = Mth.sin(yaw * ((float) Math.PI / 180F));
            float g = Mth.cos(yaw * ((float) Math.PI / 180F));
            return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
        }
    }
}