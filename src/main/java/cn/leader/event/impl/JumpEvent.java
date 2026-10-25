package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class JumpEvent extends Event {
    private float yaw;

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float value) {
        this.yaw = value;
    }

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

}
