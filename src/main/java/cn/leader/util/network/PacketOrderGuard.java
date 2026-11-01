package cn.leader.util.network;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.PacketEvent;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.util.IMinecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

/**
 * 记录本 tick 是否已经发过"会占住 Grim PacketOrderE 状态位"的包。
 * 切快捷栏（HELD_ITEM_CHANGE）如果和攻击 / 使用物品 / 放方块 / 疾跑潜行包落在同一 tick，
 * 会被 PacketOrderE 判为 "Changed held item slot during another conflicting action"。
 */
public class PacketOrderGuard implements IMinecraft {

    private static boolean dirty;

    public PacketOrderGuard() {
        instance.getEventManager().register(this);
    }

    public static boolean isDirty() {
        return dirty;
    }

    @EventTarget
    @EventPriority(0)
    public void onUpdate(UpdateEvent event) {
        dirty = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.Send) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundAttackPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundPlayerActionPacket
                || packet instanceof ServerboundPlayerCommandPacket) {
            dirty = true;
        }
    }
}
