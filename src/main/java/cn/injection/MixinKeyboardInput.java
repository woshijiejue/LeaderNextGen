package cn.injection;

import cn.leader.event.impl.MoveInputEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 replaced the old mutable input fields with a {@link Vec2} move vector plus an immutable
 * {@link Input} key-press record, so the event carries floats and both are rebuilt afterwards.
 */
@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends ClientInput implements IMinecraft {

    @Inject(method = "tick", at = @At("TAIL"))
    private void leaderNextgen$tick(CallbackInfo ci) {
        Vec2 move = this.getMoveVector();
        Input keys = this.keyPresses;

        MoveInputEvent event = new MoveInputEvent(move.x, move.y, keys.jump(), keys.shift());
        instance.getEventManager().call(event);

        this.moveVector = new Vec2(event.getForward(), event.getStrafe());
        this.keyPresses = new Input(
                event.getForward() > 0.0F,
                event.getForward() < 0.0F,
                event.getStrafe() > 0.0F,
                event.getStrafe() < 0.0F,
                event.isJumping(),
                event.isSneaking(),
                keys.sprint()
        );
    }
}
