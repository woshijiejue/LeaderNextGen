package cn.leader.module.impl.player;

import cn.injection.accessor.MinecraftClientAccessor;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.TickEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.NumberValue;

public final class FastPlace extends Module {

    private final NumberValue cooldown = new NumberValue("Cooldown", 0, 0, 4, 1);

    public FastPlace() {
        super("FastPlace", Category.Player);
        setDescription("Remove the item use cooldown between placements");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        ((MinecraftClientAccessor) mc).setItemUseCooldown(cooldown.getValue().intValue());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        ((MinecraftClientAccessor) mc).setItemUseCooldown(4);
    }
}
