package cn.leader.module.impl.combat;

import cn.leader.Client;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.event.impl.WorldEvent;
import cn.leader.manager.RotationManager;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.movement.Scaffold;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.network.IncomingPacketGate;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Velocity extends Module implements IncomingPacketGate.Interceptor {

    private final ModeValue mode = new ModeValue("Mode", "Modify", "Modify", "Jump Reset", "Hypixel", "Delay");

    private final NumberValue horizontal = new NumberValue("Horizontal", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue vertical = new NumberValue("Vertical", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue delayApply = new NumberValue("Delay", 0, 0, 20, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue chance = new NumberValue("Chance", 100, 0, 100, 1,
            () -> this.mode.is("Modify"));

    private final NumberValue pauseOnFlag = new NumberValue("Pause On Flag", 0, 0, 20, 1);
    private final BoolValue onlySprinting = new BoolValue("Only Sprinting", true,
            () -> this.mode.is("Jump Reset"));

    private final BoolValue delay = new BoolValue("Delay", true, () -> this.mode.is("Hypixel"));
    private final NumberValue delayTicks = new NumberValue("DelayTicks", 3, 0, 20, 1,
            () -> this.mode.is("Hypixel") && delay.getValue());
    private final BoolValue pushForward = new BoolValue("Push Forward", false,
            () -> this.mode.is("Hypixel"));
    private final NumberValue pushTicks = new NumberValue("Push Ticks", 3, 1, 5, 1,
            () -> this.mode.is("Hypixel") && pushForward.getValue());
    private final BoolValue extraAttack = new BoolValue("Extra Attack", false,
            () -> this.mode.is("Hypixel"));
    private final NumberValue attackCounts = new NumberValue("Attack Counts", 1, 1, 5, 1,
            () -> this.mode.is("Hypixel") && extraAttack.getValue());

    private final NumberValue delayModeTicks = new NumberValue("Delay Ticks", 3, 1, 5, 1,
            () -> this.mode.is("Delay"));
    private final BoolValue delayModeJump = new BoolValue("Jump Reset", false,
            () -> this.mode.is("Delay"));
    private final BoolValue releaseOnGround = new BoolValue("Release On Ground", true,
            () -> this.mode.is("Delay") || this.mode.is("Hypixel"));

    private final ArrayDeque<Packet<ClientGamePacketListener>> heldPackets = new ArrayDeque<>();
    private final Object holdLock = new Object();

    private volatile Vec3 pendingVelocity;
    private volatile int pendingTicks;
    private volatile int pause;

    private boolean holding;
    private long holdUntil;
    private long lagTime;
    private float knockbackYaw;
    private int attackQueue;
    private int sprintQueue;
    private boolean jumpPending;
    private boolean replaying;

    public Velocity() {
        super("Velocity", Category.Combat);
        setDescription("Reduces knockback");
    }

    @Override
    public void onEnable() {
        clearHold();
        this.pendingVelocity = null;
        this.pendingTicks = 0;
        this.pause = 0;
        this.jumpPending = false;
        this.replaying = false;
        IncomingPacketGate.add(this);
    }

    @Override
    public void onDisable() {
        IncomingPacketGate.remove(this);
        releaseHeld();
        this.pendingVelocity = null;
        this.pendingTicks = 0;
        this.jumpPending = false;
        this.replaying = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        if (this.replaying) return;

        if (event.getType() != PacketEvent.Type.Received) return;
        if (!this.mode.is("Jump Reset")) return;
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket motion)) return;
        if (motion.id() != mc.player.getId()) return;
        if (this.pause > 0) return;
        if (!mc.player.onGround()) return;
        if (this.onlySprinting.getValue() && !mc.player.isSprinting()) return;

        this.jumpPending = true;
    }

    @Override
    public boolean shouldHold(Packet<?> packet) {
        if (mc.player == null || mc.player.isDeadOrDying()) return false;

        if (packet instanceof ClientboundPlayerPositionPacket) {
            this.pause = this.pauseOnFlag.getValue().intValue();
            this.lagTime = System.currentTimeMillis();
        }
        if (this.pause > 0) return false;

        if (this.mode.is("Delay")) return holdDelay(packet);
        if (this.mode.is("Hypixel")) return holdBuffer(packet);
        if (this.mode.is("Modify")) return dropModified(packet);
        return false;
    }

    private boolean holdDelay(Packet<?> packet) {
        synchronized (this.holdLock) {
            if (isSessionReset(packet)) {
                clearHold();
                return false;
            }

            if (this.holding) {
                if (isPositionReset(packet) && this.pauseOnFlag.getValue().intValue() > 0) {
                    releaseNextTick();
                } else if (isKnockback(packet)) {
                    startHold(this.delayModeTicks.getValue().intValue());
                }
                return hold(packet);
            }

            if (isKnockback(packet)) {
                startHold(this.delayModeTicks.getValue().intValue());
                return hold(packet);
            }
            return false;
        }
    }

    private boolean holdBuffer(Packet<?> packet) {
        synchronized (this.holdLock) {
            if (isSessionReset(packet)) {
                clearHold();
                return false;
            }

            if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) {
                this.lagTime = System.currentTimeMillis();
            }

            if (this.holding) {
                if (isPositionReset(packet) && this.pauseOnFlag.getValue().intValue() > 0) {
                    releaseNextTick();
                }
                return hold(packet);
            }

            if (isKnockback(packet)) {
                if (packet instanceof ClientboundSetEntityMotionPacket motion) {
                    Vec3 movement = motion.movement();
                    this.knockbackYaw = Mth.wrapDegrees(
                            (float) Math.toDegrees(Math.atan2(movement.z, movement.x)) + 90.0F);
                }
                startHold(this.delay.getValue() ? this.delayTicks.getValue().intValue() : 0);
                return hold(packet);
            }
            return false;
        }
    }

    private boolean dropModified(Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket motion)) return false;
        if (motion.id() != mc.player.getId()) return false;
        if (!rollChance()) return false;

        Vec3 movement = motion.movement();
        this.pendingVelocity = new Vec3(
                movement.x * percent(this.horizontal),
                movement.y * percent(this.vertical),
                movement.z * percent(this.horizontal));
        this.pendingTicks = Math.max(1, this.delayApply.getValue().intValue());
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean hold(Packet<?> packet) {
        this.heldPackets.add((Packet<ClientGamePacketListener>) packet);
        if (this.heldPackets.size() > 256) {
            releaseNextTick();
        }
        return true;
    }

    private void startHold(int ticks) {
        long deadline = System.currentTimeMillis() + Math.max(0, ticks) * 50L;
        this.holding = true;
        if (deadline > this.holdUntil) {
            this.holdUntil = deadline;
        }
    }

    private void releaseNextTick() {
        this.holdUntil = System.currentTimeMillis();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (this.pause > 0) this.pause--;

        releaseHold();

        if (this.mode.is("Hypixel")) {
            processExtraAttack();
            applyPushRotation();
        }

        if (this.pendingVelocity != null) {
            if (--this.pendingTicks <= 0) {
                apply(this.pendingVelocity);
                this.pendingVelocity = null;
            }
        }
    }

    private void releaseHold() {
        List<Packet<ClientGamePacketListener>> release = null;
        boolean jump = false;
        int push = 0;
        int attacks = 0;
        long now = System.currentTimeMillis();

        synchronized (this.holdLock) {
            if (!this.holding) return;

            boolean grounded = this.releaseOnGround.getValue() && mc.player.onGround();
            if (!grounded && now < this.holdUntil) return;

            this.holding = false;
            if (!this.heldPackets.isEmpty()) {
                release = new ArrayList<>(this.heldPackets);
                this.heldPackets.clear();
            }
            jump = this.mode.is("Hypixel")
                    ? mc.player.onGround()
                    : this.delayModeJump.getValue();

            if (this.mode.is("Hypixel") && now - this.lagTime >= 100L) {
                if (this.pushForward.getValue()) push = this.pushTicks.getValue().intValue();
                if (this.extraAttack.getValue()) attacks = this.attackCounts.getValue().intValue();
            }
        }

        if (release != null) replay(release);
        if (jump) this.jumpPending = true;
        if (push > 0) this.sprintQueue = push;
        if (attacks > 0) this.attackQueue = attacks;
    }

    private void processExtraAttack() {
        if (this.attackQueue < 1) return;

        LivingEntity target = killAuraTarget();
        if (target != null && mc.player.distanceTo(target) <= 3.0F) {
            if (mc.gameMode != null) {
                mc.gameMode.attack(mc.player, target);
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        this.attackQueue--;
    }

    private void applyPushRotation() {
        if (this.sprintQueue < 1) return;
        if (killAuraTarget() != null) return;
        if (isScaffoldEnabled()) return;

        instance.getRotationManager().request(this,
                new float[]{this.knockbackYaw, RotationManager.currentRotations[1]},
                180.0F, MovementCorrection.Strict);
    }


    private void releaseHeld() {
        List<Packet<ClientGamePacketListener>> release = null;

        synchronized (this.holdLock) {
            this.holding = false;
            if (!this.heldPackets.isEmpty()) {
                release = new ArrayList<>(this.heldPackets);
                this.heldPackets.clear();
            }
        }

        if (release != null) replay(release);
    }

    private void clearHold() {
        synchronized (this.holdLock) {
            this.heldPackets.clear();
            this.holding = false;
            this.holdUntil = 0L;
            this.lagTime = 0L;
            this.knockbackYaw = 0.0F;
            this.attackQueue = 0;
            this.sprintQueue = 0;
        }
    }

    private void replay(List<Packet<ClientGamePacketListener>> packets) {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;

        this.replaying = true;
        try {
            for (Packet<ClientGamePacketListener> packet : packets) {
                try {
                    packet.handle(connection);
                } catch (Exception exception) {
                    Client.logger.debug("Failed to replay {}: {}",
                            packet.getClass().getSimpleName(), exception.getMessage());
                }
            }
        } finally {
            this.replaying = false;
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        clearHold();
        this.pendingVelocity = null;
        this.pendingTicks = 0;
        this.jumpPending = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) return;

        if (this.mode.is("Hypixel")) {
            bufferInput(event);
            return;
        }

        if (!this.jumpPending) return;

        this.jumpPending = false;
        if (!mc.player.onGround()) return;
        if (this.mode.is("Delay") && !mc.player.isSprinting()) return;

        event.setJumping(true);
    }

    private void bufferInput(MoveInputEvent event) {
        boolean settled = System.currentTimeMillis() - this.lagTime >= 100L;

        if (this.jumpPending) {
            this.jumpPending = false;
            if (mc.player.onGround()) {
                event.setJumping(true);
                if (this.pushForward.getValue() && settled) {
                    event.setForward(1.0F);
                    event.setStrafe(0.0F);
                }
            }
        }

        if (this.sprintQueue > 0) {
            if (killAuraTarget() == null && !isScaffoldEnabled() && settled) {
                event.setForward(1.0F);
                event.setStrafe(0.0F);
            }
            this.sprintQueue--;
        }
    }

    private void apply(Vec3 velocity) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(velocity);
    }

    private LivingEntity killAuraTarget() {
        KillAura aura = getModule(KillAura.class);
        return aura != null && aura.isEnabled() ? aura.getTarget() : null;
    }

    private boolean isScaffoldEnabled() {
        Scaffold scaffold = getModule(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    private boolean isKnockback(Packet<?> packet) {
        if (mc.player == null) return false;

        if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            return motion.id() == mc.player.getId() && motion.movement().lengthSqr() > 0.0D;
        }
        if (packet instanceof ClientboundExplodePacket explode) {
            return explode.playerKnockback().isPresent();
        }
        return false;
    }

    private boolean isPositionReset(Packet<?> packet) {
        return packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundRespawnPacket
                || packet instanceof ClientboundLoginPacket;
    }

    private boolean isSessionReset(Packet<?> packet) {
        return packet instanceof ClientboundDisconnectPacket
                || packet instanceof ClientboundStartConfigurationPacket;
    }

    private static double percent(NumberValue value) {
        return value.getValue().doubleValue() / 100.0D;
    }

    private boolean rollChance() {
        float value = this.chance.getValue();
        return value >= 100.0F || ThreadLocalRandom.current().nextFloat() * 100.0F < value;
    }
}
