package cn.leader.manager;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.*;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.util.IMinecraft;
import cn.leader.util.player.AimUtil;
import cn.leader.util.player.MovementUtil;

/**
 * Rotation Manager
 *
 * <p>Modules never talk to each other about rotations: they push a short lived request
 * ({@link #request(float[], float, MovementCorrection)}) and this manager owns smoothing,
 * movement correction and the bookkeeping of what the server currently believes our look
 * direction is.</p>
 *
 * @author DSJ
 */
public class RotationManager implements IMinecraft {

    /** Rotation the player actually sees (camera). */
    public static float[] currentRotations = {0.0F, 0.0F};
    public static float[] lastRotations = {0.0F, 0.0F};
    /** Rotation the current request aims at. */
    public static float[] targetRotations = {0.0F, 0.0F};
    /** Last rotation written into a movement packet, i.e. what the server knows about us. */
    public static float[] serverRotations = {0.0F, 0.0F};

    private static MovementCorrection correctMovement = MovementCorrection.None;
    private static float rotationSpeed = 180.0F;
    private static int requestTicks;

    public RotationManager() {
        instance.getEventManager().register(this);
    }

    /**
     * Ask for the given rotation for the next couple of ticks. The owner refreshes the request
     * every tick, so it expires by itself once the module stops asking.
     */
    public void request(float[] rotations, float speedPerTick, MovementCorrection correction) {
        if (rotations == null) return;

        targetRotations = new float[]{rotations[0], rotations[1]};
        rotationSpeed = speedPerTick;
        correctMovement = correction;
        requestTicks = 2;
    }

    public void clearRequest() {
        requestTicks = 0;
    }

    public boolean isActive() {
        return requestTicks > 0 && mc.player != null;
    }

    /** Rotation the server is currently aware of; hits are validated against this. */
    public float[] getServerRotation() {
        return serverRotations;
    }

    @EventTarget
    @EventPriority(999)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.player == null) return;

        float[] previous = currentRotations;

        if (requestTicks > 0) {
            currentRotations = AimUtil.step(currentRotations, targetRotations, rotationSpeed);
        } else {
            currentRotations = new float[]{mc.player.getYaw(), mc.player.getPitch()};
            correctMovement = MovementCorrection.None;
        }

        lastRotations = previous;
        requestTicks = Math.max(0, requestTicks - 1);

        mc.gameRenderer.updateCrosshairTarget(1.0F);
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
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (!event.isPre()) {
            // Record what the packet just carried, i.e. what the server now believes.
            serverRotations = isActive()
                    ? new float[]{currentRotations[0], currentRotations[1]}
                    : new float[]{event.getYaw(), event.getPitch()};
            return;
        }

        if (!isActive()) return;

        event.setYaw(currentRotations[0]);
        event.setPitch(currentRotations[1]);
    }

    @EventTarget
    @EventPriority(999)
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null || !isActive() || correctMovement != MovementCorrection.Silent) return;

        MovementUtil.fixMovement(event, currentRotations[0]);
    }

    @EventTarget
    @EventPriority(999)
    public void onRotation(RotationEvent event) {
        if (mc.player == null || !isActive()) return;

        event.setRotation(currentRotations);
        event.setLastRotation(lastRotations);
    }
}
