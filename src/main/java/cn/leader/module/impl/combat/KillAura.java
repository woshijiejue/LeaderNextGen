package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.player.AimUtil;
import cn.leader.util.player.EntityUtil;
import cn.leader.util.player.RotationUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {

    public static boolean blocking;

    private final NumberValue range = new NumberValue("Range", 3.0F, 1.0F, 6.0F, 0.1F);
    private final BoolValue throughWalls = new BoolValue("Through Walls", false);
    private final NumberValue wallsRange = new NumberValue("Walls Range", 3.0F, 1.0F, 6.0F, 0.1F,
            () -> this.throughWalls.getValue());
    private final ModeValue sort = new ModeValue("Sort", "Distance", "Distance", "Health", "Hurt Time");
    private final BoolValue playersOnly = new BoolValue("Players Only", true);

    private final ModeValue rotation = new ModeValue("Rotation", "Snap", "Snap", "Smooth");
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 180, 20, 180, 10,
            () -> this.rotation.is("Smooth"));
    private final ModeValue moveFix = new ModeValue("Move Fix", "Silent", "Silent", "None");

    private final NumberValue cpsMin = new NumberValue("CPS Min", 10, 1, 20, 1);
    private final NumberValue cpsMax = new NumberValue("CPS Max", 14, 1, 20, 1);
    private final BoolValue keepSprint = new BoolValue("Keep Sprint", true);
    private final BoolValue swing = new BoolValue("Swing", true);

    private LivingEntity target;
    private float[] aimRotation;
    private long nextAttackAt;

    public KillAura() {
        super("KillAura", Category.Combat);
        setDescription("Automatically attacks the closest valid target");
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.aimRotation = null;
        this.nextAttackAt = 0L;
        blocking = false;
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.aimRotation = null;
        blocking = false;
        release();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            reset();
            return;
        }

        if (mc.player.isDeadOrDying() || mc.gui.screen() != null) {
            reset();
            return;
        }

        this.target = selectTarget();

        if (this.target == null) {
            this.aimRotation = null;
            release();
            return;
        }

        this.aimRotation = RotationUtil.getRotations(AimUtil.aimPoint(this.target));
        instance.getRotationManager().request(this, this.aimRotation, requestSpeed(), correction());

        tryAttack();
    }

    private void tryAttack() {
        long now = System.currentTimeMillis();
        if (now < this.nextAttackAt) return;

        if (mc.player.getAttackStrengthScale(0.5F) < 1.0F) return;

        if (!inRange(this.target)) return;

        mc.gameMode.attack(mc.player, this.target);

        if (this.swing.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (!this.keepSprint.getValue()) {
            mc.player.setSprinting(false);
        }

        this.nextAttackAt = now + clickInterval();
    }

    private long clickInterval() {
        float min = Math.min(this.cpsMin.getValue(), this.cpsMax.getValue());
        float max = Math.max(this.cpsMin.getValue(), this.cpsMax.getValue());
        float cps = min + ThreadLocalRandom.current().nextFloat() * (max - min);

        return (long) (1000.0F / Math.max(1.0F, cps));
    }

    private LivingEntity selectTarget() {
        double reach = effectiveRange();
        AABB area = mc.player.getBoundingBox().inflate(reach + 1.0D);

        List<LivingEntity> candidates = new ArrayList<>(mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player
                        && EntityUtil.isSelected(entity)
                        && (!this.playersOnly.getValue() || entity instanceof Player)
                        && mc.player.distanceTo(entity) <= reach));

        candidates.removeIf(entity -> !visible(entity));
        if (candidates.isEmpty()) return null;

        candidates.sort(comparator());
        return candidates.get(0);
    }

    private Comparator<LivingEntity> comparator() {
        return switch (this.sort.getValue()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Hurt Time" -> Comparator.comparingInt((LivingEntity entity) -> -entity.hurtTime);
            default -> Comparator.comparingDouble(entity -> mc.player.distanceToSqr(entity));
        };
    }

    private boolean inRange(LivingEntity entity) {
        return mc.player.distanceTo(entity) <= effectiveRange() && visible(entity);
    }

    private double effectiveRange() {
        return this.throughWalls.getValue()
                ? this.wallsRange.getValue().doubleValue()
                : this.range.getValue().doubleValue();
    }

    private boolean visible(LivingEntity entity) {
        if (this.throughWalls.getValue()) return true;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 point = AimUtil.aimPoint(entity);

        BlockHitResult hit = mc.level.clip(new ClipContext(eye, point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return hit.getType() == HitResult.Type.MISS;
    }

    private float requestSpeed() {
        return this.rotation.is("Smooth") ? this.rotationSpeed.getValue() : 180.0F;
    }

    private MovementCorrection correction() {
        return this.moveFix.is("Silent") ? MovementCorrection.Silent : MovementCorrection.None;
    }

    private void reset() {
        this.target = null;
        this.aimRotation = null;
        blocking = false;
        release();
    }

    private void release() {
        if (instance != null && instance.getRotationManager() != null) {
            instance.getRotationManager().clearRequest(this);
        }
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

    public boolean isRenderBlock() {
        return false;
    }

    public boolean isUseKeyPressedByUs() {
        return false;
    }
}
