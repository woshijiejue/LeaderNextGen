package cn.injection;

import cn.leader.event.impl.ChatGUIEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen implements IMinecraft {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void onChatGUI(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        instance.getEventManager().call(new ChatGUIEvent(context, mouseX, mouseY));
    }
}
