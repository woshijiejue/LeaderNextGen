package cn.leader.module.impl.movement;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.util.player.MovementUtil;

public class Strafe extends Module {

    public Strafe() {
        super("Strafe", Category.Movement);
        setDescription("Aerial agility");
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        MovementUtil.strafe();
    }
}
