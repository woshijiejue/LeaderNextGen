package cn.leader.manager;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TargetManager implements IMinecraft {
    private final List<LivingEntity> targets = new ArrayList<>();

    public TargetManager() {
        instance.getEventManager().register(this);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.level == null) return;

        targets.clear();
        for (Entity entity : mc.level.getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                targets.add(livingEntity);
            }
        }
    }
}