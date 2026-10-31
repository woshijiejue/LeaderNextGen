package cn.leader.manager;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.*;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.util.IMinecraft;
import cn.leader.util.player.MovementUtil;
import net.minecraft.util.Mth;

/**
 * Rotation manager.
 *
 * <p>Deliberately simple: a module posts a target rotation, the manager walks towards it by at most
 * {@code speed} degrees per tick and writes the result into the outgoing packets. There is no mouse
 * GCD quantisation, no jitter, no "humanised" curve - just linear interpolation with an instant
 * option ({@code speed >= 180}).</p>
 *
 * <p>Movement correction is applied on the input and jump vectors so that a silent rotation does not
 * push the player in the wrong direction.</p>
 */
public class RotationManager implements IMinecraft {

    /** Rotation the player actually sees (the camera is untouched, this is our internal value). */
    public static float[] currentRotations = {0.0F, 0.0F};
    public static float[] lastRotations = {0.0F, 0.0F};
    /** Where the active request wants to end up. */
    public static float[] targetRotations = {0.0F, 0.0F};
    /** Last rotation written into a movement packet, i.e. what the server knows about us. */
    public static float[] serverRotations = {0.0F, 0.0F};

    private static final float INSTANT = 180.0F;

    private static MovementCorrection correctMovement = MovementCorrection.None;
    private static float rotationSpeed = INSTANT;
    private static int requestTicks;
    private static Object owner;

    public RotationManager() {
        instance.getEventManager().register(this);
    }

    // ------------------------------------------------------------------ requests

    /** Ask for a rotation for the next two ticks; refresh every tick to keep it alive. */
    public void request(float[] rotations, float speedPerTick, MovementCorrection correction) {
        request(null, rotations, speedPerTick, correction);
    }

    /**
     * Same, but tagged with the requesting module. A tagged request can only be cleared by its owner,
     * so an idle module cannot wipe somebody else's aim.
     */
    public void request(Object requester, float[] rotations, float speedPerTick, MovementCorrection correction) {
        if (rotations == null) return;

        owner = requester;
        targetRotations = new float[]{Mth.wrapDegrees(rotations[0]), Mth.clamp(rotations[1], -90.0F, 90.0F)};
        rotationSpeed = speedPerTick <= 0.0F ? INSTANT : Math.min(speedPerTick, INSTANT);
        correctMovement = correction;
        requestTicks = 2;
    }

    /** Snap to the rotation on the next tick. */
    public void snap(Object requester, float[] rotations, MovementCorrection correction) {
        request(requester, rotations, INSTANT, correction);
    }

    public void clearRequest() {
        requestTicks = 0;
        owner = null;
        correctMovement = MovementCorrection.None;
    }

    /** Untag and drop the request only if the caller is the current owner. */
    public void clearRequest(Object requester) {
        if (requester != null && owner != null && owner != requester) return;
        clearRequest();
    }

    public boolean isActive() {
        return requestTicks > 0 && mc.player != null;
    }

    public boolean isInstant() {
        return rotationSpeed >= INSTANT;
    }

    /** Rotation the server currently believes we are looking at. */
    public float[] getServerRotation() {
        return serverRotations;
    }

    // ------------------------------------------------------------------ stepping

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

    /** Linear interpolation, clamped to {@code maxStep} degrees on both axes. */
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

    // ------------------------------------------------------------------ hooks

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
            serverRotations = isActive()
                    ? new float[]{currentRotations[0], currentRotations[1]}
                    : new float[]{event.getYRot(), event.getXRot()};
            return;
        }

        if (!isActive()) return;

        event.setYRot(currentRotations[0]);
        event.setXRot(currentRotations[1]);
    }
}
