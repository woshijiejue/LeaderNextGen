package cn.injection.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Invoker("ensureHasSentCarriedItem")
    void callSyncSelectedSlot();

    @Invoker("startPrediction")
    void callSendSequencedPacket(ClientLevel level, PredictiveAction action);

    @Accessor("destroyDelay")
    void setBlockBreakingCooldown(int blockBreakingCooldown);

}
