package cn.injection;

import cn.leader.event.impl.PacketEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.thread.ThreadExecutor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkThreadUtils.class)
public class MixinNetworkThreadUtils implements IMinecraft {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", at = @At("HEAD"))
    private static <T extends PacketListener> void forceMainThread(Packet<T> packet, T listener, ThreadExecutor<?> engine, CallbackInfo ci) throws OffThreadException {

        if (!engine.isOnThread()) {
            engine.executeSync(() -> {
                if (listener.accepts(packet)) {
                    try {
                        PacketEvent event = new PacketEvent(packet, PacketEvent.Type.Received);
                        instance.getEventManager().call(event);
                        if (event.isCancelled()) {
                            return;
                        }

                        packet.apply(listener);
                    } catch (Exception e) {
                        if (e instanceof CrashException crashException) {
                            if (crashException.getCause() instanceof OutOfMemoryError) {
                                throw NetworkThreadUtils.createCrashException(e, packet, listener);
                            }
                        }
                        listener.onPacketException(packet, e);
                    }
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
                }
            });

            throw OffThreadException.INSTANCE;
        }
    }
}