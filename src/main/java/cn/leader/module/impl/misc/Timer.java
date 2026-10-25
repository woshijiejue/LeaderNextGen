package cn.leader.module.impl.misc;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.Util;

public class Timer extends Module {
    private final NumberValue speed = new NumberValue("Speed", 1, .1f, 10, .1f);

    public Timer() {
        super("Timer", Category.Client);
        setDescription("Modify game speed");
    }

    @Override
    public void onDisable() {
        Util.timer = 1f;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(speed.getValue().toString());

        Util.timer = speed.getValue();
    }
}
