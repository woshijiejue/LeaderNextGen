package cn.leader.util.player;

import cn.leader.util.IMinecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A block we already know how to reach: {@link #getBlockPos()} is the existing block that has to be
 * clicked, {@link #getDirection()} is the face of that block facing the hole we want to fill.
 *
 * <p>The point of this class is that a placement packet is only accepted when the look direction the
 * <em>server</em> holds actually intersects that face - not when it merely points somewhere close.
 * Every rotation produced here is therefore verified with a ray cast before it is returned.</p>
 *
 * @author Jon_awa (original), ported to 26.2
 */
public final class PlaceInfo implements IMinecraft {

    /**
     * Sample grid on a block face, 1/32 apart. Fine enough to always land inside the face while
     * staying cheap: at most 16 x 16 rays per axis.
     */
    private static final double[] PLACE_OFFSETS = {
            0.03125D, 0.09375D, 0.15625D, 0.21875D, 0.28125D, 0.34375D, 0.40625D, 0.46875D,
            0.53125D, 0.59375D, 0.65625D, 0.71875D, 0.78125D, 0.84375D, 0.90625D, 0.96875D
    };

    private static final double REACH = 4.5D;

    private final BlockPos blockPos;
    private final Direction direction;

    public PlaceInfo(BlockPos blockPos, Direction direction) {
        this.blockPos = blockPos;
        this.direction = direction;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public Hit getRotationAndHit(RotatePoint point, float[] reference, SearchSettings settings) {
        if (mc.level == null || mc.player == null) return null;

        return switch (point) {
            case Normal -> normal(reference);
            case Corner -> corner(reference);
            case Nearest -> nearest(reference);
            case Reduced -> reduced(reference, settings);
        };
    }

    // ------------------------------------------------------------------ rotation points

    /**
     * Face centres first (the cheapest and most forgiving rays), then fall back to the nearest point
     * on the face so a partially occluded block still gets a usable angle.
     */
    private Hit normal(float[] reference) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 base = Vec3.atLowerCornerOf(this.blockPos);

        Vec3[] candidates = {
                base.add(0.5D, 0.5D, 0.5D),
                base.add(0.5D, 1.001D, 0.5D),
                base.add(0.5D, 0.5D, -0.001D),
                base.add(0.5D, 0.5D, 1.001D),
                base.add(-0.001D, 0.5D, 0.5D),
                base.add(1.001D, 0.5D, 0.5D)
        };

        for (Vec3 candidate : candidates) {
            BlockHitResult result = raycast(eye, candidate);
            if (result == null) continue;
            if (!result.getBlockPos().equals(this.blockPos)) continue;

            return new Hit(candidate, rotationTo(eye, candidate));
        }

        return reference == null ? null : nearest(reference);
    }

    /** Closest of the two corners on the side the player is already looking from. */
    private Hit corner(float[] reference) {
        if (reference == null) return null;

        Vec3 eye = mc.player.getEyePosition();
        float yaw = normalize(reference[0]);
        double inset = 0.15D;

        double minX = this.blockPos.getX();
        double minZ = this.blockPos.getZ();
        double y = this.blockPos.getY() + 0.2D;

        Vec3 first;
        Vec3 second;

        if (yaw >= 315.0F || yaw < 45.0F) {
            first = new Vec3(minX + 1.0D - inset, y, minZ + inset);
            second = new Vec3(minX + 1.0D - inset, y, minZ + 1.0D - inset);
        } else if (yaw < 135.0F) {
            first = new Vec3(minX + inset, y, minZ + 1.0D - inset);
            second = new Vec3(minX + 1.0D - inset, y, minZ + 1.0D - inset);
        } else if (yaw < 225.0F) {
            first = new Vec3(minX + inset, y, minZ + inset);
            second = new Vec3(minX + inset, y, minZ + 1.0D - inset);
        } else {
            first = new Vec3(minX + inset, y, minZ + inset);
            second = new Vec3(minX + 1.0D - inset, y, minZ + inset);
        }

        Vec3 corner = eye.distanceToSqr(first) < eye.distanceToSqr(second) ? first : second;
        return new Hit(corner, rotationTo(eye, corner));
    }

    /**
     * Grid sweep over the clicked face: pick the sample whose angle is cheapest to reach from the
     * reference rotation. This is what keeps bridging from snapping the view around wildly.
     */
    private Hit nearest(float[] reference) {
        double[] xs = PLACE_OFFSETS;
        double[] ys = PLACE_OFFSETS;
        double[] zs = PLACE_OFFSETS;

        switch (this.direction) {
            case NORTH -> zs = new double[]{0.0D};
            case EAST -> xs = new double[]{1.0D};
            case SOUTH -> zs = new double[]{1.0D};
            case WEST -> xs = new double[]{0.0D};
            case DOWN -> ys = new double[]{0.0D};
            case UP -> ys = new double[]{1.0D};
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 base = Vec3.atLowerCornerOf(this.blockPos);

        Hit best = null;
        double bestCost = Double.MAX_VALUE;

        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vec3 point = base.add(x, y, z);
                    float[] rotation = rotationTo(eye, point);

                    BlockHitResult result = raycastFromRotation(rotation, REACH);
                    if (result == null) continue;

                    double cost = reference == null ? 0.0D : rotationCost(reference, rotation);
                    if (best != null && cost >= bestCost) continue;

                    bestCost = cost;
                    best = new Hit(result.getLocation(), rotation);
                }
            }
        }

        if (best != null) return best;
        return reference == null ? null : new Hit(null, reference);
    }

    /**
     * Full search with configurable inset and step. Candidates are sorted by how little they differ
     * from the current rotation, then verified one by one until the ray lands on the expected face.
     */
    private Hit reduced(float[] reference, SearchSettings settings) {
        if (reference == null) return nearest(null);

        double inset = settings.inset();
        double step = settings.step();
        boolean jitter = settings.jitter();
        double noise = step * 0.2D;

        Vec3 eye = mc.player.getEyePosition();
        double minX = this.blockPos.getX();
        double minY = this.blockPos.getY();
        double minZ = this.blockPos.getZ();

        boolean faceUp = Math.abs(eye.y - (minY + 1.0D)) < Math.abs(eye.y - minY);
        boolean faceSouth = Math.abs(eye.z - (minZ + 1.0D)) < Math.abs(eye.z - minZ);
        boolean faceEast = Math.abs(eye.x - (minX + 1.0D)) < Math.abs(eye.x - minX);

        float baseYaw = normalize(reference[0]);
        float basePitch = reference[1];
        int steps = Math.max(1, (int) Math.round(1.0D / step));

        List<Candidate> candidates = new ArrayList<>(3 * (steps + 1) * (steps + 1) + 1);
        candidates.add(new Candidate(0.0D, baseYaw, basePitch));

        Random random = new Random();

        for (int row = 0; row <= steps; row++) {
            double v = Mth.clamp(row * step + (jitter ? (random.nextDouble() * 2.0D - 1.0D) * noise : 0.0D), 0.0D, 1.0D);

            for (int column = 0; column <= steps; column++) {
                double u = Mth.clamp(column * step + (jitter ? (random.nextDouble() * 2.0D - 1.0D) * noise : 0.0D), 0.0D, 1.0D);

                float[] top = rotationTo(eye,
                        minX + u,
                        faceUp ? minY + 1.0D - inset : minY + inset,
                        minZ + v);
                candidates.add(new Candidate(rotationCost(baseYaw, basePitch, top), top[0], top[1]));

                float[] south = rotationTo(eye,
                        minX + u,
                        minY + v,
                        faceSouth ? minZ + 1.0D - inset : minZ + inset);
                candidates.add(new Candidate(rotationCost(baseYaw, basePitch, south), south[0], south[1]));

                float[] east = rotationTo(eye,
                        faceEast ? minX + 1.0D - inset : minX + inset,
                        minY + v,
                        minZ + u);
                candidates.add(new Candidate(rotationCost(baseYaw, basePitch, east), east[0], east[1]));
            }
        }

        candidates.sort((a, b) -> Double.compare(a.cost, b.cost));

        ItemStack held = mc.player.getMainHandItem();
        List<Hit> hits = new ArrayList<>();

        for (Candidate candidate : candidates) {
            float yaw = unwrap(candidate.yaw, reference[0]);
            float[] rotation = {yaw, candidate.pitch};

            BlockHitResult result = raycastFromRotation(rotation, REACH);
            if (result == null) continue;
            if (!result.getBlockPos().equals(this.blockPos)) continue;
            if (result.getDirection() != this.direction) continue;
            if (!canPlaceOnSide(held, this.blockPos, this.direction)) continue;

            hits.add(new Hit(result.getLocation(), rotation));
            if (hits.size() > (jitter ? 10 : 0)) break;
        }

        if (!hits.isEmpty()) {
            if (jitter) Collections.shuffle(hits);
            return hits.get(0);
        }

        return nearest(reference);
    }

    // ------------------------------------------------------------------ helpers

    public static boolean canPlaceOnSide(ItemStack stack, BlockPos pos, Direction side) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
        BlockPlaceContext context = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit);

        return blockItem.getBlock().getStateForPlacement(context) != null;
    }

    private BlockHitResult raycast(Vec3 from, Vec3 to) {
        if (mc.level == null || mc.player == null) return null;

        BlockHitResult result = mc.level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return result.getType() == HitResult.Type.BLOCK ? result : null;
    }

    private BlockHitResult raycastFromRotation(float[] rotation, double reach) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(Vec3.directionFromRotation(rotation[1], rotation[0]).scale(reach));

        return raycast(eye, end);
    }

    public static float[] rotationTo(Vec3 eye, double x, double y, double z) {
        return rotationTo(eye, new Vec3(x, y, z));
    }

    public static float[] rotationTo(Vec3 eye, Vec3 point) {
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    private static double rotationCost(float[] reference, float[] rotation) {
        return Math.abs(Mth.wrapDegrees(rotation[0] - reference[0])) + Math.abs(rotation[1] - reference[1]);
    }

    private static double rotationCost(float baseYaw, float basePitch, float[] rotation) {
        return Math.abs(Mth.wrapDegrees(rotation[0] - baseYaw)) + Math.abs(rotation[1] - basePitch);
    }

    /** Neutralise the 360 wrap so the value can be fed back into a rotation request. */
    private static float normalize(float yaw) {
        float value = yaw % 360.0F;
        if (value < 0.0F) value += 360.0F;
        if (value > 180.0F) value -= 360.0F;
        return value;
    }

    /** Rotate a normalised yaw back into the branch closest to the previous yaw. */
    private static float unwrap(float yaw, float reference) {
        return reference + Mth.wrapDegrees(yaw - reference);
    }

    // ------------------------------------------------------------------ types

    /** Resolved aim. {@code vec} is null when no ray could be verified, in which case nothing is placed. */
    public record Hit(Vec3 vec, float[] rotation) {
        public float yaw() {
            return this.rotation[0];
        }

        public float pitch() {
            return this.rotation[1];
        }
    }

    /** Tweaking for the {@code Reduced} search point, exposed on the module. */
    public record SearchSettings(double inset, double step, boolean jitter) {
    }

    private record Candidate(double cost, float yaw, float pitch) {
    }

    public enum RotatePoint {
        Normal,
        Corner,
        Nearest,
        Reduced
    }
}
