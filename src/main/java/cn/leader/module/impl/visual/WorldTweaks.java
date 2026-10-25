package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class WorldTweaks extends Module {
    public final NumberValue ctimeVal = new NumberValue("Time", 21000, 0, 23000);
    public final ModeValue weather = new ModeValue("Weather", "Normal", "Normal", "Clear", "Rain", "Snow");
    public final NumberValue intensity = new NumberValue("Intensity", 1, 0.1f, 1, 0.1f);
    private float oldRainGradient;
    private boolean oldRaining;
    private long oldTime;

    public WorldTweaks() {
        super("WorldTweaks", Category.Visual);
        setDescription("Level Adjustment");
    }

    @Override
    public void onEnable() {
        if (mc.level == null) return;

        oldTime = mc.level.getTimeOfDay();
        oldRaining = mc.level.getLevelData().isRaining();
        oldRainGradient = mc.level.getRainGradient(1);
    }

    @Override
    public void onDisable() {
        if (mc.level == null) return;

        mc.level.getLevelData().setTimeOfDay(oldTime);
        mc.level.getLevelData().setRaining(oldRaining);
        mc.level.setRainGradient(oldRainGradient);
    }

    @EventTarget
    private void onPacket(PacketEvent event) {
        if (mc.level == null) return;

        if (event.getPacket() instanceof ClientboundSetTimePacket packet) {
            oldTime = packet.timeOfDay();
            event.setCancelled(true);
        }

        if (event.getPacket() instanceof ClientboundGameEventPacket packet && !weather.is("Normal")) {
            ClientboundGameEventPacket.Reason reason = packet.getReason();
            if (reason == ClientboundGameEventPacket.RAIN_STARTED ||
                    reason == ClientboundGameEventPacket.RAIN_STOPPED ||
                    reason == ClientboundGameEventPacket.RAIN_GRADIENT_CHANGED ||
                    reason == ClientboundGameEventPacket.THUNDER_GRADIENT_CHANGED) {

                if (reason == ClientboundGameEventPacket.RAIN_STARTED) {
                    oldRaining = true;
                    oldRainGradient = 1;
                }
                if (reason == ClientboundGameEventPacket.RAIN_STOPPED) {
                    oldRaining = false;
                    oldRainGradient = 0;
                }
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.level == null) return;

        setSuffix(ctimeVal.getValue().toString());

        mc.level.getLevelData().setTimeOfDay(ctimeVal.getValue().longValue());

        if (weather.is("Clear")) {
            mc.level.getLevelData().setRaining(false);
            mc.level.setRainGradient(0);
        } else if (weather.is("Rain") || weather.is("Snow")) {
            mc.level.getLevelData().setRaining(true);
            mc.level.setRainGradient(intensity.getValue());
        }
    }
}