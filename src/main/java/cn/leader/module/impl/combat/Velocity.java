package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.player.MovementUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Knockback control.
 *
 * <ul>
 *   <li>{@code Hypixel}: hold the knockback (and every packet queued behind it) while we are still
 *       in the air, then release once we land or the buffer runs out. The server keeps us at the old
 *       position for those ticks, so the push is spent against a position we already corrected
 *       instead of stacking on top of our movement. A jump on the landing tick and an optional
 *       counter hit while sprinting finish the job.</li>
 *   <li>{@code Legit}: never touches packets, fights the push with movement input only.</li>
 *   <li>{@code Packet}: blunt velocity rewrite, for servers without movement simulation.</li>
 * </ul>
 *
 * @author DSJ
 */
public class Velocity extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Hypixel", "Hypixel", "Legit", "Packet");

    // Hypixel
    private final BoolValue buffer = new BoolValue("Buffer", true, () -> mode.is("Hypixel"));
    private final NumberValue bufferTicks = new NumberValue("Buffer Ticks", 4, 0, 10, 1,
            () -> mode.is("Hypixel") && buffer.getValue());
    private final BoolValue jumpReset = new BoolValue("Jump Reset", true, () -> !mode.is("Packet"));
    private final BoolValue attackReduce = new BoolValue("Attack Reduce", false, () -> mode.is("Hypixel"));
    private final BoolValue counterYaw = new BoolValue("Counter Yaw", false, () -> mode.is("Hypixel"));
    private final BoolValue onlyPlayers = new BoolValue("Only Players", true, () -> mode.is("Hypixel"));
    private final BoolValue airpush = new BoolValue("Air Push", false, () -> mode.is("Hypixel"));
    private final NumberValue airpushRange = new NumberValue("Air Push Range", 12, 6, 30, 1,
            () -> mode.is("Hypixel") && airpush.getValue());

    // Legit
    private final BoolValue counterInput = new BoolValue("Counter Input", true, () -> mode.is("Legit"));
    private final NumberValue window = new NumberValue("Window", 6, 2, 12, 1, () -> mode.is("Legit"));

    // Packet
    private final NumberValue reduction = new NumberValue("Reduction", 100, 0, 100, 5, () -> mode.is("Packet"));

    private final BoolValue debug = new BoolValue("Debug", false);

    private final Deque<Packet<?>> held = new ArrayDeque<>();

    private Vec3 knockback = Vec3.ZERO;
    private double lastStrength;
    private float counterYawTarget;

    /** Ticks left in which knockback is being fought; also drives the jump reset. */
    private int ticksLeft;
    /** -1 when not buffering, otherwise how many ticks packets have been held. */
    private int holdAge = -1;

    private boolean armed;
    private boolean jumped;
    private boolean reducePending;
    private boolean airpushPending;
    private int windowTotal;

    public Velocity() {
        super("Velocity", Category.Combat);
        setDescription("Reduce knockback (Hypixel buffer / legit strafe)");
    }

    @Override
    public void onEnable() {
        clear();
    }

    @Override
    public void onDisable() {
        release();
        clear();
    }

    private void clear() {
        knockback = Vec3.ZERO;
        lastStrength = 0.0D;
        ticksLeft = 0;
        holdAge = -1;
        armed = false;
        jumped = false;
        reducePending = false;
        airpushPending = false;
        held.clear();
    }

    // ------------------------------------------------------------------ packets

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (event.getType() != PacketEvent.Type.Received) return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motion && motion.id() == mc.player.getId()) {
            handleKnockback(event, motion.movement());
            return;
        }

        // Anything else that arrives while we hold the queue goes in behind the knockback, except
        // the handful of packets that must never be late.
        if (holdAge >= 0 && !deliverImmediately(event.getPacket())) {
            event.setCancelled(true);
            held.add(event.getPacket());
        }
    }

    private void handleKnockback(PacketEvent event, Vec3 velocity) {
        double horizontal = Math.hypot(velocity.x, velocity.z);

        // Downward pushes, jitter and riding corrections are not real knockback.
        if (horizontal < 0.1D || velocity.y <= 0.0D) return;
        if (mc.player.onClimbable() || mc.player.isInWater()) return;

        if (mode.is("Packet")) {
            double scale = 1.0D - Mth.clamp(reduction.getValue() / 100.0D, 0.0D, 1.0D);
            mc.player.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
            event.setCancelled(true);
            return;
        }

        knockback = new Vec3(velocity.x, 0.0D, velocity.z);
        lastStrength = horizontal;
        counterYawTarget = (float) Math.toDegrees(Math.atan2(velocity.x, -velocity.z));
        jumped = false;

        if (mode.is("Legit")) {
            ticksLeft = Mth.clamp((int) Math.ceil(horizontal / 0.12D), 2, window.getValue().intValue());
            setSuffix(String.format("Legit %.2f", horizontal));
            return;
        }

        ticksLeft = hitTicks(horizontal);
        windowTotal = ticksLeft;

        boolean sprinting = mc.player.isSprinting();
        boolean grounded = mc.player.onGround();
        boolean nearby = nearestTarget(8.0D) != null;

        if (sprinting) {
            // A sprinting hit is the one the server expects us to keep moving through, so release
            // at once and answer with a counter hit instead of buffering.
            armed = true;
            reducePending = attackReduce.getValue() && nearby;
        } else if (!nearby && airpush.getValue() && !grounded) {
            // Nothing in reach to fight back with: the push has to be answered by a hit on
            // something far away, which is what makes the server treat it as our own attack.
            airpushPending = true;
        } else if (buffer.getValue() && bufferTicks.getValue().intValue() > 0 && !grounded && nearby) {
            holdAge = 0;
            event.setCancelled(true);
            held.add(event.getPacket());
        } else {
            armed = true;
        }

        setSuffix(String.format("Hypixel %.2f", horizontal));
    }

    /**
     * The buffer is only worth holding while we are still airborne; the moment we land the server
     * reconciles our position anyway.
     */
    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPost() || !mode.is("Hypixel") || holdAge < 0) return;

        holdAge++;

        boolean timeout = holdAge >= bufferTicks.getValue().intValue();
        boolean landed = mc.player.onGround();
        boolean sprinting = mc.player.isSprinting();
        boolean nobodyLeft = nearestTarget(8.0D) == null;

        if (timeout || landed || sprinting || nobodyLeft) {
            release();
            armed = true;
        }
    }

    private void release() {
        if (holdAge < 0 && held.isEmpty()) {
            holdAge = -1;
            return;
        }

        holdAge = -1;

        while (!held.isEmpty()) {
            Packet<?> packet = held.poll();
            try {
                deliver(packet);
            } catch (Exception ignored) {
                // A packet that no longer applies after the delay is not worth a crash.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void deliver(Packet<?> packet) {
        ((Packet<ClientGamePacketListener>) packet).handle(mc.getConnection());
    }

    private boolean deliverImmediately(Packet<?> packet) {
        return packet instanceof ClientboundSetHealthPacket
                || packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundSoundPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ClientboundPlayerCombatKillPacket
                || packet instanceof ClientboundContainerClosePacket
                || packet instanceof ClientboundHurtAnimationPacket
                || packet instanceof ClientboundSetPlayerTeamPacket
                || (packet instanceof ClientboundAnimatePacket animate && animate.getId() != mc.player.getId());
    }

    // ------------------------------------------------------------------ ticks

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        // Leaving Hypixel while packets are held would strand them forever.
        if (!mode.is("Hypixel") && holdAge >= 0) {
            release();
        }

        if (mode.is("Hypixel")) {
            if (reducePending) {
                reducePending = false;
                counterAttack();
            }

            if (airpushPending) {
                airpushPending = false;
                airpush();
            }

            if (counterYaw.getValue() && armed && ticksLeft > 0) {
                requestCounterYaw();
            }
        }

        if (armed && --ticksLeft <= 0) {
            armed = false;
            jumped = false;
            knockback = Vec3.ZERO;
            setSuffix(mode.getValue());
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null || mode.is("Packet")) return;

        if (mode.is("Legit")) {
            if (knockback == Vec3.ZERO || ticksLeft <= 0) return;

            if (jumpReset.getValue() && !jumped && mc.player.onGround()) {
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
            return;
        }

        if (!armed) return;

        if (jumpReset.getValue() && !jumped && mc.player.onGround()) {
            event.setJumping(true);
            jumped = true;
        }

        // Only the opening ticks of the window need forward input; after that the player gets
        // their keys back.
        if (windowTotal - ticksLeft <= 2) {
            event.setForward(1.0F);
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * How many ticks the fight is worth: a plain sword hit lands around three, a knockback
     * enchantment goes to the ceiling.
     */
    private int hitTicks(double horizontal) {
        int derived = (int) Math.round(horizontal * 7.5D + 2.0D);
        return Mth.clamp(derived, 2, 10);
    }

    private void requestCounterYaw() {
        KillAura aura = instance.getModuleManager().getModule(KillAura.class);
        if (aura != null && aura.isEnabled() && aura.getTarget() != null) return;

        instance.getRotationManager().request(
                new float[]{counterYawTarget, mc.player.getXRot()}, 180.0F, MovementCorrection.Silent);
    }

    /**
     * Sprint reset: a hit delivered while sprinting cuts our own momentum on the server, which is
     * roughly the knockback we just took. Fired once, on the tick after the hit.
     */
    private void counterAttack() {
        if (mc.gameMode == null || mc.level == null) return;

        LivingEntity target = nearestTarget(3.0D);
        if (target == null) return;

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);

        Vec3 velocity = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(velocity.x * 0.6D, velocity.y, velocity.z * 0.6D);
        mc.player.setSprinting(false);
    }

    /**
     * Hits whatever is furthest away inside the configured reach. The hit itself will not land,
     * but the attack packet still cuts our own momentum the same way a sprint reset does.
     */
    private void airpush() {
        if (mc.gameMode == null || mc.level == null) return;

        LivingEntity target = farthestTarget(airpushRange.getValue().doubleValue(), 6.0D);
        if (target == null) return;

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);

        Vec3 velocity = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(velocity.x * 0.6D, velocity.y, velocity.z * 0.6D);
        mc.player.setSprinting(false);
    }

    private LivingEntity farthestTarget(double range, double minDistance) {
        if (mc.player == null || mc.level == null) return null;

        AABB area = mc.player.getBoundingBox().inflate(range);
        boolean playersOnly = onlyPlayers.getValue();

        LivingEntity best = null;
        double bestDistance = minDistance;

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, area, candidate -> {
            if (candidate == mc.player || !candidate.isAlive()) return false;
            return !playersOnly || candidate instanceof Player;
        })) {
            double distance = mc.player.distanceTo(entity);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }

        return best;
    }

    private LivingEntity nearestTarget(double range) {
        if (mc.player == null || mc.level == null) return null;

        AABB area = mc.player.getBoundingBox().inflate(range);
        boolean playersOnly = onlyPlayers.getValue();

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, area, candidate -> {
            if (candidate == mc.player || !candidate.isAlive()) return false;
            return !playersOnly || candidate instanceof Player;
        })) {
            double distance = mc.player.distanceTo(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }

        return best;
    }

    private float[] counterStrafe() {
        Vec3 away = knockback.normalize().scale(-1.0D);
        if (away.lengthSqr() < 1.0E-6D) return null;

        double wantedYaw = Math.toDegrees(Math.atan2(-away.x, away.z));
        float referenceYaw = mc.player.getYRot();

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
        return armed || holdAge >= 0;
    }

    public boolean isHolding() {
        return holdAge >= 0;
    }

    public int getHeldCount() {
        return held.size();
    }

    public double getLastStrength() {
        return lastStrength;
    }

    public Vec3 getKnockback() {
        return this.knockback;
    }

    public int getTicksLeft() {
        return this.ticksLeft;
    }

    public boolean isJumped() {
        return this.jumped;
    }
}
