package cn.leader.module.impl.combat;
import cn.leader.Client;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.RotationManager;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.network.BlinkUtils;
import cn.leader.util.network.PacketUtil;
import cn.leader.util.player.AimUtil;
import cn.leader.util.player.EntityUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * KillAura, second generation.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li><b>Aim before damage.</b> A hit is only fired from {@link MotionEvent} post, i.e. after
 *       the movement packet carrying this tick's look direction has left the client. The target
 *       is the entity our own ray actually intersects with that rotation, so the server never
 *       sees an attack that disagrees with the look it has on file.</li>
 *   <li><b>Targets are scored, not filtered.</b> Distance, remaining health and the angular
 *       cost of turning to the target are blended, then the current target keeps a bonus until
 *       it is no longer attackable. That kills both the flicker between two equally good
 *       targets and the "two entities in one tick" packet pattern.</li>
 *   <li><b>Clicking is a schedule, not a timer.</b> Intervals come from a per-second CPS pick
 *       with gaussian jitter and the occasional human pause, so the packet rhythm never
 *       degenerates into a metronome.</li>
 * </ul>
 *
 * @author DSJ
 */
public class KillAura extends Module {

    private final ModeValue sortMode = new ModeValue("Sort", "Adaptive", "Adaptive", "Distance", "Health", "Angle");
    private final ModeValue switchMode = new ModeValue("Switch", "Hysteresis", "Off", "Hysteresis", "Cycle");
    private final NumberValue switchBudget = new NumberValue("Switch Budget", 250, 0, 1000, 50, () -> !switchMode.is("Off"));

    private final NumberValue range = new NumberValue("Range", 3.0F, 1.0F, 6.0F, 0.05F);
    private final BoolValue throughWalls = new BoolValue("Through Walls", false);

    private final NumberValue rotationSpeed = new NumberValue("Rot Speed", 180, 5, 180, 5);
    private final ModeValue moveFix = new ModeValue("Move Fix", "Silent", "None", "Silent", "Strict");

    private final ModeValue attackMode = new ModeValue("Attack", "Legit", "Legit", "Packet");
    private final BoolValue keepSprint = new BoolValue("Keep Sprint", false);
    private final NumberValue minCps = new NumberValue("Min CPS", 8, 1, 20, 1);
    private final NumberValue maxCps = new NumberValue("Max CPS", 12, 1, 20, 1);

    private final ModeValue autoBlock = new ModeValue("Auto Block", "None", "None", "Fake", "Legit");
    private final NumberValue blockRange = new NumberValue("Block Range", 4.0F, 1.0F, 6.0F, 0.1F, () -> !autoBlock.is("None"));

    private final BoolValue grimSafe = new BoolValue("Grim Safe", true);
    private final BoolValue debug = new BoolValue("Debug", false);

    /** Sword blocking state, consumed by NoSlow. */
    public static boolean blocking;

    private final List<LivingEntity> candidates = new ArrayList<>();
    private final Random random = new Random();

    private LivingEntity target;
    private float[] aimRotation;
    private long nextAttackAt;
    private long lastSwitchAt;
    private boolean renderBlock;
    private boolean useKeyPressedByUs;

    public KillAura() {
        super("KillAura", Category.Combat);
        setDescription("Attack the best reachable target with server consistent aim.");
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
        candidates.clear();
        target = null;
        aimRotation = null;
        nextAttackAt = 0L;
        lastSwitchAt = 0L;
        releaseBlock();

        if (instance != null && instance.getRotationManager() != null) {
            instance.getRotationManager().clearRequest();
        }
    }

