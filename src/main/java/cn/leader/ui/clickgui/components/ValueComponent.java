package cn.leader.ui.clickgui.components;

import cn.leader.module.value.Value;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
@Getter
public abstract class ValueComponent {
    private final Value value;
    protected float lastHeight = 15;

    public ValueComponent(Value value) {
        this.value = value;
    }

    public abstract float render(GuiGraphics context, int mouseX, int mouseY, float x, float y, float width);
    public abstract void mouseClicked(int mouseX, int mouseY, int button, float x, float y, float width);
    public void mouseReleased(int mouseX, int mouseY, int button) {}

    protected boolean hovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}