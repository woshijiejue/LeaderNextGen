package cn.injection;

import cn.leader.event.impl.JumpEvent;
import cn.leader.event.impl.MoveMathEvent;
import cn.leader.event.impl.RotationEvent;
import cn.leader.module.impl.visual.Animation;
import cn.leader.util.IMinecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements IMinecraft {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3 movementInput, CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        if (((Object) this) instanceof LocalPlayer) {
            MoveMathEvent event = new MoveMathEvent();
            instance.getEventManager().call(event);

            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float jump(LivingEntity entity) {
        if (mc.player == null || mc.world == null) return entity.getYaw();

        if (entity == mc.player) {
            JumpEvent event = new JumpEvent(entity.getYaw());
            instance.getEventManager().call(event);
            return event.getYaw();
        }

        return entity.getYaw();
    }

    @Redirect(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float turnHead(LivingEntity entity) {
        if (entity == mc.player) {
            RotationEvent event = new RotationEvent(new float[]{entity.getYaw(), 0}, new float[]{0, 0});
            instance.getEventManager().call(event);
            return event.getRotation()[0];
        }

        return entity.getYaw();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    public void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        Animation animation = instance.getModuleManager().getModule(Animation.class);

        if (animation.isEnabled()) {
            cir.setReturnValue(6 + animation.swingSpeed.getValue().intValue());
        }
    }
}
