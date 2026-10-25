package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ChatGUIEvent extends Event {
    private GuiGraphicsExtractor context;
    private int mouseX, mouseY;

    public GuiGraphicsExtractor getContext() {
        return this.context;
    }

    public void setContext(GuiGraphicsExtractor value) {
        this.context = value;
    }

    public ChatGUIEvent(GuiGraphicsExtractor context) {
        this.context = context;
    }

}
