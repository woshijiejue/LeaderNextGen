package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class ScaffoldUtil implements IMinecraft {

    private static final Direction[] SEARCH_FACES = {
            Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP
    };

    private static final int[][] NEIGHBOUR_OFFSETS = {
            {0, 1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    private static final Direction[] PLACE_FACES = {
            Direction.UP, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    private ScaffoldUtil() {
    }

    public static PlaceInfo getPlaceInfo(BlockPos origin, SearchMode mode) {
        if (origin == null) return null;

        return switch (mode) {
            case Normal -> searchNormal(origin);
            case Distance -> searchDistance(origin);
        };
    }

    private static PlaceInfo searchNormal(BlockPos origin) {
        if (mc.level == null) return null;

        ArrayDeque<BlockInfo> queue = new ArrayDeque<>();

        for (Direction face : SEARCH_FACES) {
            BlockPos neighbour = origin.relative(face.getOpposite());
            if (isAttachable(neighbour)) return new PlaceInfo(neighbour, face);

            queue.addLast(new BlockInfo(neighbour, 1));
        }

        while (!queue.isEmpty()) {
            BlockInfo current = queue.removeFirst();
            if (current.depth >= 4) continue;

            for (Direction face : SEARCH_FACES) {
                BlockPos next = current.pos.relative(face.getOpposite());
                if (isAttachable(next)) return new PlaceInfo(next, face);

                queue.addLast(new BlockInfo(next, current.depth + 1));
            }
        }

        for (int depth = 1; depth <= 2; depth++) {
            BlockPos below = origin.below(depth);
            if (isAttachable(below)) return new PlaceInfo(below, Direction.UP);
        }

        return null;
    }

    private static PlaceInfo searchDistance(BlockPos origin) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 feet = mc.player.position();
        int baseY = origin.getY();
        BlockPos feetPos = BlockPos.containing(feet.x, baseY, feet.z);

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = feetPos.getX() - 5; x <= feetPos.getX() + 5; x++) {
            for (int y = baseY - 1; y <= baseY; y++) {
                for (int z = feetPos.getZ() - 5; z <= feetPos.getZ() + 5; z++) {
                    cursor.set(x, y, z);

                    BlockState state = mc.level.getBlockState(cursor);
                    VoxelShape shape = state.getCollisionShape(mc.level, cursor);
                    if (shape.isEmpty()) continue;
                    if (isInteractable(cursor)) continue;

                    AABB box = shape.bounds();
                    double px = Mth.clamp(feet.x, x + box.minX, x + box.maxX);
                    double py = Mth.clamp(feet.y, y + box.minY, y + box.maxY);
                    double pz = Mth.clamp(feet.z, z + box.minZ, z + box.maxZ);

                    double score = feet.distanceToSqr(px, py, pz);
                    if (score >= bestScore) continue;

                    bestScore = score;
                    best = new BlockPos(x, y, z);
                }
            }
        }

        if (best == null) return null;

        Direction side = findPlaceSide(best, baseY, feet);
        return side == null ? null : new PlaceInfo(best, side);
    }

    private static Direction findPlaceSide(BlockPos blockPos, int baseY, Vec3 feet) {
        if (mc.player == null || mc.level == null) return null;

        int playerX = Mth.floor(feet.x);
        int playerY = baseY + 1;
        int playerZ = Mth.floor(feet.z);

        boolean jumping = !mc.player.onGround() && jumpKeyDown();

        double bestDistance = Double.MAX_VALUE;
        Direction bestFace = null;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int bx = blockPos.getX();
        int by = blockPos.getY();
        int bz = blockPos.getZ();

        for (int index = 0; index < NEIGHBOUR_OFFSETS.length; index++) {

            if (index == 0 && !jumping) continue;

            int[] offset = NEIGHBOUR_OFFSETS[index];
            int x = bx + offset[0];
            int y = by + offset[1];
            int z = bz + offset[2];

            if (x == playerX && y == playerY && z == playerZ) continue;

            cursor.set(x, y, z);
            Direction face = PLACE_FACES[index];

            if (!canPlaceAt(cursor)) continue;
            if (index == 0 && !hasLineOfSight(cursor, face)) continue;

            ItemStack held = mc.player.getMainHandItem();
            if (!PlaceInfo.canPlaceOnSide(held, cursor, face)) continue;

            Vec3 hit = closestPointOnFeet(cursor, feet);
            double distance = feet.distanceToSqr(hit);

            if (distance >= bestDistance) continue;

            bestDistance = distance;
            bestFace = face;
        }

        if (bestFace == null) return null;

        double current = feet.distanceToSqr(closestPointOnFeet(blockPos, feet));
        return bestDistance < current ? bestFace : null;
    }

    private static boolean hasLineOfSight(BlockPos pos, Direction face) {
        if (mc.player == null) return false;

        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
        Vec3 hit = centre.add(normal.scale(0.5D));
        Vec3 eye = mc.player.getEyePosition();
        Vec3 toFace = hit.subtract(eye);

        if (toFace.lengthSqr() > 20.25D) return false;

        return toFace.normalize().dot(normal.scale(-1.0D).normalize()) >= 0.0D;
    }

    private static Vec3 closestPointOnFeet(BlockPos pos, Vec3 feet) {
        VoxelShape shape = mc.level.getBlockState(pos).getCollisionShape(mc.level, pos);

        if (shape.isEmpty()) {
            return new Vec3(
                    Mth.clamp(feet.x, pos.getX() + 0.05D, pos.getX() + 0.95D),
                    Mth.clamp(feet.y, pos.getY() - 0.05D, pos.getY() + 1.05D),
                    Mth.clamp(feet.z, pos.getZ() + 0.05D, pos.getZ() + 0.95D));
        }

        AABB box = shape.bounds();
        return new Vec3(
                Mth.clamp(feet.x, pos.getX() + box.minX + 0.05D, pos.getX() + box.maxX - 0.05D),
                Mth.clamp(feet.y, pos.getY() + box.minY - 0.05D, pos.getY() + box.maxY + 0.05D),
                Mth.clamp(feet.z, pos.getZ() + box.minZ + 0.05D, pos.getZ() + box.maxZ - 0.05D));
    }

    public static boolean canPlaceAt(BlockPos pos) {
        if (mc.level == null) return false;

        BlockState state = mc.level.getBlockState(pos);

        if (state.isAir()) return true;

        if (state.getBlock() == Blocks.SNOW
                && state.hasProperty(SnowLayerBlock.LAYERS)
                && state.getValue(SnowLayerBlock.LAYERS) <= 1) {
            return true;
        }

        return state.canBeReplaced();
    }

    public static boolean isAttachable(BlockPos pos) {
        if (mc.level == null) return false;

        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
    }

    public static boolean isInteractable(BlockPos pos) {
        if (mc.level == null) return false;

        var block = mc.level.getBlockState(pos).getBlock();

        return block instanceof EntityBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof BedBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof FenceBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof JukeboxBlock;
    }

    private static boolean jumpKeyDown() {
        return InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getDefaultKey().getValue());
    }

    public static int blockCount() {        if (mc.player == null) return 0;

        int count = 0;

        ItemStack offhand = mc.player.getOffhandItem();
        if (ItemUtils.isNotInBlockBlacklist(offhand)) {
            count += offhand.getCount();
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (ItemUtils.isNotInBlockBlacklist(stack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private record BlockInfo(BlockPos pos, int depth) {
    }

    public enum SearchMode {
        Normal,
        Distance
    }
}
