package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;

public class Animation extends Module {
    public final NumberValue swingSpeed = new NumberValue("Swing Speed", 0, -4, 20, 1);
    public final ModeValue swingMode = new ModeValue("Swing Mode", "Vanilla", "Vanilla", "Smooth");
    public final ModeValue blockMode = new ModeValue("Block Mode", "1.7", "1.7", "SideDown");
    public final BoolValue equipProgress = new BoolValue("Equip Progress",true);

    public Animation() {
        super("Animation", Category.Visual);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(blockMode.getValue());
    }
}
