package cn.leader.util.player;

import cn.leader.event.impl.MoveInputEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MovementUtil implements IMinecraft {

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(double speed) {
        if (mc.player == null || !isMoving()) return;

        double yaw = getDirection();
        mc.player.setDeltaMovement(-Mth.sin((float) yaw) * speed, mc.player.getDeltaMovement().y, Mth.cos((float) yaw) * speed);
    }

    public static boolean isMoving() {
        if (mc.player == null) return false;
        return mc.player.input.getMoveVector().x != 0.0f || mc.player.input.getMoveVector().y != 0.0f;
    }

    public static double getDirection() {
        if (mc.player == null) return 0;
        return getDirection(mc.player.getYRot(), mc.player.input.getMoveVector().y, mc.player.input.getMoveVector().x);
    }

    public static double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static double getSpeed() {
        if (mc.player == null) return 0;

        double x = mc.player.getDeltaMovement().x;
        double z = mc.player.getDeltaMovement().z;

        return Math.sqrt(x * x + z * z);
    }

    public static void fixMovement(MoveInputEvent event, float yaw) {
        if (mc.player == null) return;

        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0.0F && strafe == 0.0F) return;

        float delta = Mth.wrapDegrees(mc.player.getYRot() - yaw);
        float radians = delta * ((float) Math.PI / 180.0F);
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);

        float rotatedStrafe = strafe * cos - forward * sin;
        float rotatedForward = forward * cos + strafe * sin;

        event.setStrafe(Math.round(rotatedStrafe));
        event.setForward(Math.round(rotatedForward));
    }

    public static Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
            float f = Mth.sin(yaw * ((float) Math.PI / 180F));
            float g = Mth.cos(yaw * ((float) Math.PI / 180F));
            return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
        }
    }

    private MovementUtil() {
    }

}
