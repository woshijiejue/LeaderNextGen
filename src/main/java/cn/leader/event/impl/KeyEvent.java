package cn.leader.event.impl;

import cn.leader.event.base.Event;

public class KeyEvent extends Event {
    private int key;

    public int getKey() {
        return this.key;
    }

    public void setKey(int value) {
        this.key = value;
    }

    public KeyEvent(int key) {
        this.key = key;
    }

}
