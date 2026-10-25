package cn.leader.module.impl.movement;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.SlowEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.util.network.BlinkUtils;
import cn.leader.util.network.PacketUtil;
import cn.leader.util.player.EntityUtil;
import cn.leader.util.player.ItemUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import static cn.leader.util.network.BlinkUtils.*;

public class NoSlow extends Module {
    public NoSlow(){super("NoSlow", Category.Movement);}
    private final ModeValue swordMode = new ModeValue("Sword Mode", "Prediction", "Vanilla", "Prediction");
    private final BoolValue swordSprint = new BoolValue("Sword Sprint",true);
    public static boolean unblockState = false;
    public static boolean interactingBlockThisTick = false;
    public static boolean serverBlocking = false;
    public static boolean shouldSetState = false;
    public static boolean noSlowBlinked = false;
    public static boolean slowByWaitingServer = false;
    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (swordMode.is("Prediction")) {
           handlePrediction(event);
        }
    }
    private void handlePrediction(MotionEvent event) {
        boolean holdingSword = !mc.player.getMainHandItem().isEmpty()
                && mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS);
        boolean wantBlock = holdingSword && (KillAura.blocking || mc.options.keyUse.isPressed());

        if (interactingBlockThisTick) return;

        if (wantBlock) {
            if (unblockState) {
                if (event.isPost()) {
                    EntityUtil.doItemUseWithoutBlock();
                    serverBlocking = true;
                    BlinkUtils.stopBlink();
                    noSlowBlinked = false;
                    unblockState = false;
                }
            } else {
                if (event.isPre()) {
                    BlinkUtils.startBlink();
                    noSlowBlinked = true;
                    PacketUtil.sendPacket(
                            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                                    BlockPos.ORIGIN, Direction.DOWN));
                    serverBlocking = false;
                    shouldSetState = true;
                    unblockState = true;
                }
                if (event.isPost()) {
                    if (shouldSetState) {
                        shouldSetState = false;
                    }
                }
            }
        } else {
            if (noSlowBlinked) {
                BlinkUtils.stopBlink();
                noSlowBlinked = false;
            }
            if (serverBlocking) {
                PacketUtil.sendPacket(
                        new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN, Direction.DOWN));
                serverBlocking = false;
            }
            unblockState = false;
            shouldSetState = false;
        }
    }
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(swordMode.getValue());
    }
    @EventTarget
    public void onSlow(SlowEvent event) {
        if (mc.player == null) return;
        if (mc.player.getMainHandItem().isEmpty()) return;
        if ((mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS) && !ItemUtils.isConsumable(mc.player.getOffhandItem()) && (mc.player.isUsingItem() || mc.options.keyUse.isPressed()))) {
            if (swordSprint.getValue()){
                mc.player.setSprinting(true);
            }
            event.setForward(1F);
            event.setSideways(1F);
        } else {
            event.setForward(0.2F);
            event.setSideways(0.2F);
        }
    }
}
