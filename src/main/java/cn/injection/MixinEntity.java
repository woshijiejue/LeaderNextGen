package cn.injection;

import cn.leader.event.impl.LookEvent;
import cn.leader.event.impl.StrafeEvent;
import cn.leader.util.IMinecraft;
import cn.leader.util.player.MovementUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IMinecraft {

    @Shadow
    public abstract Vec3 calculateViewVector(float pitch, float yaw);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public float yRotO;

    @Shadow
    public float xRotO;

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void getRotationVec(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        if (((Object) this) instanceof LocalPlayer) {
            float yaw = this.getYRot();
            float pitch = this.getXRot();
            float lastYaw = this.yRotO;
            float lastPitch = this.xRotO;

            LookEvent event = new LookEvent(new float[]{yaw, pitch}, new float[]{lastYaw, lastPitch});
            instance.getEventManager().call(event);

            float getYaw = Mth.lerp(tickDelta, event.getLastRotation()[0], event.getRotation()[0]);
            float getPitch = Mth.lerp(tickDelta, event.getLastRotation()[1], event.getRotation()[1]);

            cir.setReturnValue(this.calculateViewVector(getPitch, getYaw));
        }
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 updateVelocity(Vec3 movementInput, float speed, float yaw) {
        if (((Object) this) instanceof LocalPlayer) {
            StrafeEvent event = new StrafeEvent(yaw);
            instance.getEventManager().call(event);

            if (event.isCancelled()) {
                return Vec3.ZERO;
            }

            return MovementUtil.movementInputToVelocity(movementInput, speed, event.getYaw());
        }

        return MovementUtil.movementInputToVelocity(movementInput, speed, yaw);
    }
}
