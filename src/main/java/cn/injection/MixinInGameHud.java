package cn.injection;

import cn.leader.event.impl.Render2DEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class MixinInGameHud implements IMinecraft {

    @Inject(method = "extractRenderState", at = @At(value = "TAIL"))
    private void render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc.player == null || mc.level == null) return;

        Render2DEvent event = new Render2DEvent(context, tickCounter.getGameTimeDeltaPartialTick(true));
        instance.getEventManager().call(event);
    }
}
