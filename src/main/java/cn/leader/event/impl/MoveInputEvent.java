package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class MoveInputEvent extends Event {
    private float forward;
    private float strafe;
    private boolean jumping;
    private boolean sneaking;

    public float getForward() {
        return this.forward;
    }

    public void setForward(float value) {
        this.forward = value;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public void setStrafe(float value) {
        this.strafe = value;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public void setJumping(boolean value) {
        this.jumping = value;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(boolean value) {
        this.sneaking = value;
    }

    public MoveInputEvent(float forward, float strafe, boolean jumping, boolean sneaking) {
        this.forward = forward;
        this.strafe = strafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

}