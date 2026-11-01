package cn.injection;

import cn.leader.event.impl.PacketEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketUtils.class)
public class MixinNetworkThreadUtils implements IMinecraft {

    @Inject(
            method = "ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <T extends PacketListener> void leaderNextgen$receive(Packet<T> packet, T listener, PacketProcessor processor, CallbackInfo ci) {
        if (!processor.isSameThread()) {
            return;
        }

        PacketEvent event = new PacketEvent(packet, PacketEvent.Type.Received);
        instance.getEventManager().call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
