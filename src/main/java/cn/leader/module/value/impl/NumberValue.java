package cn.leader.module.value.impl;

import cn.leader.module.value.Value;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import java.util.function.Supplier;

@Getter
public final class NumberValue extends Value {
    private final float min;
    private final float max;
    private final float inc;
    private float value;

    public NumberValue(String name, float value, float min, float max, float inc, Supplier<Boolean> visible) {
        super(name, visible);
        this.min = min;
        this.max = max;
        this.inc = inc;
        this.value = MathHelper.clamp(value, min, max);
    }

    public NumberValue(String name, float value, float min, float max, float inc) {
        this(name, value, min, max, inc, () -> true);
    }

    public NumberValue(String name, float value, float min, float max) {
        this(name, value, min, max, 1.0F, () -> true);
    }

    public Float getValue() {
        return MathHelper.clamp(this.value, this.min, this.max);
    }

    public void setValue(float value) {
        this.value = MathHelper.clamp(value, this.min, this.max);
    }
}