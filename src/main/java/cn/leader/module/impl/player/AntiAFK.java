package cn.leader.module.impl.player;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.MoveInputEvent;
import cn.leader.event.impl.TickEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.util.misc.TimerUtil;

/**
 * Small periodic movement so an idle session does not get kicked for inactivity.
 *
 * <p>Everything here goes through normal input and normal look packets, so it is indistinguishable
 * from a player wiggling the mouse and tapping space.</p>
 */
public final class AntiAFK extends Module {

    private final NumberValue interval = new NumberValue("Interval", 1800, 200, 8000, 100);
    private final NumberValue yawRange = new NumberValue("Yaw Range", 25, 1, 90, 1);
    private final BoolValue rotate = new BoolValue("Rotate", true);
    private final BoolValue jump = new BoolValue("Jump", true);
    private final BoolValue sneak = new BoolValue("Sneak", false);

    private final TimerUtil timer = new TimerUtil();

    private boolean jumpNextTick;
    private int sneakTicks;

    public AntiAFK() {
        super("AntiAFK", Category.Player);
        setDescription("Periodic movement so servers keep you online");
    }

    @Override
    public void onEnable() {
        timer.reset();
        jumpNextTick = false;
        sneakTicks = 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (sneakTicks > 0 && --sneakTicks == 0) {
            mc.options.keyShift.setDown(false);
        }

        if (!timer.hasTimeElapsed(interval.getValue().longValue())) return;
        timer.reset();

        if (rotate.getValue()) {
            float delta = (float) ((Math.random() * 2.0D - 1.0D) * yawRange.getValue());
            mc.player.setYRot(mc.player.getYRot() + delta);
        }

        if (jump.getValue() && mc.player.onGround()) {
            jumpNextTick = true;
        }

        if (sneak.getValue() && Math.random() < 0.35D) {
            sneakTicks = 4;
            mc.options.keyShift.setDown(true);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!jumpNextTick) return;

        event.setJumping(true);
        jumpNextTick = false;
    }

    @Override
    public void onDisable() {
        jumpNextTick = false;
        sneakTicks = 0;

        if (mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }
}
