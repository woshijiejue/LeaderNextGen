package cn.leader.event.impl;

import cn.leader.event.base.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public class Render3DEvent extends Event {
    private final PoseStack matrices;
    private final float partialTicks;

    public PoseStack getMatrices() {
        return this.matrices;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public Render3DEvent(PoseStack matrices, float partialTicks) {
        this.matrices = matrices;
        this.partialTicks = partialTicks;
    }

}
