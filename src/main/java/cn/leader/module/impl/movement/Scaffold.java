package cn.leader.module.impl.movement;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.event.impl.WorldEvent;
import cn.leader.manager.RotationManager;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.misc.TimerUtil;
import cn.leader.util.player.ItemUtils;
import cn.leader.util.player.PlaceInfo;
import cn.leader.util.player.ScaffoldUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Automatic bridging.
 *
 * <p>Placement logic follows Epsilon's Scaffold: search for an existing block that can be clicked to
 * fill the hole below us, aim at a verified point on its face, then place. The click is fired from
 * the top of the player tick, before this tick's movement packet exists.</p>
 *
 * <p>Rotation is intentionally primitive: {@code Snap} turns instantly, {@code Smooth} turns at
 * {@code Rotation Speed} degrees per tick. Nothing emulates mouse input.</p>
 */
public final class Scaffold extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Normal", "Normal", "Telly");
    private final ModeValue rotation = new ModeValue("Rotation", "Snap", "Snap", "Smooth");
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 180, 20, 180, 10,
            () -> this.rotation.is("Smooth"));
    private final ModeValue rotationPoint = new ModeValue("Rotation Point", "Normal",
            "Normal", "Corner", "Nearest", "Reduced");
    private final ModeValue searchMode = new ModeValue("Search Mode", "Normal", "Normal", "Distance");
    private final ModeValue raycast = new ModeValue("Raycast", "Off", "Off", "Simple", "Normal", "Medium");
    private final ModeValue swingMode = new ModeValue("Swing", "Client", "Client", "Server", "Off");

    private final ModeValue switchMode = new ModeValue("Switch Block", "Biggest", "Normal", "Biggest", "Threshold");
    private final NumberValue switchThreshold = new NumberValue("Switch Threshold", 5, 0, 64, 1,
            () -> this.switchMode.is("Threshold"));

    private final NumberValue placeDelayMin = new NumberValue("Min Place Delay", 0, 0, 200, 1);
    private final NumberValue placeDelayMax = new NumberValue("Max Place Delay", 50, 0, 200, 1);

    // Reduced rotation point tuning
    private final NumberValue searchStep = new NumberValue("Search Step", 0.02F, 0.01F, 0.1F, 0.01F,
            () -> this.rotationPoint.is("Reduced"));
    private final NumberValue inset = new NumberValue("Inset", 0.1F, 0.05F, 0.1F, 0.01F,
            () -> this.rotationPoint.is("Reduced"));
    private final BoolValue jitter = new BoolValue("Jitter", true, () -> this.rotationPoint.is("Reduced"));

    // Addons
    private final BoolValue keepY = new BoolValue("Keep Y", false);
    private final BoolValue autoSneak = new BoolValue("Auto Sneak", false);

    // Telly timing
    private final ModeValue placeableMode = new ModeValue("Placeable Mode", "Check Height",
            () -> this.mode.is("Telly"), "Check Height", "Check Tick", "Off");
    private final NumberValue height = new NumberValue("Height", 0.5F, 0, 1.25F, 0.05F,
            () -> this.mode.is("Telly") && this.placeableMode.is("Check Height") && !this.keepY.getValue());
    private final NumberValue upTellyHeight = new NumberValue("UpTelly Height", 0.2F, 0, 1.25F, 0.05F,
            () -> this.mode.is("Telly") && this.placeableMode.is("Check Height") && !this.keepY.getValue());
    private final NumberValue tick = new NumberValue("Tick", 3, 0, 5, 1,
            () -> this.mode.is("Telly") && this.placeableMode.is("Check Tick"));
    private final NumberValue upTellyTick = new NumberValue("UpTelly Tick", 1, 0, 5, 1,
            () -> this.mode.is("Telly") && this.placeableMode.is("Check Tick") && !this.keepY.getValue());
    private final NumberValue jumpDelayMin = new NumberValue("Min Jump Delay", 1, 0, 3, 1, () -> this.mode.is("Telly"));
    private final NumberValue jumpDelayMax = new NumberValue("Max Jump Delay", 2, 0, 3, 1, () -> this.mode.is("Telly"));

    // Movement fix, plus how far a forward-facing turn may swing the view
    private final ModeValue moveFix = new ModeValue("Move Fix", "Silent", "Silent", "Strict", "None");
    private final NumberValue forwardYawLimit = new NumberValue("Forward Yaw Limit", 180, 15, 180, 5);

    private final TimerUtil placeTimer = new TimerUtil();
    private final Random random = new Random();

    private PlaceInfo lastPlaceInfo;
    private boolean canPlace;
    private int placeY;
    private int baseY;
    private int offGroundTicks;
    private int onGroundTicks;
    private String lastMode = "";

    public Scaffold() {
        super("Scaffold", Category.Movement);
        setDescription("Automatic bridging (Normal / Telly)");
    }

    @Override
    public void onEnable() {
        this.lastPlaceInfo = null;
        this.canPlace = false;
        this.offGroundTicks = 0;
        this.onGroundTicks = 0;
        this.placeTimer.reset();
        this.lastMode = "";
        setSuffix(this.mode.getValue());

        if (mc.player != null) {
            this.baseY = this.placeY = Mth.floor(mc.player.getY() - 1.0D);
        }
    }

    @Override
    public void onDisable() {
        this.lastPlaceInfo = null;
        this.canPlace = false;

        if (instance != null && instance.getRotationManager() != null) {
            instance.getRotationManager().clearRequest(this);
        }

        if (this.autoSneak.getValue() && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }

    // ------------------------------------------------------------------ tick

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // One rotation slot: never fight the aura for the view.
        if (auraBusy()) {
            this.canPlace = false;
            return;
        }

        if (!this.mode.getValue().equals(this.lastMode)) {
            this.lastMode = this.mode.getValue();
            setSuffix(this.lastMode);
        }

        if (mc.player.onGround()) {
            this.onGroundTicks++;
            this.offGroundTicks = 0;
        } else {
            this.onGroundTicks = 0;
            if (shouldBuild()) this.offGroundTicks++;
        }

        if (this.autoSneak.getValue()) {
            updateSneak();
        }

        switchBlock();

        this.placeY = computePlaceY();

        if (this.mode.is("Telly")) {
            switch (this.placeableMode.getValue()) {
                case "Check Height" -> {
                    double threshold = this.placeY + 1.0D
                            + (jumpKeyDown() && !this.keepY.getValue()
                            ? this.upTellyHeight.getValue() : this.height.getValue());

                    if (mc.player.fallDistance == 0.0F && mc.player.getY() < threshold && mc.player.hurtTime == 0) {
                        this.canPlace = false;
                        return;
                    }
                }
                case "Check Tick" -> {
                    int required = (jumpKeyDown() && !this.keepY.getValue()
                            ? this.upTellyTick.getValue() : this.tick.getValue()).intValue();

                    if (this.offGroundTicks < required) {
                        this.canPlace = false;
                        return;
                    }
                }
                default -> {
                }
            }
        }

        BlockPos base = BlockPos.containing(mc.player.getX(), this.placeY, mc.player.getZ());
        ScaffoldUtil.SearchMode search = this.searchMode.is("Distance")
                ? ScaffoldUtil.SearchMode.Distance : ScaffoldUtil.SearchMode.Normal;

        PlaceInfo info = ScaffoldUtil.getPlaceInfo(base, search);
        if (info == null) info = this.lastPlaceInfo;
        if (info == null) {
            this.canPlace = false;
            return;
        }

        this.lastPlaceInfo = info;

        PlaceInfo.Hit hit = info.getRotationAndHit(rotatePoint(), referenceRotation(), searchSettings());
        if (hit == null) {
            this.canPlace = false;
            return;
        }

        this.canPlace = true;
        float[] requested = aim(hit);
        place(info, hit, requested);
    }

    /** Where the search measures angles from: our current aim, otherwise the camera. */
    private float[] referenceRotation() {
        if (instance.getRotationManager() != null && instance.getRotationManager().isActive()) {
            return RotationManager.currentRotations;
        }

        return new float[]{mc.player.getYRot(), mc.player.getXRot()};
    }

    private float[] aim(PlaceInfo.Hit hit) {
        float[] target = clampForward(hit.rotation().clone());
        float speed = this.rotation.is("Smooth") ? this.rotationSpeed.getValue() : 180.0F;

        instance.getRotationManager().request(this, target, speed, correction());

        return target;
    }

    // ------------------------------------------------------------------ placing

    private void place(PlaceInfo info, PlaceInfo.Hit hit, float[] requested) {
        if (hit.vec() == null || !shouldBuild()) return;

        BlockPos feet = BlockPos.containing(mc.player.getX(), this.placeY, mc.player.getZ());
        if (!ScaffoldUtil.canPlaceAt(feet)) return;

        boolean mainHand = ItemUtils.isNotInBlockBlacklist(mc.player.getMainHandItem());
        boolean offHand = ItemUtils.isNotInBlockBlacklist(mc.player.getOffhandItem());
        if (!mainHand && !offHand) return;

        int min = Math.min(this.placeDelayMin.getValue().intValue(), this.placeDelayMax.getValue().intValue());
        int max = Math.max(this.placeDelayMin.getValue().intValue(), this.placeDelayMax.getValue().intValue());
        int delay = min == max ? min : min + this.random.nextInt(max - min + 1);

        if (delay != 0 && !this.placeTimer.hasTimeElapsed(delay)) return;

        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        BlockHitResult result = new BlockHitResult(hit.vec(), info.getDirection(), info.getBlockPos(), false);

        if (!passesRaycast(info, hit, requested)) return;

        boolean placeNow = !this.mode.is("Telly")
                || this.offGroundTicks > 2
                || (mc.player.onGround() && this.onGroundTicks < this.jumpDelayMin.getValue());

        if (!placeNow) return;

        InteractionResult placed = mc.gameMode.useItemOn(mc.player, hand, result);
        if (placed.consumesAction() || placed == InteractionResult.SUCCESS) {
            swing(hand);
            this.placeTimer.reset();
        }
    }

    /** Optional pre-flight check that the chosen face is really what we are looking at. */
    private boolean passesRaycast(PlaceInfo info, PlaceInfo.Hit hit, float[] requested) {
        switch (this.raycast.getValue()) {
            case "Simple" -> {
                return Math.abs(Mth.wrapDegrees(requested[0] - hit.rotation()[0])) <= 35.0F;
            }
            case "Normal" -> {
                BlockHitResult cast = castRay(mc.player.getYRot(), mc.player.getXRot());
                return cast != null && cast.getBlockPos().equals(info.getBlockPos());
            }
            case "Medium" -> {
                float[] server = instance.getRotationManager().getServerRotation();
                BlockHitResult cast = castRay(server[0], server[1]);
                return cast != null && cast.getBlockPos().equals(info.getBlockPos());
            }
            default -> {
                return true;
            }
        }
    }

    private BlockHitResult castRay(float yaw, float pitch) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(Vec3.directionFromRotation(pitch, yaw).scale(mc.player.blockInteractionRange()));

        BlockHitResult result = mc.level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return result.getType() == HitResult.Type.BLOCK ? result : null;
    }

    private void swing(InteractionHand hand) {
        switch (this.swingMode.getValue()) {
            case "Client" -> mc.player.swing(hand);
            case "Server" -> {
                if (mc.player.connection != null) {
                    mc.player.connection.send(new ServerboundSwingPacket(hand));
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ movement

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.mode.is("Telly") || mc.player == null) return;
        if (!moving()) return;

        int min = Math.min(this.jumpDelayMin.getValue().intValue(), this.jumpDelayMax.getValue().intValue());
        int max = Math.max(this.jumpDelayMin.getValue().intValue(), this.jumpDelayMax.getValue().intValue());
        int delay = min == max ? min : min + this.random.nextInt(max - min + 1);

        event.setJumping(delay == 0 || this.onGroundTicks >= delay);
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        setEnabled(false);
    }

    // ------------------------------------------------------------------ inventory

    private void switchBlock() {
        if (ItemUtils.isNotInBlockBlacklist(mc.player.getOffhandItem())) return;

        int current = mc.player.getInventory().getSelectedSlot();
        ItemStack stack = mc.player.getInventory().getItem(current);

        boolean needSwitch = stack.isEmpty()
                || !(stack.getItem() instanceof BlockItem blockItem)
                || !ItemUtils.isNotInBlockBlacklist(blockItem.getBlock())
                || stack.getCount() <= this.switchThreshold.getValue()
                || !this.switchMode.is("Threshold");

        if (!needSwitch) return;

        int slot = bestBlockSlot();
        if (slot == -1 || slot == current) return;

        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.gameMode != null) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).callSyncSelectedSlot();
        }
    }

    private int bestBlockSlot() {
        int current = mc.player.getInventory().getSelectedSlot();

        if (this.switchMode.is("Normal")) {
            for (int slot = 0; slot < 9; slot++) {
                if (ItemUtils.isNotInBlockBlacklist(mc.player.getInventory().getItem(slot))) return slot;
            }
            return -1;
        }

        int best = -1;
        int bestCount = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!ItemUtils.isNotInBlockBlacklist(stack)) continue;

            int count = stack.getCount();
            int distance = Math.abs(slot - current);

            if (count > bestCount || (count == bestCount && distance < bestDistance)) {
                bestCount = count;
                bestDistance = distance;
                best = slot;
            }
        }

        return best;
    }

    // ------------------------------------------------------------------ helpers

    /** Lock the build row to the one the player started on, unless they are jumping. */
    private int computePlaceY() {
        if (jumpKeyDown() && !this.keepY.getValue()) {
            return this.baseY = Mth.floor(mc.player.getY() - 1.0D);
        }

        if (this.baseY > mc.player.getY() - 1.0D || mc.player.onGround()) {
            return this.baseY = Mth.floor(mc.player.getY() - 1.0D);
        }

        return this.baseY;
    }

    /** Only build while the space under the feet is actually empty. */
    private boolean shouldBuild() {
        BlockPos under = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5D, mc.player.getZ());
        return mc.level.getBlockState(under).isAir();
    }

    private void updateSneak() {
        BlockPos under = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.1D, mc.player.getZ());

        if (mc.level.getBlockState(under).isAir()) {
            if (mc.player.onGround()) mc.options.keyShift.setDown(true);
        } else if (mc.player.onGround()) {
            mc.options.keyShift.setDown(false);
        }
    }

    private PlaceInfo.RotatePoint rotatePoint() {
        return switch (this.rotationPoint.getValue()) {
            case "Corner" -> PlaceInfo.RotatePoint.Corner;
            case "Nearest" -> PlaceInfo.RotatePoint.Nearest;
            case "Reduced" -> PlaceInfo.RotatePoint.Reduced;
            default -> PlaceInfo.RotatePoint.Normal;
        };
    }

    private PlaceInfo.SearchSettings searchSettings() {
        return new PlaceInfo.SearchSettings(
                this.inset.getValue().doubleValue(),
                this.searchStep.getValue().doubleValue(),
                this.jitter.getValue());
    }

    private MovementCorrection correction() {
        return switch (this.moveFix.getValue()) {
            case "Strict" -> MovementCorrection.Strict;
            case "None" -> MovementCorrection.None;
            default -> MovementCorrection.Silent;
        };
    }

    /** Keeps forward bridging from swinging the view past the configured limit. */
    private float[] clampForward(float[] rotation) {
        float limit = this.forwardYawLimit.getValue();
        if (limit >= 180.0F || !forwardKeyDown()) return rotation;

        float delta = Mth.wrapDegrees(rotation[0] - mc.player.getYRot());
        if (Math.abs(delta) <= limit) return rotation;

        rotation[0] = Mth.wrapDegrees(mc.player.getYRot() + Math.copySign(limit, delta));
        return rotation;
    }

    private boolean auraBusy() {
        if (instance == null || instance.getModuleManager() == null) return false;

        KillAura aura = instance.getModuleManager().getModule(KillAura.class);
        return aura != null && aura.isEnabled() && aura.getTarget() != null;
    }

    private boolean moving() {
        Vec3 velocity = mc.player.getDeltaMovement();
        return Math.abs(velocity.x) > 0.005D || Math.abs(velocity.z) > 0.005D;
    }

    private boolean jumpKeyDown() {
        return keyDown(mc.options.keyJump);
    }

    private boolean forwardKeyDown() {
        return keyDown(mc.options.keyUp);
    }

    private static boolean keyDown(net.minecraft.client.KeyMapping mapping) {
        return InputConstants.isKeyDown(mc.getWindow(), mapping.getDefaultKey().getValue());
    }
}
