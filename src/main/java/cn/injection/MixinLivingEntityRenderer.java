package cn.injection;

import cn.leader.event.impl.RotationEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> implements IMinecraft {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void entity(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci) {
        RotationEvent.currentEntity = livingEntity;
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0))
    private float yaw(float delta, float start, float end) {
        RotationEvent event = new RotationEvent(new float[]{end, 0}, new float[]{start, 0});
        if (RotationEvent.currentEntity == mc.player) {
            instance.getEventManager().call(event);
        }

        return Mth.rotLerp(delta, event.getLastRotation()[0], event.getRotation()[0]);
    }

    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float pitch(LivingEntity entity, float delta) {
        RotationEvent event = new RotationEvent(new float[]{0, entity.getXRot()}, new float[]{0, entity.xRotO});
        if (entity == mc.player) {
            instance.getEventManager().call(event);
        }

        return Mth.lerp(delta, event.getLastRotation()[1], event.getRotation()[1]);
    }
}