package cn.leader.module.value;

import java.util.function.Supplier;

public abstract class Value {
    private final String name;
    private final Supplier<Boolean> visible;

    public final boolean isVisible() {
        return Boolean.TRUE.equals(this.visible.get());
    }

    public String getName() {
        return this.name;
    }

    public Supplier<Boolean> getVisible() {
        return this.visible;
    }

    protected Value(String name, Supplier<Boolean> visible) {
        this.name = name;
        this.visible = visible;
    }

}
