package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.movement.NoSlow;
import cn.leader.module.impl.movement.Scaffold;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.network.BlinkUtils;
import cn.leader.util.network.PacketUtil;
import cn.leader.util.player.AimUtil;
import cn.leader.util.player.EntityUtil;
import cn.leader.util.player.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
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

    private static final float BLOCK_SLOW = 0.2F;

    private final NumberValue range = new NumberValue("Range", 3.0F, 1.0F, 6.0F, 0.1F);
    private final BoolValue throughWalls = new BoolValue("Through Walls", false);
    private final NumberValue wallsRange = new NumberValue("Walls Range", 3.0F, 1.0F, 6.0F, 0.1F,
            this.throughWalls::getValue);
    private final ModeValue sort = new ModeValue("Sort", "Distance", "Distance", "Health", "Hurt Time");
    private final BoolValue playersOnly = new BoolValue("Players Only", true);

    private final ModeValue rotation = new ModeValue("Rotation", "Snap", "Snap", "Smooth");
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 180, 20, 180, 10,
            () -> this.rotation.is("Smooth"));
    private final ModeValue moveFix = new ModeValue("Move Fix", "Silent", "Silent", "Strict", "None");

    private final NumberValue cpsMin = new NumberValue("CPS Min", 10, 1, 20, 1);
    private final NumberValue cpsMax = new NumberValue("CPS Max", 14, 1, 20, 1);
    private final ModeValue keepSprintMode = new ModeValue("Keep Sprint Mode", "Grim", "Off", "Legit", "Grim");
    private final BoolValue swing = new BoolValue("Swing", true);

    private final ModeValue autoBlock = new ModeValue("Auto Block", "Off", "Off", "Hypixel", "Post");
    private final NumberValue blockRange = new NumberValue("Block Range", 4.0F, 0.0F, 6.0F, 0.1F,
            () -> !this.autoBlock.is("Off"));

    private LivingEntity target;
    private float[] aimRotation;
    private long nextAttackAt;

    private boolean sprintPaused;
    private boolean sprintArmed;
    private int sprintTries;
    private boolean firstHit;
    private LivingEntity lastTarget;

    private boolean serverUsing;
    private boolean blinking;
    private boolean usePending;
    private boolean releasePending;
    private int blockPhase;

    public KillAura() {
        super("KillAura", Category.Combat);
        setDescription("Automatically attacks the closest valid target");
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.aimRotation = null;
        this.nextAttackAt = 0L;
        this.sprintPaused = false;
        this.sprintArmed = false;
        this.sprintTries = 0;
        this.firstHit = true;
        this.lastTarget = null;
        this.serverUsing = false;
        this.blinking = false;
        this.usePending = false;
        this.releasePending = false;
        this.blockPhase = 0;
    }

    @Override
    public void onDisable() {
        endBlink();
        releaseUse();
        this.target = null;
        this.aimRotation = null;
        this.sprintPaused = false;
        this.sprintArmed = false;
        this.sprintTries = 0;
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

        this.sprintArmed = this.sprintPaused;
        this.sprintPaused = false;

        endBlink();

        if (this.autoBlock.is("Off")) {
            this.usePending = false;
            releaseUse();
        }

        if (this.autoBlock.is("Hypixel") && this.target != null && this.blockPhase == 0) {
            BlinkUtils.startBlink();
            this.blinking = true;
        }

        this.target = selectTarget();

        if (this.target != this.lastTarget) {
            this.lastTarget = this.target;
            this.firstHit = true;
        }

        if (this.target == null) {
            this.aimRotation = null;
            endBlink();
            release();
            releaseUse();
            return;
        }

        this.aimRotation = RotationUtil.getRotations(AimUtil.aimPoint(this.target));
        instance.getRotationManager().request(this, this.aimRotation, requestSpeed(), correction());

        if (this.autoBlock.is("Post")) return;

        tryAttack();

        if (this.autoBlock.is("Hypixel")) {
            tickAutoBlock();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (this.autoBlock.is("Hypixel")) {
            if (!event.isPost()) return;

            if (this.releasePending) {
                this.releasePending = false;
                releaseUse();
                return;
            }

            if (this.usePending) {
                this.usePending = false;

                if (wantsBlock()) {
                    startUse();
                }
            }
            return;
        }

        if (!this.autoBlock.is("Post")) return;

        if (!wantsBlock()) {
            endBlink();
            releaseUse();
            return;
        }

        if (event.isPre()) {
            BlinkUtils.startBlink();
            this.blinking = true;
            sendRelease();
            tryAttack();
            return;
        }

        startUse();
        endBlink();
    }

    private void tickAutoBlock() {
        if (!wantsBlock()) {
            this.blockPhase = 0;
            this.usePending = false;
            this.releasePending = false;
            endBlink();
            releaseUse();
            return;
        }

        if (this.blockPhase == 0) {
            this.blockPhase = 1;
            this.usePending = true;
        } else {
            this.blockPhase = 0;
            this.releasePending = true;
        }
    }

    private void endBlink() {
        if (!this.blinking) return;

        this.blinking = false;
        BlinkUtils.stopBlink();
    }

    private boolean wantsBlock() {
        return !this.autoBlock.is("Off")
                && this.target != null
                && this.target.isAlive()
                && mc.player.getMainHandItem().is(ItemTags.SWORDS)
                && !scaffoldBusy()
                && mc.player.distanceTo(this.target) <= this.blockRange.getValue().doubleValue();
    }

    private void startUse() {
        if (mc.player == null || mc.gameMode == null) return;

        EntityUtil.interactItem(mc.player, InteractionHand.MAIN_HAND);
        this.serverUsing = true;
    }

    private void sendRelease() {
        this.serverUsing = false;

        if (mc.getConnection() == null) return;

        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
    }

    private void releaseUse() {
        if (!this.serverUsing) return;

        this.serverUsing = false;

        if (mc.getConnection() == null) return;

        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
    }

    private boolean noSlowEnabled() {
        NoSlow noSlow = getModule(NoSlow.class);
        return noSlow != null && noSlow.isEnabled();
    }

    private boolean scaffoldBusy() {
        Scaffold scaffold = getModule(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    private boolean attackReady() {
        if (mc.player == null || this.target == null) return false;
        if (System.currentTimeMillis() < this.nextAttackAt) return false;
        if (mc.player.isUsingItem()) return false;
        if (mc.player.getAttackStrengthScale(0.5F) < 1.0F) return false;

        return inRange(this.target);
    }

    private void tryAttack() {
        if (!attackReady()) return;

        if (this.keepSprintMode.is("Legit") && this.target.hurtTime > 2) return;

        if (!this.firstHit && pauseSprint()) return;

        if (wantsBlock() && this.blockPhase == 1) return;

        if (mc.gameMode != null) {
            mc.gameMode.attack(mc.player, this.target);
        }

        if (this.swing.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        this.sprintArmed = false;
        this.sprintTries = 0;
        this.firstHit = false;
        this.nextAttackAt = System.currentTimeMillis() + clickInterval();
    }

    private boolean pauseSprint() {
        if (!this.keepSprintMode.is("Grim") || mc.player == null) return false;
        if (!mc.player.isSprinting()) return false;

        if (this.sprintArmed && ++this.sprintTries > 3) {
            this.sprintTries = 0;
            return false;
        }

        this.sprintPaused = true;
        this.sprintArmed = false;
        mc.player.setSprinting(false);
        return true;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.sprintPaused && this.keepSprintMode.is("Grim")) {
            event.setSprint(false);
        }

        if (this.serverUsing && !noSlowEnabled()) {
            event.setForward(event.getForward() * BLOCK_SLOW);
            event.setStrafe(event.getStrafe() * BLOCK_SLOW);
        }
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

        Vec3 eye = mc.player != null ? mc.player.getEyePosition() : null;
        Vec3 point = AimUtil.aimPoint(entity);

        BlockHitResult hit = mc.level.clip(new ClipContext(eye, point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return hit.getType() == HitResult.Type.MISS;
    }

    private float requestSpeed() {
        return this.rotation.is("Smooth") ? this.rotationSpeed.getValue() : 180.0F;
    }

    private MovementCorrection correction() {
        return switch (this.moveFix.getValue()) {
            case "Strict" -> MovementCorrection.Strict;
            case "None" -> MovementCorrection.None;
            default -> MovementCorrection.Silent;
        };
    }

    private void reset() {
        if (this.blinking) {
            endBlink();
        }
        releaseUse();
        this.target = null;
        this.aimRotation = null;
        this.release();
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
