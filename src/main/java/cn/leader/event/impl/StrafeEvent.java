package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class StrafeEvent extends Event {
    private float yaw;

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float value) {
        this.yaw = value;
    }

    public StrafeEvent(float yaw) {
        this.yaw = yaw;
    }

}
