package cn.leader.module.value.impl;

import cn.leader.module.value.Value;
import java.awt.Color;
import java.util.function.Supplier;

public final class ColorValue extends Value {
    private float hue = 0.0F;
    private float saturation = 1.0F;
    private float brightness = 1.0F;

    public ColorValue(String name, Color color, Supplier<Boolean> visible) {
        super(name, visible);
        this.setValue(color);
    }

    public ColorValue(String name, Color color) {
        this(name, color, () -> true);
    }

    public Color getValue() {
        return Color.getHSBColor(this.hue, this.saturation, this.brightness);
    }

    public void setValue(Color color) {
        var hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public void setHSB(float h, float s, float b) {
        this.hue = h;
        this.saturation = s;
        this.brightness = b;
    }

    public float getHue() {
        return this.hue;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public float getBrightness() {
        return this.brightness;
    }

}
