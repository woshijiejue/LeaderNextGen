package cn.leader.module.impl.combat;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.movement.Scaffold;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.network.ActionWindow;
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
    /**
     * 1.8 服务端的 itemInUse 会被挨打 / 攻击清掉，光举一次盾撑不了多久，必须周期性重新发 use。
     * 0 = 不主动刷新（只在状态丢了以后重举），数值越小举得越勤、留给攻击的窗口越少。
     */
    private final NumberValue blockRefresh = new NumberValue("Block Refresh", 5, 0, 20, 1,
            () -> this.autoBlock.is("Hypixel"));
    /**
     * 1.8 服务端把"举剑"当成真正使用物品，会按 0.2 倍速度预测位移，客户端也得同步减速，否则会被 Simulation 抓。
     * 现代服务端（剑不可用）根本不会减速，这时候绝不能自己减速，默认关掉。
     */
    private final BoolValue blockSlow = new BoolValue("Block Slow", false,
            () -> !this.autoBlock.is("Off"));

    private LivingEntity target;
    private float[] aimRotation;
    private long nextAttackAt;

    private boolean sprintPaused;
    private boolean sprintArmed;
    private int sprintTries;
    private boolean firstHit;
    private LivingEntity lastTarget;

    /** 服务端是否还认为我们在"使用物品"（1.8 手里举着剑格挡），只由我们自己发出去的 use / release 维护 */
    private boolean serverUsing;
    /** Hypixel：刷新流程走完 release 之后，下一拍补 use */
    private boolean pendingReuse;
    /** Hypixel：距离下一次刷新还有几拍 */
    private int refreshTicks;
    /** Post 模式：这一拍已经把盾放下去了，下一个窗口用来攻击 */
    private boolean postArmed;

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
        this.pendingReuse = false;
        this.refreshTicks = 0;
        this.postArmed = false;
    }

    @Override
    public void onDisable() {
        forceRelease();
        this.target = null;
        this.aimRotation = null;
        this.sprintPaused = false;
        this.sprintArmed = false;
        this.sprintTries = 0;
        this.postArmed = false;
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

        updateSuffix();

        this.target = selectTarget();

        if (this.target != this.lastTarget) {
            this.lastTarget = this.target;
            this.firstHit = true;
        }

        if (this.target == null) {
            this.aimRotation = null;
            this.postArmed = false;
            release();
            stopBlocking();
            return;
        }

        this.aimRotation = RotationUtil.getRotations(AimUtil.aimPoint(this.target));
        instance.getRotationManager().request(this, this.aimRotation, requestSpeed(), correction());

        if (this.autoBlock.is("Off")) {
            this.postArmed = false;
            stopBlocking();
            tryAttack();
            return;
        }

        tickAutoBlock();
    }

    /**
     * 所有动作都在 tick 头部（原版 handleKeybinds 的位置，移动包之前）发出，
     * 并且保证同一个 tick 窗口里只发一个"冲突动作"：
     * 攻击 / use / release 三者互斥，永远不会落进同一个 Grim 窗口。
     * <p>
     * 之前把 use / release 挪到移动包之后（post motion）是错的：
     * 那会让攻击（下一个 tick 头部，仍属于同一窗口）和 use 撞在一起触发 PacketOrderI，
     * 而且 release 发生在移动包之前、客户端减速却发生在下一次 aiStep，预测位移和实际位移差一拍，
     * Simulation 就是这么爆出来的。
     */
    private void tickAutoBlock() {
        if (!wantsBlock()) {
            this.postArmed = false;
            this.pendingReuse = false;
            stopBlocking();
            return;
        }

        if (this.autoBlock.is("Post")) {
            tickPostBlock();
        } else {
            tickHypixelBlock();
        }
    }

    /**
     * Hypixel：格挡常驻，攻击不占格挡（1.8 允许边举盾边打）。
     * <p>
     * 关键点是"刷新"：1.8 的 itemInUse 会在挨打 / 攻击之后被服务端清掉，setItemInUse 对同一把剑又是空操作，
     * 所以必须周期性 release + use 才能把盾重新举起来——只发一次 use 的话，掉一次状态就再也不防砍了。
     * 刷新走两拍（release 一拍、use 一拍），其余每一拍都可以攻击，且永远一窗一包。
     */
    private void tickHypixelBlock() {
        if (this.pendingReuse) {
            if (startBlocking()) {
                this.pendingReuse = false;
                this.refreshTicks = refreshInterval();
            }
            return;
        }

        if (!this.serverUsing) {
            if (startBlocking()) {
                this.refreshTicks = refreshInterval();
            }
            return;
        }

        int interval = refreshInterval();
        if (interval > 0 && --this.refreshTicks <= 0) {
            if (stopBlocking()) {
                this.pendingReuse = true;
            }
            return;
        }

        tryAttack();
    }

    /**
     * Post：一次攻击拆成 release -> attack -> use 三步，每步独占一个窗口。
     * 攻击落在"已经放盾"的窗口上（服务端里攻击时不在格挡状态，伤害不打折），打完下一个窗口立刻重新举盾。
     */
    private void tickPostBlock() {
        if (this.postArmed) {
            this.postArmed = false;

            if (!tryAttack() && attackReady()) {
                this.postArmed = true; // 这一拍窗口被别人占了，顺延一拍继续打，别退回举盾
            }
            return;
        }

        if (!this.serverUsing) {
            startBlocking();
            return;
        }

        if (!attackReady()) return;

        if (stopBlocking()) {
            this.postArmed = true;
        }
    }

    private int refreshInterval() {
        return this.blockRefresh.getValue().intValue();
    }

    /** HUD 上直接能看到现在到底有没有举着盾，方便判断 autoblock 是不是真的生效 */
    private void updateSuffix() {
        if (this.autoBlock.is("Off")) {
            setSuffix("");
            return;
        }

        setSuffix(this.autoBlock.getValue() + (this.serverUsing ? " · Block" : ""));
    }

    private boolean startBlocking() {
        if (mc.player == null || mc.gameMode == null) return false;
        if (!ActionWindow.tryUse()) return false;

        EntityUtil.interactItem(mc.player, InteractionHand.MAIN_HAND);
        this.serverUsing = true;
        return true;
    }

    /**
     * 放盾，返回 true 表示这一拍真的把 RELEASE_USE_ITEM 发出去了。
     * 窗口被别人占用时返回 false，下一个 tick 再试，绝不硬塞进同一个窗口。
     */
    private boolean stopBlocking() {
        if (!this.serverUsing) return false;

        if (mc.getConnection() == null) {
            this.serverUsing = false;
            return true;
        }

        if (!ActionWindow.tryUse()) return false;

        this.serverUsing = false;
        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        return true;
    }

    /** 关闭模块 / 死亡时用，不再考虑窗口占用，状态一定要收干净 */
    private void forceRelease() {
        if (!this.serverUsing) return;

        this.serverUsing = false;

        if (mc.getConnection() == null) return;

        PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
    }

    private boolean wantsBlock() {
        return !this.autoBlock.is("Off")
                && this.target != null
                && this.target.isAlive()
                && mc.player.getMainHandItem().is(ItemTags.SWORDS)
                && !scaffoldBusy()
                && mc.player.distanceTo(this.target) <= this.blockRange.getValue().doubleValue();
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

    private boolean tryAttack() {
        if (!attackReady()) return false;

        if (this.keepSprintMode.is("Legit") && this.target.hurtTime > 2) return false;

        if (!this.firstHit && pauseSprint()) return false;

        if (!ActionWindow.tryUse()) return false;

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
        return true;
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

        // serverUsing 在 tick 头部（移动包之前）就已经翻转，这里同步减速，
        // 和 Grim 按包序算出来的 tick 完全对齐
        if (this.blockSlow.getValue() && this.serverUsing) {
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
        forceRelease();
        this.target = null;
        this.aimRotation = null;
        this.postArmed = false;
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
