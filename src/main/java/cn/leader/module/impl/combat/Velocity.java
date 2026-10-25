package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.player.MovementUtil;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Knockback control, built around one rule: do not lie about our velocity.
 *
 * <p>Velocity packets are the server's own projection of where we should end up. Cancelling or
 * scaling them leaves the client's position short of that projection, and any anticheat that
 * simulates movement (Grim included) measures exactly that gap. The honest way to take less
 * knockback is to fight it with the only thing the client is allowed to choose freely: input.</p>
 *
 * <ul>
 *   <li>{@code Grim} / {@code Legit}: never touch the velocity packet. For a few ticks after a
 *       hit we aim the movement input against the knockback vector, which genuinely eats part of
 *       the displacement because the server simulates our input too. {@code Grim} adds a jump
 *       on the first grounded tick (the classic jump reset), {@code Legit} only counter-strafes.</li>
 *   <li>{@code Packet}: the old blunt mode, kept for servers without movement simulation. It
 *       cancels the packet and writes our own velocity, which is by definition a desync.</li>
 * </ul>
 *
 * @author DSJ
 */
@Getter
public class Velocity extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Grim", "Grim", "Legit", "Packet");

    private final BoolValue counterInput = new BoolValue("Counter Input", true, () -> !mode.is("Packet"));
    private final BoolValue jumpReset = new BoolValue("Jump Reset", true, () -> mode.is("Grim"));
    private final NumberValue window = new NumberValue("Window", 6, 2, 12, 1, () -> !mode.is("Packet"));

    private final NumberValue reduction = new NumberValue("Reduction", 100, 0, 100, 5, () -> mode.is("Packet"));
    private final BoolValue debug = new BoolValue("Debug", false);

    private Vec3 knockback = Vec3.ZERO;
    private int ticksLeft;
    private boolean jumped;
    private double lastStrength;

    public Velocity() {
        super("Velocity", Category.Combat);
        setDescription("Fight knockback with input instead of desyncing velocity.");
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        knockback = Vec3.ZERO;
        ticksLeft = 0;
        jumped = false;
        lastStrength = 0.0D;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        if (event.getType() != PacketEvent.Type.Received) return;
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket packet)) return;

        if (packet.getEntityId() != mc.player.getId()) return;

        Vec3 velocity = new Vec3(
                packet.getVelocityX() / 8000.0D,
                packet.getVelocityY() / 8000.0D,
                packet.getVelocityZ() / 8000.0D
        );

        double horizontal = Math.hypot(velocity.x, velocity.z);

        // Servers (and vanilla itself) love sending velocity packets that do nothing: pure
        // downward pushes, damage jitter, riding corrections. Only horizontal pushes matter here.
        if (horizontal < 0.02D) return;
        if (mc.player.isClimbing() || mc.player.isTouchingWater()) return;

        if (mode.is("Packet")) {
            double scale = 1.0D - Mth.clamp(reduction.getValue() / 100.0D, 0.0D, 1.0D);
            mc.player.setVelocity(velocity.x * scale, velocity.y, velocity.z * scale);
            event.setCancelled(true);
            return;
        }

        knockback = new Vec3(velocity.x, 0.0D, velocity.z);
        lastStrength = horizontal;
        jumped = false;

        // A stronger hit keeps us busy for longer; a light tap is over in two ticks.
        int derived = (int) Math.ceil(horizontal / 0.12D);
        ticksLeft = Mth.clamp(derived, 2, window.getValue().intValue());

        setSuffix(String.format("%s %.2f", mode.getValue(), horizontal));
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null || mode.is("Packet")) return;

        if (knockback == Vec3.ZERO || ticksLeft <= 0) {
            setSuffix(mode.getValue());
            return;
        }

        if (jumpReset.getValue() && !jumped && mc.player.isOnGround()) {
            event.setJumping(true);
            jumped = true;
        }

        if (counterInput.getValue()) {
            float[] input = counterStrafe();
            if (input != null) {
                event.setForward(input[0]);
                event.setStrafe(input[1]);
            }
        }

        if (debug.getValue()) {
            setSuffix(String.format("%s %d/%.2f", mode.getValue(), ticksLeft, lastStrength));
        }

        ticksLeft--;

        if (ticksLeft <= 0) {
            knockback = Vec3.ZERO;
            setSuffix(mode.getValue());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (knockback == Vec3.ZERO) setSuffix(mode.getValue());
    }

    /**
     * Pick the 8-way input whose world direction points most directly against the knockback.
     *
     * <p>Inputs are expressed in camera space: that is the frame the player's keys live in and
     * the frame the silent-rotation movement fix rewrites from. Computing the counter direction
     * in server space instead would fight the fix and send us sideways.</p>
     */
    private float[] counterStrafe() {
        Vec3 away = knockback.normalize().multiply(-1.0D);
        if (away.lengthSquared() < 1.0E-6D) return null;

        double wantedYaw = Math.toDegrees(Math.atan2(-away.x, away.z));
        float referenceYaw = mc.player.getYaw();

        float bestForward = 0.0F;
        float bestStrafe = 0.0F;
        double bestDelta = Double.MAX_VALUE;

        for (int forward = -1; forward <= 1; forward++) {
            for (int strafe = -1; strafe <= 1; strafe++) {
                if (forward == 0 && strafe == 0) continue;

                double candidate = Math.toDegrees(MovementUtil.getDirection(referenceYaw, forward, strafe));
                double delta = Math.abs(Mth.wrapDegrees(candidate - wantedYaw));

                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestForward = forward;
                    bestStrafe = strafe;
                }
            }
        }

        return new float[]{bestForward, bestStrafe};
    }

    public boolean isMitigating() {
        return ticksLeft > 0;
    }

    public double getLastStrength() {
        return lastStrength;
    }
}
