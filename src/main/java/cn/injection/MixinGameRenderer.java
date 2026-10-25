package cn.injection;

import cn.leader.event.impl.Render3DEvent;
import cn.leader.module.impl.visual.NoHurtCam;
import cn.leader.util.IMinecraft;
import cn.leader.util.render.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMinecraft {

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void renderWorld(DeltaTracker tickCounter, CallbackInfo ci) {
        PoseStack matrixStack = new PoseStack();
        Render3DEvent event = new Render3DEvent(matrixStack, tickCounter.getTickDelta(true));

        Camera camera = mc.gameRenderer.getCamera();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(Axis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(Axis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        RenderUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
        RenderUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
        RenderUtil.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
        instance.getEventManager().call(event);
        RenderSystem.getModelViewStack().popMatrix();
    }


    @Inject(at = @At("HEAD"), method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/PoseStack;F)V", cancellable = true)
    private void tiltViewWhenHurt(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        if (instance.getModuleManager().getModule(NoHurtCam.class).isEnabled()) {
            ci.cancel();
        }
    }
}
