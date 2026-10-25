package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.injection.accessor.LivingEntityAccessor;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super("NoJumpDelay", Category.Player);
        setDescription("No jump delay");
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        ((LivingEntityAccessor)mc.player).setJumpCooldown(0);
    }
}
