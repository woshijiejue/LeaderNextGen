package cn.leader.module.impl.player;

import cn.injection.accessor.MultiPlayerGameModeAccessor;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Swaps to the fastest tool for whatever block is being mined, and back again when mining stops.
 */
public final class AutoTool extends Module {

    private final BoolValue swapBack = new BoolValue("Swap Back", true);
    /** Tools below this multiplier are not worth the inventory packet. */
    private final NumberValue minSpeed = new NumberValue("Min Speed", 1.4F, 1.0F, 12.0F, 0.1F);

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", Category.Player);
        setDescription("Switch to the best tool while mining");
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (!mc.gameMode.isBreakingBlock() || !(mc.hitResult instanceof BlockHitResult hit)) {
            restore();
            return;
        }

        BlockState state = mc.level.getBlockState(hit.getBlockPos());

        int best = bestSlot(state);
        if (best == -1) return;

        int current = mc.player.getInventory().selected;
        if (best == current) return;

        if (previousSlot == -1) {
            previousSlot = current;
        }

        select(best);
    }

    @Override
    public void onDisable() {
        restore();
    }

    private int bestSlot(BlockState state) {
        float bestSpeed = minSpeed.getValue();
        int bestSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private void select(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);

        if (mc.gameMode != null) {
            ((MultiPlayerGameModeAccessor) mc.gameMode).callSyncSelectedSlot();
        }
    }

    private void restore() {
        if (previousSlot == -1) return;

        if (swapBack.getValue() && mc.player != null) {
            select(previousSlot);
        }

        previousSlot = -1;
    }
}
