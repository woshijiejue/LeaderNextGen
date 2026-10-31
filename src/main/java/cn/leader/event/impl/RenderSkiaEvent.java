package cn.leader.event.impl;

import cn.leader.event.base.Event;
import org.jetbrains.skija.Canvas;

public class RenderSkiaEvent extends Event {
    public enum Layer {
        HUD, SCREEN
    }

    private final Canvas canvas;
    private final Layer layer;

    public Canvas getCanvas() {
        return this.canvas;
    }

    public Layer getLayer() {
        return this.layer;
    }

    public RenderSkiaEvent(Canvas canvas, Layer layer) {
        this.canvas = canvas;
        this.layer = layer;
    }

}
