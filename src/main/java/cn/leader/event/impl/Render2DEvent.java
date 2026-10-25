package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent extends Event {
    private final GuiGraphicsExtractor context;
    private final float partialTicks;

    public GuiGraphicsExtractor getContext() {
        return this.context;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public Render2DEvent(GuiGraphicsExtractor context, float partialTicks) {
        this.context = context;
        this.partialTicks = partialTicks;
    }

}
