package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class MoveInputEvent extends Event {
    private float forward;
    private float strafe;
    private boolean jumping;
    private boolean sneaking;
    private boolean sprint;

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

    /**
     * The sprint key state that goes into {@code Input.sprint}. {@code LocalPlayer#aiStep} re-enables
     * sprinting every tick while this is true, so a module that needs the player to genuinely stop
     * sprinting for one tick has to clear it here, not just call {@code setSprinting(false)}.
     */
    public boolean isSprint() {
        return this.sprint;
    }

    public void setSprint(boolean value) {
        this.sprint = value;
    }

    public MoveInputEvent(float forward, float strafe, boolean jumping, boolean sneaking) {
        this(forward, strafe, jumping, sneaking, false);
    }

    public MoveInputEvent(float forward, float strafe, boolean jumping, boolean sneaking, boolean sprint) {
        this.forward = forward;
        this.strafe = strafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
        this.sprint = sprint;
    }

}
