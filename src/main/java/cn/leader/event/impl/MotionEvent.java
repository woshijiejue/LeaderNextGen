package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class MotionEvent extends Event.StateEvent {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean horizontalCollision;

    public double getX() {
        return this.x;
    }

    public void setX(double value) {
        this.x = value;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double value) {
        this.y = value;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double value) {
        this.z = value;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float value) {
        this.yaw = value;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float value) {
        this.pitch = value;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void setOnGround(boolean value) {
        this.onGround = value;
    }

    public boolean isHorizontalCollision() {
        return this.horizontalCollision;
    }

    public void setHorizontalCollision(boolean value) {
        this.horizontalCollision = value;
    }

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
    }


    public float getYRot() {
        return this.yaw;
    }

    public float getXRot() {
        return this.pitch;
    }

    public void setYRot(float yaw) {
        this.yaw = yaw;
    }

    public void setXRot(float pitch) {
        this.pitch = pitch;
    }
}
