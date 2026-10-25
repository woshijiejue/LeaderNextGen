package cn.injection;

import cn.leader.event.impl.MoveInputEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends Input implements IMinecraft {

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick(CallbackInfo ci) {
        MoveInputEvent event = new MoveInputEvent(this.movementForward, this.movementSideways, this.playerInput.jump(), this.playerInput.sneak());
        instance.getEventManager().call(event);

        this.movementForward = event.getForward();
        this.movementSideways = event.getStrafe();
        this.playerInput = new PlayerInput(this.movementForward > 0, this.movementForward < 0, this.movementSideways > 0, this.movementSideways < 0, event.isJumping(), event.isSneaking(), this.playerInput.sprint());
    }
}