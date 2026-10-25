package cn.injection;

import cn.leader.Client;
import cn.leader.event.impl.TickEvent;
import cn.leader.event.impl.WorldEvent;
import cn.leader.util.IMinecraft;
import cn.leader.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient implements IMinecraft {

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;instance:Lnet/minecraft/client/Minecraft;", shift = At.Shift.AFTER, opcode = Opcodes.PUTSTATIC))
    private void preInit(CallbackInfo ci) {
        Client.instance = new Client();
        Client.logger = LogManager.getLogger(Client.name);
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHud:Lnet/minecraft/client/gui/hud/Gui;", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void postInit(CallbackInfo ci) {
        Client.instance.init();
    }

    @Inject(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;close()V", shift = At.Shift.AFTER))
    private void stop(CallbackInfo ci) {
        Client.instance.shutdown();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.onGround()) {
            Util.offGroundTicks = 0;
            Util.onGroundTicks++;
        } else {
            Util.onGroundTicks = 0;
            Util.offGroundTicks++;
        }

        Client.instance.getEventManager().call(new TickEvent());
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void setWorld(ClientLevel world, CallbackInfo ci) {
        Client.instance.getEventManager().call(new WorldEvent(world));
    }
}