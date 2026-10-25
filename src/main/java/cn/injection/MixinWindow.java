package cn.injection;

import cn.leader.ClientMeta;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces every window title the game tries to set with the client identity.
 *
 * <p>Vanilla writes "Minecraft 1.21.4" (and later the world name) into the title bar, which makes
 * a modded client look like a stock game. Rewriting the argument keeps the client name and
 * version visible from the first frame, before any screen is drawn.</p>
 */
@Mixin(Window.class)
public class MixinWindow {

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private String leaderNextgen$title(String title) {
        String custom = ClientMeta.title();
        return custom == null || custom.isBlank() ? title : custom;
    }
}
