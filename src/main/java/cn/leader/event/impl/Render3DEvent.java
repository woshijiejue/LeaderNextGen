package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.MatrixStack;

@Setter
@Getter
@AllArgsConstructor
public class Render3DEvent extends Event {
    private final MatrixStack matrices;
    private final float partialTicks;
}
