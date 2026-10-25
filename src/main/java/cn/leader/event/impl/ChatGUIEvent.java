package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ChatGUIEvent extends Event {
    private GuiGraphicsExtractor context;
    private int mouseX;
    private int mouseY;

    public ChatGUIEvent(GuiGraphicsExtractor context) {
        this.context = context;
    }

    public GuiGraphicsExtractor getContext() {
        return this.context;
    }

    public void setContext(GuiGraphicsExtractor context) {
        this.context = context;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public void setMouseX(int mouseX) {
        this.mouseX = mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public void setMouseY(int mouseY) {
        this.mouseY = mouseY;
    }
}
