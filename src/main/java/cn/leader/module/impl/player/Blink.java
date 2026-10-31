package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.util.network.BlinkUtils;

public final class Blink extends Module {

    public Blink() {
        super("Blink", Category.Player);
        setDescription("Hold outgoing packets, flush on disable");
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            toggle();
            return;
        }

        BlinkUtils.startBlink();
    }

    @Override
    public void onDisable() {
        BlinkUtils.stopBlink();
        setSuffix("");
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(String.valueOf(BlinkUtils.getClientPackets().size()));
    }
}
