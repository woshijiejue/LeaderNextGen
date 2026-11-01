package cn.leader.module.impl.movement;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.PacketEvent;
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
import cn.leader.util.network.ActionWindow;
import cn.leader.util.network.PacketOrderGuard;
import cn.leader.util.player.ItemUtils;
import cn.leader.util.player.PlaceInfo;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Random;

public final class Scaffold extends Module {

    private static final int NO_BLOCK = -1;
    private static final int OFFHAND = -2;

    private static final float[] YAW_CANDIDATES = {-135.0F, -90.0F, -45.0F, 0.0F, 45.0F, 90.0F, 135.0F, 180.0F};
    private static final float[] PITCH_CANDIDATES = {75.0F, 82.0F, 87.0F};

    private final ModeValue mode = new ModeValue("Mode", "Normal", "Normal", "Telly");

    private final ModeValue rotationMode = new ModeValue("Rotation Mode", "Static", "Static", "Hypixel", "Heypixel");
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 127, 10, 180, 1,
            () -> !this.rotationMode.is("Hypixel"));
    private final NumberValue rotationSpeed2 = new NumberValue("Rotation Speed 2", 36, 10, 180, 1,
            () -> this.rotationMode.is("Heypixel"));
    private final NumberValue rotationBackSpeed = new NumberValue("Rotation Back Speed", 180, 10, 180, 1,
            () -> this.mode.is("Telly"));
    private final NumberValue tellyTicks = new NumberValue("Telly Ticks", 1, 0, 6, 1,
            () -> this.mode.is("Telly"));
    private final BoolValue degrees45 = new BoolValue("45 Degrees", false);

    private final ModeValue raycast = new ModeValue("Raycast", "Normal", "Off", "Normal", "Strict");
    private final BoolValue selfRescue = new BoolValue("Self Rescue", true);
    private final BoolValue toggleOnTeleport = new BoolValue("Toggle On Teleport", false);
    private final BoolValue snap = new BoolValue("Snap", false, () -> this.mode.is("Normal"));

    private final ModeValue swapMode = new ModeValue("Swap Mode", "Normal", "Normal", "Silent", "None");
    private final BoolValue swapBack = new BoolValue("Swap Back", true,
            () -> this.swapMode.is("Normal"));

    private final ModeValue switchMode = new ModeValue("Switch Block", "Biggest", "Normal", "Biggest", "Threshold");
    private final NumberValue switchThreshold = new NumberValue("Switch Threshold", 5, 0, 64, 1,
            () -> this.switchMode.is("Threshold"));

    private final NumberValue placeDelayMin = new NumberValue("Min Place Delay", 0, 0, 200, 1);
    private final NumberValue placeDelayMax = new NumberValue("Max Place Delay", 50, 0, 200, 1);

    private final BoolValue keepY = new BoolValue("Keep Y", false);
    private final BoolValue autoSneak = new BoolValue("Auto Sneak", false);

    private final ModeValue moveFix = new ModeValue("Move Fix", "Silent", "Silent", "Strict", "None");
    private final ModeValue swingMode = new ModeValue("Swing", "Client", "Client", "Server", "Off");

    private final TimerUtil placeTimer = new TimerUtil();
    private final Random random = new Random();

    private int airTicks;
    private int yLevel;
    private BlockPos blockPos;
    private Direction direction;
    private float[] rotation;
    private int rotateCount;
    private float forwardInput;
    private float strafeInput;
    private float inputYaw;
    private float rawInputYaw;
    private double lengthSqr = 4.5D * 4.5D;

    private int blockSlot = NO_BLOCK;
    private int previousSlot = -1;
    private boolean restoreAfterPlace;
    private boolean restorePending;
    private boolean restoreOnDisable;
    private boolean emergencyPlacement;
    private boolean pearlSent;

    public Scaffold() {
        super("Scaffold", Category.Movement);
        setDescription("Automatic bridging");
    }

    @Override
    public void onEnable() {
        resetState();
        setSuffix(this.mode.getValue());
    }

    @Override
    public void onDisable() {
        if (this.restoreOnDisable || this.restoreAfterPlace || this.restorePending) {
            restoreSlot();
        }

        resetState();

        if (instance != null && instance.getRotationManager() != null) {
            instance.getRotationManager().clearRequest(this);
        }

        if (this.autoSneak.getValue() && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }

    private void resetState() {
        this.airTicks = 0;
        this.yLevel = 0;
        this.blockPos = null;
        this.direction = null;
        this.rotation = null;
        this.rotateCount = 0;
        this.blockSlot = NO_BLOCK;
        this.previousSlot = -1;
        this.restoreAfterPlace = false;
        this.restoreOnDisable = false;
        this.restorePending = false;
        this.emergencyPlacement = false;
        this.pearlSent = false;
        this.placeTimer.reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (auraBusy()) return;

        restorePendingSlot();

        this.emergencyPlacement = false;

        this.blockSlot = findBlockSlot();
        if (this.blockSlot == NO_BLOCK) return;

        if (mc.player.onGround()) {
            this.airTicks = 0;
            this.yLevel = Mth.floor(mc.player.getY()) - 1;
        } else {
            this.airTicks++;
        }

        getBlockInfo();

        if (this.autoSneak.getValue()) {
            updateSneak();
        }

        if (this.selfRescue.getValue() && handleSelfRescue()) {
            return;
        }

        if (this.mode.is("Telly")) {
            handleTelly();
        } else {
            handleNormal();
        }
    }

    private boolean handleSelfRescue() {
        if (this.blockPos == null || this.direction == null) return false;
        if (this.rotateCount > 8 || getBlockCount() < 1) return false;

        boolean reachable = true;
        if (mc.player.getDeltaMovement().y < -0.1D) {
            double predicted = predictY(2);
            if (this.blockPos.getY() > predicted) {
                reachable = false;
            }
        }

        double speed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
        if (reachable && speed < 1.5D) {
            this.rotateCount = 0;
            return false;
        }

        this.emergencyPlacement = true;
        this.rotateCount++;

        float[] target = getRotation(this.blockPos, this.direction);
        if (target == null) return true;

        instance.getRotationManager().request(this, target, 180.0F, correction());
        sendRotation(target);

        place();
        return true;
    }

    private void handleTelly() {
        if (mc.player.onGround() && (this.strafeInput != 0.0F || this.forwardInput != 0.0F)) {
            float pitch = this.rotation != null ? this.rotation[1] : mc.player.getXRot();
            requestRotation(new float[]{this.rawInputYaw, pitch}, this.rotationBackSpeed.getValue());
            return;
        }

        this.rotation = getRotation(this.blockPos, this.direction);
        if (this.rotation == null) return;

        float speed = this.rotationSpeed.getValue();
        if (this.rotationMode.is("Hypixel")) {
            speed = this.airTicks <= 1 ? 127.0F : 35.0F;
        } else if (this.rotationMode.is("Heypixel")) {
            speed = this.airTicks <= 1 ? this.rotationSpeed.getValue() : this.rotationSpeed2.getValue();
        }

        requestRotation(this.rotation, speed);

        if (this.airTicks > this.tellyTicks.getValue().intValue()) {
            place();
        }
    }

    private void handleNormal() {
        if (!this.snap.getValue() || !mc.player.onGround() || overEdge()) {
            this.rotation = getRotation(this.blockPos, this.direction);
            if (this.rotation != null) {
                requestRotation(this.rotation, this.rotationSpeed.getValue());
            }
        }
        place();
    }

    private void requestRotation(float[] target, float speed) {
        instance.getRotationManager().request(this, target.clone(), speed, correction());
    }

    private void sendRotation(float[] target) {
        if (mc.getConnection() == null) return;

        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                RotationManager.toSentYaw(target[0]), target[1],
                mc.player.onGround(), mc.player.horizontalCollision));
    }

    private void place() {
        if (!onAir() || this.blockPos == null || this.direction == null) return;
        if (!canUseBlockResult()) return;

        if (!this.emergencyPlacement && isPlaceDelayed()) return;

        if (this.raycast.is("Normal") || this.raycast.is("Strict")) {
            float[] current = this.rotation != null
                    ? this.rotation
                    : new float[]{mc.player.getYRot(), mc.player.getXRot()};
            Direction face = this.raycast.is("Strict") ? this.direction : null;

            if (!overBlock(current, this.blockPos, face)) return;
        }

        // 一个 tick 窗口只允许一个冲突动作，被占用就放弃这一拍，避免和 KillAura / NoSlow 撞 PacketOrder
        if (!ActionWindow.tryUse()) return;

        InteractionHand hand = swap();
        if (hand == null) return;

        BlockHitResult hit = new BlockHitResult(getVec3(this.blockPos, this.direction),
                this.direction, this.blockPos, false);

        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
        if (result.consumesAction()) {
            swing(hand);
            this.placeTimer.reset();
        }

        swapBack();
    }

    private boolean isPlaceDelayed() {
        int min = Math.min(this.placeDelayMin.getValue().intValue(), this.placeDelayMax.getValue().intValue());
        int max = Math.max(this.placeDelayMin.getValue().intValue(), this.placeDelayMax.getValue().intValue());
        int delay = min == max ? min : min + this.random.nextInt(max - min + 1);

        return delay != 0 && !this.placeTimer.hasTimeElapsed(delay);
    }

    private void getBlockInfo() {
        this.lengthSqr = 4.5D * 4.5D;
        this.blockPos = null;
        this.direction = null;

        if (!onAir()) return;

        Vec3 eye = mc.player.getEyePosition();
        int baseY = getYLevel();
        BlockPos base = BlockPos.containing(eye.x, baseY, eye.z);

        if (checkBlock(eye, base)) return;

        for (int d = 1; d <= 6; d++) {
            if (checkBlock(eye, new BlockPos(base.getX(), baseY - d, base.getZ()))) {
                return;
            }

            for (int x = 0; x <= d; x++) {
                for (int z = 0; z <= (d - x); z++) {
                    int y = d - x - z;

                    for (int revX = 0; revX <= 1; revX++) {
                        for (int revZ = 0; revZ <= 1; revZ++) {
                            BlockPos pos = new BlockPos(
                                    base.getX() + (revX == 0 ? x : -x),
                                    baseY - y,
                                    base.getZ() + (revZ == 0 ? z : -z));

                            checkBlock(eye, pos);
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3 eye, BlockPos pos) {
        if (!onAir() || pos.getY() > getYLevel()) return false;

        Vec3 center = Vec3.atBottomCenterOf(pos);

        for (Direction dir : Direction.values()) {
            Vec3 normal = dir.getUnitVec3();
            Vec3 hitPoint = center.add(normal.scale(0.5D));
            BlockPos support = pos.relative(dir);

            BlockState state = mc.level.getBlockState(support);
            if (state.getCollisionShape(mc.level, support).isEmpty()) continue;
            if (state.getMenuProvider(mc.level, support) != null) continue;

            Direction face = dir.getOpposite();
            Vec3 relevant = hitPoint.subtract(eye);

            if (relevant.lengthSqr() > this.lengthSqr) continue;
            if (relevant.dot(normal) < 0.0D) continue;
            if (face == Direction.UP && moving() && !jumpKeyDown()) continue;

            this.lengthSqr = relevant.lengthSqr();
            this.blockPos = support;
            this.direction = face;
            return true;
        }

        return false;
    }

    private float[] getRotation(BlockPos pos, Direction face) {
        if (this.rotation == null) {
            return new float[]{Mth.wrapDegrees(mc.player.getYRot() - 135.0F), 82.0F};
        }
        if (!onAir() || pos == null || face == null) {
            return this.rotation;
        }

        float[] calculated = PlaceInfo.rotationTo(mc.player.getEyePosition(), getVec3(pos, face));
        if (this.raycast.is("Off")) {
            return calculated;
        }

        Direction checkFace = this.raycast.is("Strict") ? face : null;
        float[] yaws = sortByInputYaw(YAW_CANDIDATES.clone());
        float[] finalYaws = Arrays.copyOf(yaws, yaws.length + 1);
        finalYaws[yaws.length] = calculated[0];

        for (float yaw : finalYaws) {
            if (this.degrees45.getValue() && yaw % 90.0F == 0.0F) continue;

            for (float pitch : PITCH_CANDIDATES) {
                float[] candidate = {
                        yaw + (float) randomOffset(0.3D),
                        pitch + (float) randomOffset(0.3D)
                };
                if (overBlock(candidate, pos, checkFace)) {
                    return candidate;
                }
            }

            for (int pitch = -90; pitch < 90; pitch++) {
                float[] candidate = {yaw, pitch};
                if (overBlock(candidate, pos, checkFace)) {
                    return candidate;
                }
            }
        }

        return calculated;
    }

    private float[] sortByInputYaw(float[] yaws) {
        float baseYaw = Mth.wrapDegrees(this.inputYaw - 180.0F);

        for (int i = 1; i < yaws.length; i++) {
            float key = yaws[i];
            int j = i - 1;

            while (j >= 0 && Math.abs(Mth.wrapDegrees(baseYaw - yaws[j]))
                    > Math.abs(Mth.wrapDegrees(baseYaw - key))) {
                yaws[j + 1] = yaws[j];
                j--;
            }
            yaws[j + 1] = key;
        }

        return yaws;
    }

    private boolean overBlock(float[] rotation, BlockPos pos, Direction face) {
        if (mc.level == null || mc.player == null) return false;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(Vec3.directionFromRotation(rotation[1], rotation[0])
                .scale(mc.player.blockInteractionRange() + 0.5D));

        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));

        if (hit.getType() != HitResult.Type.BLOCK) return false;
        if (!hit.getBlockPos().equals(pos)) return false;

        return face == null || hit.getDirection() == face;
    }

    private boolean onAir() {
        Vec3 eye = mc.player.getEyePosition();
        BlockPos base = BlockPos.containing(eye.x, getYLevel(), eye.z);

        return mc.level.getBlockState(base).canBeReplaced();
    }

    private int getYLevel() {
        int current = Mth.floor(mc.player.getY()) - 1;

        if (this.keepY.getValue()) {
            if (Math.abs(this.yLevel - current) <= 1.25D) {
                return this.yLevel;
            }
            return current;
        }

        if (!jumpKeyDown() && moving() && this.mode.is("Telly")
                && Math.abs(this.yLevel - current) <= 1.25D) {
            return this.yLevel;
        }

        return current;
    }

    private boolean overEdge() {
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

    private double predictY(int ticks) {
        double y = mc.player.getY();
        double motion = mc.player.getDeltaMovement().y;

        for (int i = 0; i < ticks; i++) {
            motion = (motion - 0.08D) * 0.98D;
            y += motion;
        }

        return y;
    }

    private Vec3 getVec3(BlockPos pos, Direction face) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08D;
        } else {
            x += randomOffset(0.3D);
            z += randomOffset(0.3D);
        }

        if (face == Direction.WEST || face == Direction.EAST) {
            z += randomOffset(0.3D);
        }

        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += randomOffset(0.3D);
        }

        return new Vec3(x, y, z);
    }

    private double randomOffset(double range) {
        return (this.random.nextDouble() * 2.0D - 1.0D) * range;
    }

    private int findBlockSlot() {
        if (ItemUtils.isValidBlock(mc.player.getOffhandItem())) {
            return OFFHAND;
        }

        return bestBlockSlot();
    }

    private int bestBlockSlot() {
        int current = mc.player.getInventory().getSelectedSlot();

        if (this.switchMode.is("Normal")) {
            for (int slot = 0; slot < 9; slot++) {
                if (ItemUtils.isValidBlock(mc.player.getInventory().getItem(slot))) return slot;
            }
            return NO_BLOCK;
        }

        if (this.switchMode.is("Threshold")) {
            ItemStack held = mc.player.getInventory().getItem(current);
            if (ItemUtils.isValidBlock(held) && held.getCount() > this.switchThreshold.getValue()) {
                return current;
            }
        }

        int best = NO_BLOCK;
        int bestCount = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!ItemUtils.isValidBlock(stack)) continue;

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

    private int getBlockCount() {
        int total = 0;

        if (ItemUtils.isValidBlock(mc.player.getOffhandItem())) {
            total += mc.player.getOffhandItem().getCount();
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (ItemUtils.isValidBlock(stack)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private boolean canUseBlockResult() {
        if (this.blockSlot == OFFHAND) {
            return ItemUtils.isValidBlock(mc.player.getOffhandItem());
        }
        if (this.blockSlot < 0) {
            return false;
        }
        if (this.swapMode.is("None")) {
            return ItemUtils.isValidBlock(mc.player.getInventory().getSelectedItem());
        }

        return true;
    }

    private InteractionHand swap() {
        if (this.blockSlot == OFFHAND) {
            return ItemUtils.isValidBlock(mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        }
        if (this.blockSlot < 0) return null;

        if (this.swapMode.is("None")) {
            return ItemUtils.isValidBlock(mc.player.getInventory().getSelectedItem())
                    ? InteractionHand.MAIN_HAND : null;
        }

        if (PacketOrderGuard.isDirty()) return null;

        int selected = mc.player.getInventory().getSelectedSlot();
        if (this.blockSlot == selected) {
            return InteractionHand.MAIN_HAND;
        }

        if (this.previousSlot < 0) {
            this.previousSlot = selected;
        }

        mc.player.getInventory().setSelectedSlot(this.blockSlot);
        syncSelectedSlot();

        if (this.swapMode.is("Silent")) {
            this.restoreAfterPlace = true;
        } else if (this.swapBack.getValue()) {
            this.restoreOnDisable = true;
        }

        return InteractionHand.MAIN_HAND;
    }

    private void swapBack() {
        if (this.restoreAfterPlace) {
            this.restorePending = true;
        }
    }

    private void restorePendingSlot() {
        if (!this.restorePending || PacketOrderGuard.isDirty()) return;

        this.restorePending = false;
        restoreSlot();
    }

    private void restoreSlot() {
        if (this.previousSlot >= 0 && this.previousSlot < 9) {
            mc.player.getInventory().setSelectedSlot(this.previousSlot);
            syncSelectedSlot();
        }

        this.previousSlot = -1;
        this.restoreAfterPlace = false;
        this.restoreOnDisable = false;
    }

    private void syncSelectedSlot() {
        if (mc.gameMode != null) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).callSyncSelectedSlot();
        }
    }

    private void swing(InteractionHand hand) {
        switch (this.swingMode.getValue()) {
            case "Client" -> mc.player.swing(hand);
            case "Server" -> {
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSwingPacket(hand));
                }
            }
            default -> {
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) return;

        this.forwardInput = event.getForward();
        this.strafeInput = event.getStrafe();

        float moveYaw = (float) Math.toDegrees(Math.atan2(-this.strafeInput, this.forwardInput));
        float yaw = mc.player.getYRot() + moveYaw;

        this.inputYaw = Mth.wrapDegrees(Math.round(yaw / 45.0F) * 45.0F);
        this.rawInputYaw = Mth.wrapDegrees(Math.round(yaw));

        if (this.mode.is("Telly") && mc.player.onGround() && !jumpKeyDown() && walking()) {
            event.setJumping(true);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.toggleOnTeleport.getValue()) return;

        if (event.getType() == PacketEvent.Type.Send
                && event.getPacket() instanceof ServerboundUseItemPacket packet) {
            ItemStack used = mc.player != null ? mc.player.getItemInHand(packet.getHand()) : ItemStack.EMPTY;
            if (used.is(net.minecraft.world.item.Items.ENDER_PEARL)) {
                this.pearlSent = true;
            }
            return;
        }

        if (event.getType() == PacketEvent.Type.Received
                && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            if (this.pearlSent) {
                this.pearlSent = false;
                setEnabled(false);
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        resetState();
        setSuffix(this.mode.getValue());
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

    private boolean walking() {
        return Math.abs(this.forwardInput) > 0.01F || Math.abs(this.strafeInput) > 0.01F;
    }

    private boolean jumpKeyDown() {
        return keyDown(mc.options.keyJump);
    }

    private MovementCorrection correction() {
        return switch (this.moveFix.getValue()) {
            case "Strict" -> MovementCorrection.Strict;
            case "None" -> MovementCorrection.None;
            default -> MovementCorrection.Silent;
        };
    }

    private static boolean keyDown(net.minecraft.client.KeyMapping mapping) {
        return InputConstants.isKeyDown(mc.getWindow(), mapping.getDefaultKey().getValue());
    }
}
