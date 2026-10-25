package cn.leader.module.impl.movement;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.CobwebEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.player.MovementUtil;

public class FastWeb extends Module {
    public final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "Motion");
    public final NumberValue motion = new NumberValue("Mode", 0.6f,0.1f, 1, 0.1f, () -> mode.is("Motion"));

    public FastWeb() {
        super("FastWeb", Category.Movement);
        setDescription("Move quickly through the web");
    }

    @EventTarget
    public void onCobweb(CobwebEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Vanilla")) {
            event.setCancelled(true);
        } else {
            MovementUtil.strafe(mode.is("GrimAC") ? 0.6f : motion.getValue());

            if (mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 1, mc.player.getVelocity().z);
            } else if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, -1, mc.player.getVelocity().z);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(mode.getValue());
    }
}