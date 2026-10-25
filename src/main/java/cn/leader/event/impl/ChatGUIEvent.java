package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
@Setter
@AllArgsConstructor
public class ChatGUIEvent extends Event {
    private GuiGraphics context;
    private int mouseX, mouseY;
}
