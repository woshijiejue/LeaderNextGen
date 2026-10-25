package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

@Getter
@Setter
@AllArgsConstructor
public class CobwebEvent extends Event {
    private final BlockState state;
    private final BlockPos pos;
}