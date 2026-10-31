package cn.leader.module.impl.movement;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.LivingUpdateEvent;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.manager.movement.MovementCorrection;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Automatic bridging.
 *
 * <p>Placement happens in {@link LivingUpdateEvent} on purpose: that runs at the very top of
 * {@code aiStep}, before this tick's movement packet exists. The server therefore validates the
 * placement against the rotation it already received last tick, which is exactly the rotation we
 * verify with here. Placing later in the tick means the server sees a rotation that never matched
 * the hit vector.</p>
 */
public final class Scaffold extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Grim", "Grim", "Normal");
    private final ModeValue blockSelection = new ModeValue("Block Selection", "Maximum", "Maximum", "Nearest");

    private final NumberValue placeDelay = new NumberValue("Place Delay", 0, 0, 5, 1);
    private final NumberValue searchRadius = new NumberValue("Search Radius", 1.0F, 0.5F, 3.0F, 0.1F);
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 180, 30, 360, 10);
    private final NumberValue forwardYawLimit = new NumberValue("Forward Yaw Limit", 90, 15, 180, 5,
            () -> mode.is("Grim"));
    private final BoolValue diagonal = new BoolValue("Diagonal", true, () -> mode.is("Grim"));
    private final BoolValue telly = new BoolValue("Telly", true, () -> mode.is("Grim"));
    private final BoolValue swing = new BoolValue("Swing", true);
    private final BoolValue pauseOnAura = new BoolValue("Pause On Aura", true);
    private final BoolValue debug = new BoolValue("Debug", false);

    private Placement placement;
    private int delay;
    private int previousSlot = -1;

    public Scaffold() {
        super("Scaffold", Category.Movement);
        setDescription("Bridge for you, one block behind the feet");
    }

    @Override
    public void onEnable() {
        placement = null;
        delay = 0;
        previousSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
    }

    @Override
    public void onDisable() {
        placement = null;
        delay = 0;

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

        if (!mode.is("Grim")) {
            // Vanilla mode never turns for you: it only places what is already under the crosshair.
            if (debug.getValue()) setSuffix("vanilla");
            return;
        }

        float[] aim = rotationTo(placement.hit);
        float yaw = diagonal.getValue() ? snapDiagonal(aim[0]) : aim[0];
        yaw = limitForwardYaw(yaw);

        instance.getRotationManager().request(new float[]{yaw, aim[1]},
                rotationSpeed.getValue(), MovementCorrection.Silent);

        if (debug.getValue()) {
            setSuffix(String.format("aim %.0f", aim[0]));
        }
    }

    /**
     * The actual click. Runs after the rotation manager (priority 1000 vs 999) so the rotation for
     * this tick is already resolved; the ray is cast with the rotation the server knows about,
     * not the one we just requested.
     */
    @EventTarget
    @EventPriority(1000)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (placement == null) return;

        if (delay++ < placeDelay.getValue().intValue()) return;
        delay = 0;

        float[] server = instance.getRotationManager().getServerRotation();
        if (server == null) return;

        BlockHitResult hit = raycast(server[0], server[1]);
        if (hit == null) return;
        if (!hit.getBlockPos().equals(placement.support) || hit.getDirection() != placement.face) return;

        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        if (!result.consumesAction()) return;

        if (swing.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        placement = null;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!mode.is("Grim") || !telly.getValue()) return;
        if (placement == null || mc.player == null) return;
        if (!mc.player.onGround()) return;

        Vec2 move = mc.player.input.getMoveVector();
        if (move.y == 0.0F) return;

        event.setJumping(true);
    }

    // ------------------------------------------------------------------ target search

    private Placement findPlacement() {
        BlockPos base = mc.player.blockPosition().below();
        int radius = Math.max(1, (int) Math.ceil(searchRadius.getValue()));

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

    /**
     * While running forward, a scaffold that snaps the view sideways or backwards is the most
     * obvious thing on screen (and the easiest thing for a server to flag). This caps how far the
     * requested yaw may deviate from the player's own yaw in that situation.
     */
    private float limitForwardYaw(float yaw) {
        if (!mode.is("Grim")) return yaw;

        Vec2 move = mc.player.input.getMoveVector();
        if (move.y <= 0.0F) return yaw;

        float current = mc.player.getYRot();
        float delta = Mth.wrapDegrees(yaw - current);
        float limit = forwardYawLimit.getValue();

        if (Math.abs(delta) <= limit) return yaw;
        return Mth.wrapDegrees(current + Math.copySign(limit, delta));
    }

    private BlockHitResult raycast(float yaw, float pitch) {
        Vec3 eye = mc.player.getEyePosition();
        double reach = mc.player.blockInteractionRange();
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);

        BlockHitResult hit = mc.level.clip(new ClipContext(eye, eye.add(direction.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));

        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    // ------------------------------------------------------------------ inventory

    private int bestBlockSlot() {
        int current = mc.player.getInventory().getSelectedSlot();
        boolean nearest = blockSelection.is("Nearest");

        int best = -1;
        int bestCount = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            int distance = Math.abs(slot - current);

            if (nearest) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = slot;
                }
                continue;
            }

            int count = stack.getCount();
            if (count > bestCount || (count == bestCount && distance < bestDistance)) {
                bestCount = count;
                bestDistance = distance;
                best = slot;
            }
        }

        return best;
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
