package cn.injection;

import cn.leader.event.impl.Render2DEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinInGameHud implements IMinecraft {

    @Inject(method = "render", at = @At(value = "HEAD"))
    private void render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        Render2DEvent event = new Render2DEvent(context, tickCounter.getTickDelta(true));
        instance.getEventManager().call(event);
        instance.getSkiaManager().render();
    }
}
