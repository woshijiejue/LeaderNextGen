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

    @Inject(method = "onKey", at = @At(value="HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1) {
            KeyEvent event = new KeyEvent(key);
            instance.getEventManager().call(event);
        }
    }
}
