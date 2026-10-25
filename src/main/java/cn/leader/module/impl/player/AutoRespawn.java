package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.TickEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import net.minecraft.client.gui.screens.DeathScreen;

/**
 * Leaves the death screen as soon as it appears.
 */
public final class AutoRespawn extends Module {

    private final BoolValue closeScreen = new BoolValue("Close Screen", true);

    public AutoRespawn() {
        super("AutoRespawn", Category.Player);
        setDescription("Respawn instantly on death");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        if (!(mc.screen instanceof DeathScreen)) return;

        mc.player.requestRespawn();

        if (closeScreen.getValue()) {
            mc.setScreen(null);
        }
    }
}
