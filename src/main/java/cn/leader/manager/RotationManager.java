package cn.leader.manager;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.*;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.util.IMinecraft;
import cn.leader.util.player.MovementUtil;
import net.minecraft.util.Mth;

public class RotationManager implements IMinecraft {

    public static float[] currentRotations = {0.0F, 0.0F};
    public static float[] lastRotations = {0.0F, 0.0F};

    public static float[] targetRotations = {0.0F, 0.0F};

    public static float[] serverRotations = {0.0F, 0.0F};

    private static final float INSTANT = 180.0F;

    private static MovementCorrection correctMovement = MovementCorrection.None;
    private static float rotationSpeed = INSTANT;
    private static int requestTicks;
    private static Object owner;

    private static float sentYaw;
    private static boolean sentYawReady;

    public RotationManager() {
        instance.getEventManager().register(this);
    }

    /**
     * Vanilla never wraps the yaw it sends (Entity.setYRot only sanitises non-finite values), so a
     * normal player's yaw drifts past +-360 while spinning. Wrapping into [-180, 180] instead makes
     * every +-180 boundary crossing look like a ~360 degree snap in the packet stream, which is what
     * Grim's AimModulo360 flags (raw deltaYaw > 320 with a small previous delta).
     * <p>
     * Accumulating the shortest path delta keeps the sent yaw continuous.
     */
    public static float toSentYaw(float target) {
        if (!sentYawReady) {
            sentYaw = target;
            sentYawReady = true;
            return sentYaw;
        }

        sentYaw += Mth.wrapDegrees(target - sentYaw);
        return sentYaw;
    }

    public static float getSentYaw() {
        return sentYaw;
    }

    public static void resetSentYaw() {
        sentYaw = 0.0F;
        sentYawReady = false;
    }

    public void request(float[] rotations, float speedPerTick, MovementCorrection correction) {
        request(null, rotations, speedPerTick, correction);
    }

    public void request(Object requester, float[] rotations, float speedPerTick, MovementCorrection correction) {
        if (rotations == null) return;

        owner = requester;
        targetRotations = new float[]{Mth.wrapDegrees(rotations[0]), Mth.clamp(rotations[1], -90.0F, 90.0F)};
        rotationSpeed = speedPerTick <= 0.0F ? INSTANT : Math.min(speedPerTick, INSTANT);
        correctMovement = correction;
        requestTicks = 2;
    }

    public void snap(Object requester, float[] rotations, MovementCorrection correction) {
        request(requester, rotations, INSTANT, correction);
    }

    public void clearRequest() {
        requestTicks = 0;
        owner = null;
        correctMovement = MovementCorrection.None;
    }

    public void clearRequest(Object requester) {
        if (requester != null && owner != null && owner != requester) return;
        clearRequest();
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        resetSentYaw();
    }

    public boolean isActive() {
        return requestTicks > 0 && mc.player != null;
    }

    public boolean isInstant() {
        return rotationSpeed >= INSTANT;
    }

    public float[] getServerRotation() {
        return serverRotations;
    }

    @EventTarget
    @EventPriority(999)
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        float[] previous = currentRotations;

        if (requestTicks > 0) {
            currentRotations = step(currentRotations, targetRotations, rotationSpeed);
        } else {
            currentRotations = new float[]{mc.player.getYRot(), mc.player.getXRot()};
            correctMovement = MovementCorrection.None;
        }

        lastRotations = previous;
        requestTicks = Math.max(0, requestTicks - 1);
    }

    private static float[] step(float[] from, float[] to, float maxStep) {
        if (maxStep >= INSTANT) {
            return new float[]{Mth.wrapDegrees(to[0]), to[1]};
        }

        float yawDelta = Mth.wrapDegrees(to[0] - from[0]);
        float pitchDelta = to[1] - from[1];

        float yaw = from[0] + Mth.clamp(yawDelta, -maxStep, maxStep);
        float pitch = from[1] + Mth.clamp(pitchDelta, -maxStep, maxStep);

        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    @EventTarget
    @EventPriority(999)
    public void onLook(LookEvent event) {
        if (mc.player == null || !isActive()) return;

        event.setRotation(currentRotations);
        event.setLastRotation(lastRotations);
    }

    @EventTarget
    @EventPriority(999)
    public void onRotation(RotationEvent event) {
        if (mc.player == null || !isActive()) return;

        event.setRotation(currentRotations);
        event.setLastRotation(lastRotations);
    }

    @EventTarget
    @EventPriority(999)
    public void onStrafe(StrafeEvent event) {
        if (mc.player == null || !isActive() || correctMovement == MovementCorrection.None) return;

        event.setYaw(currentRotations[0]);
    }

    @EventTarget
    @EventPriority(999)
    public void onJump(JumpEvent event) {
        if (mc.player == null || !isActive() || correctMovement == MovementCorrection.None) return;

        event.setYaw(currentRotations[0]);
    }

    @EventTarget
    @EventPriority(999)
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null || !isActive() || correctMovement != MovementCorrection.Silent) return;

        MovementUtil.fixMovement(event, currentRotations[0]);
    }

    @EventTarget
    @EventPriority(999)
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (!event.isPre()) {
            serverRotations = new float[]{event.getYRot(), event.getXRot()};
            return;
        }

        float targetYaw = isActive() ? currentRotations[0] : event.getYRot();
        event.setYRot(toSentYaw(targetYaw));

        if (isActive()) {
            event.setXRot(currentRotations[1]);
        }
    }
}
