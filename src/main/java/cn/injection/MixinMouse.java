package cn.injection;

import cn.leader.event.impl.MouseUpdateEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse implements IMinecraft {

    @Inject(method = "updateMouse", at = @At("RETURN"))
    private void updateMouse(CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        instance.getEventManager().call(new MouseUpdateEvent());
    }
}
