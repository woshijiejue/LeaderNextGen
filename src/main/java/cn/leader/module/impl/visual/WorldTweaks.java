package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

import java.util.Map;
import java.util.Optional;

public class WorldTweaks extends Module {
    public final NumberValue ctimeVal = new NumberValue("Time", 21000, 0, 23000);
    public final ModeValue weather = new ModeValue("Weather", "Normal", "Normal", "Clear", "Rain", "Snow");
    public final NumberValue intensity = new NumberValue("Intensity", 1, 0.1f, 1, 0.1f);
    private float oldRainGradient;
    private ClockNetworkState oldClock;

    public WorldTweaks() {
        super("WorldTweaks", Category.Visual);
        setDescription("Level Adjustment");
    }

    private Optional<Holder.Reference<WorldClock>> overworldClock() {
        return mc.level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).get(WorldClocks.OVERWORLD);
    }

    private void setClock(ClockNetworkState state) {
        overworldClock().ifPresent(clock -> mc.level.clockManager().handleUpdates(mc.level.getGameTime(), Map.of(clock, state)));
    }

    @Override
    public void onEnable() {
        if (mc.level == null) return;

        oldClock = new ClockNetworkState(mc.level.getOverworldClockTime(), 0.0F, 1.0F);
        oldRainGradient = mc.level.getRainLevel(1);
    }

    @Override
    public void onDisable() {
        if (mc.level == null) return;

        if (oldClock != null) {
            setClock(oldClock);
        }
        mc.level.setRainLevel(oldRainGradient);
    }

    @EventTarget
    private void onPacket(PacketEvent event) {
        if (mc.level == null) return;

        if (event.getPacket() instanceof ClientboundSetTimePacket packet) {
            overworldClock().map(packet.clockUpdates()::get).ifPresent(state -> oldClock = state);
            event.setCancelled(true);
        }

        if (event.getPacket() instanceof ClientboundGameEventPacket packet && !weather.is("Normal")) {
            ClientboundGameEventPacket.Type reason = packet.getEvent();
            if (reason == ClientboundGameEventPacket.START_RAINING ||
                    reason == ClientboundGameEventPacket.STOP_RAINING ||
                    reason == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE ||
                    reason == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {

                if (reason == ClientboundGameEventPacket.START_RAINING) {
                    oldRainGradient = 1;
                }
                if (reason == ClientboundGameEventPacket.STOP_RAINING) {
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

        setClock(new ClockNetworkState(ctimeVal.getValue().longValue(), 0.0F, 0.0F));

        if (weather.is("Clear")) {
            mc.level.setRainLevel(0);
        } else if (weather.is("Rain") || weather.is("Snow")) {
            mc.level.setRainLevel(intensity.getValue());
        }
    }
}
