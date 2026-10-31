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
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Velocity (anti knockback).
 *
 * <p>Two plain techniques, both taken from LiquidBounce's default set:</p>
 * <ul>
 *     <li>{@code Modify} - swallow the knockback packet and apply a scaled velocity ourselves.</li>
 *     <li>{@code Jump Reset} - let the knockback through, but jump on the first tick so the arc is
 *     cut short.</li>
 * </ul>
 *
 * <p>No packet buffering, no transaction games.</p>
 */
public class Velocity extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Modify", "Modify", "Jump Reset", "Off");

    private final NumberValue horizontal = new NumberValue("Horizontal", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue vertical = new NumberValue("Vertical", 0, 0, 100, 1,
            () -> this.mode.is("Modify"));
    private final NumberValue delay = new NumberValue("Delay", 0, 0, 20, 1,
            () -> this.mode.is("Modify"));

    private final NumberValue chance = new NumberValue("Chance", 100, 0, 100, 1,
            () -> !this.mode.is("Off"));
    private final NumberValue pauseOnFlag = new NumberValue("Pause On Flag", 0, 0, 20, 1);
    private final BoolValue onlySprinting = new BoolValue("Only Sprinting", true,
            () -> this.mode.is("Jump Reset"));

    private Vec3 pending;
    private int pendingTicks;
    private int pause;
    private boolean jumpPending;

    public Velocity() {
        super("Velocity", Category.Combat);
        setDescription("Reduces knockback (Modify / Jump Reset)");
    }

    @Override
    public void onEnable() {
        this.pending = null;
        this.pendingTicks = 0;
        this.pause = 0;
        this.jumpPending = false;
    }

    @Override
    public void onDisable() {
        this.pending = null;
        this.pendingTicks = 0;
        this.jumpPending = false;
    }

    // ------------------------------------------------------------------ packets

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.Received) return;
        if (mc.player == null) return;

        // A server teleport means our velocity is meaningless; stop touching things for a moment.
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.pause = this.pauseOnFlag.getValue().intValue();
            return;
        }

        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket motion)) return;
        if (motion.id() != mc.player.getId()) return;
        if (this.pause > 0 || this.mode.is("Off")) return;
        if (!rollChance()) return;

        switch (this.mode.getValue()) {
            case "Modify" -> {
                event.setCancelled(true);

                Vec3 movement = motion.movement();
                Vec3 scaled = new Vec3(
                        movement.x * percent(this.horizontal),
                        movement.y * percent(this.vertical),
                        movement.z * percent(this.horizontal));

                int wait = this.delay.getValue().intValue();
                if (wait <= 0) {
                    apply(scaled);
                } else {
                    this.pending = scaled;
                    this.pendingTicks = wait;
                }
            }
            case "Jump Reset" -> {
                // Let the knockback land, then jump so the arc is cancelled by the input.
                if (!mc.player.onGround()) return;
                if (this.onlySprinting.getValue() && !mc.player.isSprinting()) return;

                this.jumpPending = true;
            }
            default -> {
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.pause > 0) this.pause--;

        if (this.pending != null) {
            if (--this.pendingTicks <= 0) {
                apply(this.pending);
                this.pending = null;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.jumpPending) return;

        this.jumpPending = false;
        event.setJumping(true);
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Vec3 velocity) {
        if (mc.player == null) return;
        mc.player.setDeltaMovement(velocity);
    }

    private static double percent(NumberValue value) {
        return value.getValue().doubleValue() / 100.0D;
    }

    private boolean rollChance() {
        float value = this.chance.getValue();
        return value >= 100.0F || ThreadLocalRandom.current().nextFloat() * 100.0F < value;
    }
}
