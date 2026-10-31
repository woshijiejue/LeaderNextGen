package cn.injection;

import cn.leader.event.impl.PacketEvent;
import cn.leader.util.IMinecraft;
import cn.leader.util.network.PacketUtil;
import net.minecraft.network.Connection;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinClientConnection implements IMinecraft {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (mc.player == null || mc.level == null) return;
        if (PacketUtil.getPackets().remove(packet)) return;

        PacketEvent event = new PacketEvent(packet, PacketEvent.Type.Send);
        instance.getEventManager().call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
