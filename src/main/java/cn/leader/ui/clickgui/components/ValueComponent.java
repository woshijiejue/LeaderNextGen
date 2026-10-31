package cn.leader.ui.clickgui.components;

import cn.leader.module.value.Value;
import org.jetbrains.skija.Canvas;

public abstract class ValueComponent {
    private final Value value;
    protected float lastHeight = 17.0F;

    protected ValueComponent(Value value) {
        this.value = value;
    }

    public abstract float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha);

    public abstract void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width);

    public void mouseReleased(float mouseX, float mouseY, int button) {
    }

    protected boolean hovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getHeight() {
        return lastHeight;
    }

    public Value getValue() {
        return value;
    }
}
