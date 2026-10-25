package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.mojang.blaze3d.vertex.PoseStack;

@Setter
@Getter
@AllArgsConstructor
public class Render3DEvent extends Event {
    private final PoseStack matrices;
    private final float partialTicks;
}
