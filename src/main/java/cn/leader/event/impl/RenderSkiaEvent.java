package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.skija.Canvas;

@Setter
@Getter
@AllArgsConstructor
public class RenderSkiaEvent extends Event {
    private final Canvas canvas;
}
