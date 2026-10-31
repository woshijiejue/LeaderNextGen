package cn.leader.module.value.impl;

import cn.leader.module.value.Value;
import java.util.function.Supplier;

public final class BoolValue extends Value {
    private boolean value;

    public BoolValue(String name, boolean value, Supplier<Boolean> visible) {
        super(name, visible);
        this.value = value;
    }

    public BoolValue(String name, boolean value) {
        this(name, value, () -> true);
    }

    public boolean getValue() {
        return this.value;
    }

    public void toggle() {
        this.value = !this.value;
    }

    public boolean isValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

}
