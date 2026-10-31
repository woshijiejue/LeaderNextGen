package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public class CobwebEvent extends Event {
    private final BlockState state;
    private final BlockPos pos;

    public BlockState getState() {
        return this.state;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public CobwebEvent(BlockState state, BlockPos pos) {
        this.state = state;
        this.pos = pos;
    }

}
