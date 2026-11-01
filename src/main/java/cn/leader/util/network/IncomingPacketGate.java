package cn.leader.util.network;

import net.minecraft.network.protocol.Packet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gate that runs before the game handles an incoming packet.
 *
 * <p>Vanilla delivers every incoming packet through {@code Connection.genericsFtw}; that call is
 * the only place where a packet can still be dropped for real. The client handlers begin with
 * {@code PacketUtils.ensureRunningOnSameThread}, so cancelling <i>that</i> (the old
 * {@code PacketEvent} hook) only skips a thread check and the handler body keeps running.</p>
 *
 * <p>{@link #intercept(Packet)} is invoked on the netty thread: implementations must be fast,
 * thread-safe and must not touch game state.</p>
 */
public final class IncomingPacketGate {

    @FunctionalInterface
    public interface Interceptor {
        /**
         * @return {@code true} when the packet is taken over by the interceptor and must not be
         * handled by the game. The interceptor is then responsible for replaying it.
         */
        boolean shouldHold(Packet<?> packet);
    }

    private static final List<Interceptor> interceptors = new CopyOnWriteArrayList<>();

    public static void add(Interceptor interceptor) {
        if (interceptor != null && !interceptors.contains(interceptor)) {
            interceptors.add(interceptor);
        }
    }

    public static void remove(Interceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public static boolean hasInterceptors() {
        return !interceptors.isEmpty();
    }

    public static boolean intercept(Packet<?> packet) {
        for (Interceptor interceptor : interceptors) {
            try {
                if (interceptor.shouldHold(packet)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // an interceptor must never take the network thread down
            }
        }
        return false;
    }

    private IncomingPacketGate() {
    }
}
