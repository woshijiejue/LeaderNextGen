package cn.leader.util.network;

import cn.leader.event.base.annotation.EventPriority;
import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MotionEvent;
import cn.leader.event.impl.WorldEvent;
import cn.leader.util.IMinecraft;

/**
 * 一个 "tick 窗口" 指的是两个移动包 (ServerboundMovePlayerPacket) 之间的所有包。
 * <p>
 * Grim 的 PacketOrderProcessor 在每个窗口收到移动包时清零状态位，PacketOrderI / J / M / N
 * 全部是在窗口内判定"两个冲突动作同时出现"，而且两个方向都抓：
 * <ul>
 *     <li>PacketOrderI: 窗口里先 use 再 attack → attack 时 isRightClicking() 为真</li>
 *     <li>PacketOrderJ: 窗口里先 attack 再 use → use 时 isAttacking() 为真</li>
 *     <li>PacketOrderI: 窗口里 release 和 use/attack 同时出现同样会 flag</li>
 * </ul>
 * 也就是说：攻击 / 使用物品 / 释放使用 / 挖掘，必须一个窗口只出现一个，
 * 顺序无论如何排列都救不回来。
 * <p>
 * 这个类就负责在客户端侧把窗口当成独占资源分配。
 */
public final class ActionWindow implements IMinecraft {

    private static boolean used;

    public ActionWindow() {
        instance.getEventManager().register(this);
    }

    /**
     * 申请当前窗口的操作权。
     *
     * @return false 表示这个窗口已经被别的动作占用，本次调用方必须放弃，留到下一个 tick 再试
     */
    public static boolean tryUse() {
        if (used) return false;
        used = true;
        return true;
    }

    public static boolean isUsed() {
        return used;
    }

    @EventTarget
    @EventPriority(0)
    public void onMotion(MotionEvent event) {
        // post 阶段说明本次移动包已经发出，服务端那边已经开启了一个新的窗口
        if (event.isPost()) {
            used = false;
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        used = false;
    }
}
