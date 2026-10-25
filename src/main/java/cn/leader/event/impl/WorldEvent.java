package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.world.ClientWorld;

@Getter
@Setter
@AllArgsConstructor
public class WorldEvent extends Event {
    public final ClientWorld world;
}
