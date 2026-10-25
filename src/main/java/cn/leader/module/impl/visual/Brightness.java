package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Brightness extends Module {

    public Brightness() {
        super("Brightness", Category.Visual);
        setDescription("The screen is highlighted");
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 8963, 0, false, false, false));
    }
}