package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class LookEvent extends Event {
    private float[] rotation;
    private float[] lastRotation;

    public float[] getRotation() {
        return this.rotation;
    }

    public void setRotation(float[] value) {
        this.rotation = value;
    }

    public float[] getLastRotation() {
        return this.lastRotation;
    }

    public void setLastRotation(float[] value) {
        this.lastRotation = value;
    }

    public LookEvent(float[] rotation, float[] lastRotation) {
        this.rotation = rotation;
        this.lastRotation = lastRotation;
    }

}
