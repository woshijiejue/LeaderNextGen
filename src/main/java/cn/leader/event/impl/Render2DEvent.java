package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;

@Setter
@Getter
@AllArgsConstructor
public class Render2DEvent extends Event {
    private final GuiGraphics context;
    private final float partialTicks;
}
