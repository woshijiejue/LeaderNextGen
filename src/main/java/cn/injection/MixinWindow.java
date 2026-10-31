package cn.injection;

import cn.leader.ClientMeta;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Window.class)
public class MixinWindow {

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private String leaderNextgen$title(String title) {
        String custom = ClientMeta.title();
        return custom == null || custom.isBlank() ? title : custom;
    }
}
