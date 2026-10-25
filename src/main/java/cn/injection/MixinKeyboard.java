package cn.injection;

import cn.leader.event.impl.KeyEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard implements IMinecraft {

    @Inject(method = "keyPress", at = @At(value="HEAD"))
    private void onKey(long window, int action, net.minecraft.client.input.KeyEvent keyEvent, CallbackInfo ci) {
        if (action == 1) {
            KeyEvent event = new KeyEvent(keyEvent.key());
            instance.getEventManager().call(event);
        }
    }
}
