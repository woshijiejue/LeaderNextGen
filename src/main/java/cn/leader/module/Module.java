package cn.leader.module;

import cn.leader.Client;
import cn.leader.module.value.Value;
import cn.leader.util.IMinecraft;
import cn.leader.util.animation.Easing;
import cn.leader.util.animation.EasingAnimation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Module implements IMinecraft {
    private final EasingAnimation animation = new EasingAnimation(Easing.EASE_OUT_QUART, 300);
    private final List<Value> values = new ArrayList<>();
    private final String name;
    private final Category category;
    private String suffix = "";
    private String description = "";
    private boolean enabled;
    private boolean hidden;
    private int key = -1;

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                enable();
            } else {
                disable();
            }
        }
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        return instance.getModuleManager().getModule(clazz);
    }

    private void enable() {
        instance.getEventManager().register(this);

        try {
            onEnable();
        } catch (Exception e) {
            Client.logger.debug(e.getMessage());
        }
    }

    private void disable() {
        instance.getEventManager().unregister(this);

        try {
            onDisable();
        } catch (Exception e) {
            Client.logger.debug(e.getMessage());
        }
    }

    public void onEnable() {}
    public void onDisable() {}
}
