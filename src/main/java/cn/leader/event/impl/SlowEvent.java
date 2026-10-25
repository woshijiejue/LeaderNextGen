package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class SlowEvent extends Event {
    private float sideways;
    private float forward;

    public float getSideways() {
        return this.sideways;
    }

    public void setSideways(float value) {
        this.sideways = value;
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float value) {
        this.forward = value;
    }

    public SlowEvent(float sideways, float forward) {
        this.sideways = sideways;
        this.forward = forward;
    }

}
