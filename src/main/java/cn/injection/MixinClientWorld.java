package cn.injection;

import cn.leader.util.IMinecraft;
import cn.leader.util.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientWorld implements IMinecraft {

    @Inject(method = "tickEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;resetPosition()V", shift = At.Shift.AFTER), cancellable = true)
    private void tickEntity(Entity entity, CallbackInfo ci) {
        if (Util.skipTicks > 0 && entity == IMinecraft.mc.player) {
            Util.skipTicks--;
            ci.cancel();
        }
    }
}
