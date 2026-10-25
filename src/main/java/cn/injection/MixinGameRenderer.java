package cn.injection;

import cn.leader.event.impl.Render3DEvent;
import cn.leader.module.impl.visual.NoHurtCam;
import cn.leader.util.IMinecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMinecraft {

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift = At.Shift.AFTER))
    private void renderWorld(DeltaTracker tickCounter, CallbackInfo ci) {
        instance.getEventManager().call(new Render3DEvent(new PoseStack(), tickCounter.getGameTimeDeltaPartialTick(true)));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void renderSkia(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        if (mc.player == null || mc.level == null) return;

        instance.getSkiaManager().render();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurt(CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
        if (instance.getModuleManager().getModule(NoHurtCam.class).isEnabled()) {
            ci.cancel();
        }
    }
}
