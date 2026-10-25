package cn.injection.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {

    @Invoker
    void callSendSequencedPacket(final ClientLevel world, final SequencedPacketCreator packetCreator);

    @Invoker
    void callSyncSelectedSlot();

    @Accessor
    void setBlockBreakingCooldown(int blockBreakingCooldown);

}
