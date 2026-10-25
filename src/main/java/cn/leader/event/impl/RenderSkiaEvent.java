package cn.leader.event.impl;

import cn.leader.event.base.Event;
import org.jetbrains.skija.Canvas;

public class RenderSkiaEvent extends Event {
    private final Canvas canvas;

    public Canvas getCanvas() {
        return this.canvas;
    }

    public RenderSkiaEvent(Canvas canvas) {
        this.canvas = canvas;
    }

}
