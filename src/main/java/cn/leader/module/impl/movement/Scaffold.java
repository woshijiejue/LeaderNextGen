package cn.leader.module.impl.movement;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.LivingUpdateEvent;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.RotationManager;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import cn.leader.util.misc.TimerUtil;

import java.util.Random;

/**
 * Automatic bridging.
 *
 * <p>Two placement schools live here. {@code Grim} clicks at the top of {@code aiStep}, before this
 * tick's movement packet exists, and only when the rotation the server already knows really hits the
 * support face - the safest ordering there is. {@code Hypixel} instead flips the view nearly
 * backwards while grounded and then drags the yaw towards the target in the air at a limited rate,
 * which is what that server expects to see.</p>
 */
public final class Scaffold extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Grim", "Grim", "Hypixel", "Vanilla");
    private final ModeValue blockSelection = new ModeValue("Block Selection", "Maximum", "Maximum", "Nearest", "Threshold");
    private final NumberValue switchThreshold = new NumberValue("Switch Threshold", 5, 0, 64, 1,
            () -> blockSelection.is("Threshold"));
    private final ModeValue swing = new ModeValue("Swing", "Client", "Client", "Server", "Off");
    private final BoolValue pauseOnAura = new BoolValue("Pause On Aura", true);
    private final BoolValue debug = new BoolValue("Debug", false);

    // ---------------------------------------------------------------- Grim
    private final NumberValue placeDelay = new NumberValue("Place Delay", 0, 0, 5, 1, () -> mode.is("Grim"));
    private final NumberValue searchRadius = new NumberValue("Search Radius", 1.0F, 0.5F, 3.0F, 0.1F, () -> mode.is("Grim"));
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 180, 30, 360, 10, () -> mode.is("Grim"));
    private final NumberValue forwardYawLimit = new NumberValue("Forward Yaw Limit", 90, 15, 180, 5, () -> mode.is("Grim"));
    private final BoolValue diagonal = new BoolValue("Diagonal", true, () -> mode.is("Grim"));

    // ---------------------------------------------------------------- Hypixel
    private final NumberValue preTurn = new NumberValue("Pre Turn", 157, 90, 180, 1, () -> mode.is("Hypixel"));
    private final NumberValue prePitch = new NumberValue("Pre Pitch", 80, 60, 90, 1, () -> mode.is("Hypixel"));
    private final NumberValue yawLimit = new NumberValue("Yaw Limit", 35, 10, 180, 1, () -> mode.is("Hypixel"));
    private final NumberValue yawLimitAir = new NumberValue("Yaw Limit Air", 127, 30, 180, 1, () -> mode.is("Hypixel"));
    private final ModeValue raycast = new ModeValue("Raycast", "Server", () -> mode.is("Hypixel"), "Server", "Simple", "Off");
    private final NumberValue delayMin = new NumberValue("Delay Min", 0, 0, 200, 1, () -> mode.is("Hypixel"));
    private final NumberValue delayMax = new NumberValue("Delay Max", 50, 0, 200, 1, () -> mode.is("Hypixel"));
    private final BoolValue slow = new BoolValue("Slow", false, () -> mode.is("Hypixel"));

    // ---------------------------------------------------------------- shared
    private final BoolValue telly = new BoolValue("Telly", true, () -> !mode.is("Vanilla"));
    private final NumberValue jumpDelayMin = new NumberValue("Jump Delay Min", 1, 0, 3, 1,
            () -> mode.is("Hypixel") && telly.getValue());
    private final NumberValue jumpDelayMax = new NumberValue("Jump Delay Max", 2, 0, 3, 1,
            () -> mode.is("Hypixel") && telly.getValue());

    private final Random random = new Random();
    private final TimerUtil timer = new TimerUtil();

    private Placement placement;
    private int delay;
    private int previousSlot = -1;
    private int groundTicks;
    private int airTicks;
    private boolean canRotate;
    private float lastYaw;

    public Scaffold() {
        super("Scaffold", Category.Movement);
        setDescription("Automatic bridging (Grim / Hypixel)");
    }

    @Override
    public void onEnable() {
        placement = null;
        delay = 0;
        groundTicks = 0;
        airTicks = 0;
        canRotate = false;
        previousSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
    }

    @Override
    public void onDisable() {
        placement = null;
        canRotate = false;

        if (previousSlot >= 0 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;

        if (instance != null && instance.getRotationManager() != null) {
            instance.getRotationManager().clearRequest();
        }
    }

    // ------------------------------------------------------------------ aiming

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (mc.player.onGround()) {
            groundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            groundTicks = 0;
        }

        if (pauseOnAura.getValue() && auraBusy()) {
            placement = null;
            return;
        }

        int slot = bestBlockSlot();
        if (slot < 0) {
            placement = null;
            return;
        }

        switchTo(slot);
        placement = findPlacement();
        if (placement == null) return;

        if (mode.is("Hypixel")) {
            aimHypixel();
        } else if (mode.is("Grim")) {
            aimGrim();
        }
    }

    private void aimGrim() {
        float[] aim = rotationTo(placement.hit);
        float yaw = diagonal.getValue() ? snapDiagonal(aim[0]) : aim[0];
        yaw = limitForwardYaw(yaw);

        instance.getRotationManager().request(new float[]{yaw, aim[1]},
                rotationSpeed.getValue(), MovementCorrection.Silent);

        if (debug.getValue()) setSuffix(String.format("grim %.0f", aim[0]));
    }

    /**
     * Ground: look almost straight down and backwards, so the placement is behind us. Air: drag the
     * yaw towards the target but never faster than the configured limit - the first airborne tick is
     * allowed a much larger jump because that is the tick the server tolerates a flip.
     */
    private void aimHypixel() {
        float[] aim = rotationTo(placement.hit);
        float limit = (airTicks <= 1 ? yawLimitAir.getValue() : yawLimit.getValue());
        if (slow.getValue()) limit *= 0.8F;

        boolean grounded = mc.player.onGround();
        boolean flipWanted = grounded && isMoving()
                && mc.level.getBlockState(mc.player.blockPosition().above()).isAir();

        if (flipWanted) {
            // Turn away from the block we are about to place: the flip is instant, the server sees
            // it in the same movement packet that leaves the ground.
            float base = mc.player.getYRot() - movementOffset();
            float sign = Mth.wrapDegrees(lastYaw - base) >= 0.0F ? 1.0F : -1.0F;

            lastYaw = Mth.wrapDegrees(base + preTurn.getValue() * sign);
            canRotate = true;

            instance.getRotationManager().request(new float[]{lastYaw, prePitch.getValue()},
                    180.0F, MovementCorrection.Silent);
        } else {
            if (grounded) canRotate = false;

            lastYaw = aim[0];
            instance.getRotationManager().request(new float[]{aim[0], aim[1]},
                    limit, MovementCorrection.Silent);
        }

        if (debug.getValue()) setSuffix(String.format("hypixel %.0f", aim[0]));
    }

    // ------------------------------------------------------------------ placing

    @EventTarget
    @EventPriority(1000)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (placement == null) return;

        if (mode.is("Hypixel")) {
            placeHypixel();
        } else if (mode.is("Grim")) {
            placeGrim();
        }
    }

    /**
     * Grim: the click is only allowed when the rotation the server currently holds would hit the
     * same face we planned for.
     */
    private void placeGrim() {
        if (delay++ < placeDelay.getValue().intValue()) return;
        delay = 0;

        float[] server = instance.getRotationManager().getServerRotation();
        if (server == null) return;

        BlockHitResult hit = raycast(server[0], server[1]);
        if (hit == null) return;
        if (!hit.getBlockPos().equals(placement.support) || hit.getDirection() != placement.face) return;

        click(hit);
    }

    private void placeHypixel() {
        if (!checkRaycast()) return;

        int min = Math.min(delayMin.getValue().intValue(), delayMax.getValue().intValue());
        int max = Math.max(delayMin.getValue().intValue(), delayMax.getValue().intValue());
        int wait = min == max ? min : min + random.nextInt(max - min + 1);

        if (wait > 0 && !timer.hasTimeElapsed((long) wait)) return;

        BlockHitResult hit = new BlockHitResult(placement.hit, placement.face, placement.support, false);
        if (!click(hit)) return;

        timer.reset();
    }

    private boolean checkRaycast() {
        if (raycast.is("Off")) return true;

        if (raycast.is("Simple")) {
            float delta = Math.abs(Mth.wrapDegrees(RotationManager.currentRotations[0] - rotationTo(placement.hit)[0]));
            return delta < 35.0F;
        }

        float[] server = instance.getRotationManager().getServerRotation();
        if (server == null) return false;

        BlockHitResult hit = raycast(server[0], server[1]);
        return hit != null && hit.getBlockPos().equals(placement.support) && hit.getDirection() == placement.face;
    }

    private boolean click(BlockHitResult hit) {
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        if (!result.consumesAction()) return false;

        switch (swing.getValue()) {
            case "Client" -> mc.player.swing(InteractionHand.MAIN_HAND);
            case "Server" -> mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            default -> {
            }
        }

        placement = null;
        delay = 0;
        return true;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mode.is("Vanilla") || !telly.getValue() || placement == null || mc.player == null) return;
        if (!mc.player.onGround() || !isMoving()) return;

        if (mode.is("Hypixel")) {
            int min = Math.min(jumpDelayMin.getValue().intValue(), jumpDelayMax.getValue().intValue());
            int max = Math.max(jumpDelayMin.getValue().intValue(), jumpDelayMax.getValue().intValue());
            int wait = min == max ? min : min + random.nextInt(max - min + 1);
            if (groundTicks < wait) return;
        }

        event.setJumping(true);
    }

    // ------------------------------------------------------------------ target search

    private Placement findPlacement() {
        BlockPos base = mc.player.blockPosition().below();
        int radius = mode.is("Grim") ? Math.max(1, (int) Math.ceil(searchRadius.getValue())) : 1;

        double reach = mc.player.blockInteractionRange();
        double reachSqr = reach * reach;
        Vec3 eye = mc.player.getEyePosition();

        Placement best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos place = base.offset(dx, 0, dz);
                if (!canReplace(place)) continue;

                for (Direction side : Direction.values()) {
                    BlockPos support = place.relative(side);
                    BlockState state = mc.level.getBlockState(support);
                    if (state.getCollisionShape(mc.level, support).isEmpty()) continue;

                    Direction face = side.getOpposite();
                    Vec3 point = Vec3.atCenterOf(support)
                            .add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5D));

                    double distance = eye.distanceToSqr(point);
                    if (distance > reachSqr || distance >= bestDistance) continue;

                    BlockHitResult visible = mc.level.clip(new ClipContext(eye, point,
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                    if (visible.getType() != HitResult.Type.BLOCK) continue;
                    if (!visible.getBlockPos().equals(support)) continue;

                    bestDistance = distance;
                    best = new Placement(support, face, point);
                }
            }
        }

        return best;
    }

    private boolean canReplace(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return true;
        return state.getCollisionShape(mc.level, pos).isEmpty();
    }

    // ------------------------------------------------------------------ rotation helpers

    private float[] rotationTo(Vec3 point) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontal));

        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    private float snapDiagonal(float yaw) {
        return Mth.wrapDegrees(Math.round(yaw / 45.0F) * 45.0F);
    }

    private float limitForwardYaw(float yaw) {
        Input keys = mc.player.input.keyPresses;
        if (!keys.forward()) return yaw;

        float current = mc.player.getYRot();
        float delta = Mth.wrapDegrees(yaw - current);
        float limit = forwardYawLimit.getValue();

        if (Math.abs(delta) <= limit) return yaw;
        return Mth.wrapDegrees(current + Math.copySign(limit, delta));
    }

    /** How far the movement direction sits from the camera, the way the Hypixel flip expects it. */
    private float movementOffset() {
        Input keys = mc.player.input.keyPresses;
        boolean w = keys.forward();
        boolean s = keys.backward();
        boolean a = keys.left();
        boolean d = keys.right();
        boolean both = a && d;

        float offset = 0.0F;

        if (w) {
            offset -= 180.0F;
            if (!both) {
                if (a) offset += 45.0F;
                if (d) offset -= 45.0F;
            }
        } else if (!s) {
            offset -= 180.0F;
            if (!both) {
                if (a) offset += 90.0F;
                if (d) offset -= 90.0F;
            }
        } else {
            if (!both) {
                if (a) offset -= 45.0F;
                if (d) offset += 45.0F;
            }
        }

        return offset;
    }

    private BlockHitResult raycast(float yaw, float pitch) {
        Vec3 eye = mc.player.getEyePosition();
        double reach = mc.player.blockInteractionRange();
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);

        BlockHitResult hit = mc.level.clip(new ClipContext(eye, eye.add(direction.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));

        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private boolean isMoving() {
        Vec3 velocity = mc.player.getDeltaMovement();
        return Math.abs(velocity.x) > 0.005D || Math.abs(velocity.z) > 0.005D;
    }

    // ------------------------------------------------------------------ inventory

    private int bestBlockSlot() {
        int current = mc.player.getInventory().getSelectedSlot();
        int best = -1;

        if (blockSelection.is("Nearest")) {
            int bestDistance = Integer.MAX_VALUE;
            for (int slot = 0; slot < 9; slot++) {
                if (!isBlockIn(slot)) continue;
                int distance = Math.abs(slot - current);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = slot;
                }
            }
            return best;
        }

        int bestCount = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int slot = 0; slot < 9; slot++) {
            if (!isBlockIn(slot)) continue;

            int count = mc.player.getInventory().getItem(slot).getCount();
            int distance = Math.abs(slot - current);

            if (count > bestCount || (count == bestCount && distance < bestDistance)) {
                bestCount = count;
                bestDistance = distance;
                best = slot;
            }
        }

        return best;
    }

    private boolean isBlockIn(int slot) {
        ItemStack stack = mc.player.getInventory().getItem(slot);
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private void switchTo(int slot) {
        if (mc.player.getInventory().getSelectedSlot() == slot) return;

        mc.player.getInventory().setSelectedSlot(slot);

        if (mc.gameMode != null) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).callSyncSelectedSlot();
        }
    }

    private boolean auraBusy() {
        KillAura aura = instance.getModuleManager().getModule(KillAura.class);
        return aura != null && aura.isEnabled() && aura.getTarget() != null;
    }

    private record Placement(BlockPos support, Direction face, Vec3 hit) {
    }
}