    // ------------------------------------------------------------------ targeting

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!canRun()) {
            reset();
            return;
        }

        collectCandidates();
        selectTarget();

        if (target == null) {
            aimRotation = null;
            releaseBlock();
            instance.getRotationManager().clearRequest();
            setSuffix("");
            return;
        }

        updateAutoBlock();

        aimRotation = AimUtil.rotationTo(AimUtil.aimPoint(target));
        instance.getRotationManager().request(aimRotation, rotationSpeed.getValue(), movementCorrection());

        setSuffix(target.getName().getString());
    }

    private boolean canRun() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return false;
        if (instance == null || instance.getTargetManager() == null) return false;

        return mc.player.isAlive() && !mc.player.isSpectator();
    }

    private void collectCandidates() {
        candidates.clear();

        // Turn towards targets slightly outside of attack range, so the aim is already there
        // when they walk in. Attacks are still validated against the strict range below.
        double scanRange = effectiveRange() + 0.6D;

        for (LivingEntity entity : instance.getTargetManager().getTargets()) {
            if (entity == mc.player) continue;
            if (entity.isDeadOrDying() || !entity.isAlive() || entity.getHealth() <= 0.0F) continue;
            if (!EntityUtil.isSelected(entity)) continue;
            if (AimUtil.distanceToBox(entity) > scanRange) continue;

            candidates.add(entity);
        }
    }

    private void selectTarget() {
        if (candidates.isEmpty()) {
            target = null;
            return;
        }

        long now = System.currentTimeMillis();
        boolean currentStillValid = target != null && candidates.contains(target);

        if (currentStillValid && switchMode.is("Hysteresis") && now - lastSwitchAt < switchBudget.getValue()) {
            return;
        }

        LivingEntity best = candidates.stream().max(Comparator.comparingDouble(this::score)).orElse(null);

        if (best == null) {
            target = null;
            return;
        }

        // Keep the incumbent unless a challenger is clearly better; flipping targets every tick
        // is both visually obvious and produces an unnatural packet pattern.
        if (currentStillValid && best != target && score(best) - score(target) < 0.12D) {
            return;
        }

        if (best != target) {
            target = best;
            lastSwitchAt = now;
        }
    }

    private double score(LivingEntity entity) {
        double distanceScore = 1.0D - Math.min(1.0D, AimUtil.distanceToBox(entity) / Math.max(0.1D, effectiveRange()));
        double angleScore = 1.0D - Math.min(1.0D, AimUtil.angleTo(entity, serverRotation()) / 180.0D);
        double healthScore = 1.0D - Math.min(1.0D, (entity.getHealth() + entity.getAbsorptionAmount()) / 20.0D);

        return switch (sortMode.getValue()) {
            case "Distance" -> distanceScore;
            case "Health" -> healthScore;
            case "Angle" -> angleScore;
            default -> distanceScore * 0.45D + angleScore * 0.35D + healthScore * 0.20D;
        };
    }

    private float[] serverRotation() {
        float[] rotation = instance.getRotationManager().getServerRotation();
        if (rotation != null) return rotation;

        return new float[]{mc.player.getYRot(), mc.player.getXRot()};
    }

    // ------------------------------------------------------------------ attacking

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPost()) return;
        if (!canRun() || target == null) return;
        if (mc.gui.screen() != null) return;

        // While Blink is holding packets the server has not seen this tick's rotation yet, so an
        // attack now would be judged against a stale look direction. Wait for the release.
        if (BlinkUtils.blinking) {
            if (debug.getValue()) setSuffix("blink");
            return;
        }

        // This is the rotation the movement packet just carried. Validating against the event
        // instead of the manager's field keeps the check independent of handler order.
        float[] sent = {event.getYRot(), event.getXRot()};

        // A miss on this check means the look direction the server holds misses the hitbox,
        // so an attack right now would be rejected as an invalid hit. Wait for the next tick.
        double reach = AimUtil.reach(target, sent);
        if (reach < 0.0D) {
            if (debug.getValue()) setSuffix("aim");
            return;
        }

        if (reach > effectiveRange()) {
            if (debug.getValue()) setSuffix(String.format("far %.2f", reach));
            return;
        }

        if (!throughWalls.getValue() && !AimUtil.isVisible(AimUtil.aimPoint(target))) {
            if (debug.getValue()) setSuffix("wall");
            return;
        }

        if (System.currentTimeMillis() < nextAttackAt) return;

        attack(target);
        scheduleNextAttack();

        if (debug.getValue()) setSuffix(String.format("%.2f", reach));
    }

    private void attack(LivingEntity entity) {
        if (mc.gameMode == null) return;

        boolean sprinting = mc.player.isSprinting();

        if (attackMode.is("Packet") || keepSprint.getValue()) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).callSyncSelectedSlot();
            PacketUtil.sendPacket(new ServerboundInteractPacket(entity.getId(), InteractionHand.MAIN_HAND, null, mc.player.isCrouching()));
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (keepSprint.getValue() && sprinting) {
            mc.player.setSprinting(true);
        }
    }

    /**
     * Human click rhythm: draw a CPS for this second, add gaussian jitter, and now and then
     * insert a short pause the way a real hand loses its rhythm.
     */
    private void scheduleNextAttack() {
        double min = Math.min(minCps.getValue(), maxCps.getValue());
        double max = Math.max(minCps.getValue(), maxCps.getValue());
        double cps = min + random.nextDouble() * (max - min);

        double base = 1000.0D / Math.max(1.0D, cps);
        double jitter = random.nextGaussian() * 0.12D * base;
        double interval = base + jitter;

        if (random.nextDouble() < 0.06D) {
            interval += 40.0D + random.nextInt(60);
        }

        nextAttackAt = System.currentTimeMillis() + (long) Math.max(50.0D, Math.min(400.0D, interval));
    }

    // ------------------------------------------------------------------ blocking

    private void updateAutoBlock() {
        boolean inRange = target != null && AimUtil.distanceToBox(target) <= blockRange.getValue();
        boolean shouldBlock = inRange && !autoBlock.is("None") && isHoldingSword();

        if (!shouldBlock) {
            releaseBlock();
            return;
        }

        renderBlock = true;

        if (autoBlock.is("Legit") && !blocking && mc.gui.screen() == null) {
            mc.options.keyUse.setDown(true);
            useKeyPressedByUs = true;
            blocking = true;
        }
    }

    private void releaseBlock() {
        renderBlock = false;

        if (useKeyPressedByUs) {
            mc.options.keyUse.setDown(false);
            useKeyPressedByUs = false;
        }

        blocking = false;
    }

    private boolean isHoldingSword() {
        return mc.player != null && mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS);
    }

    // ------------------------------------------------------------------ helpers

    private double effectiveRange() {
        double configured = range.getValue();
        // Vanilla survival interaction range is 3.0; above that is a measurable advantage.
        return grimSafe.getValue() ? Math.min(3.0D, configured) : configured;
    }

    private MovementCorrection movementCorrection() {
        if (moveFix.is("Silent")) return MovementCorrection.Silent;
        if (moveFix.is("Strict")) return MovementCorrection.Strict;
        return MovementCorrection.None;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public float[] getAimRotation() {
        return this.aimRotation;
    }

    public long getNextAttackAt() {
        return this.nextAttackAt;
    }

    public long getLastSwitchAt() {
        return this.lastSwitchAt;
    }

    public boolean isRenderBlock() {
        return this.renderBlock;
    }

    public boolean isUseKeyPressedByUs() {
        return this.useKeyPressedByUs;
    }

}
