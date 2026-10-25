package cn.leader.event.impl;

import cn.leader.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

@Getter
@Setter
@AllArgsConstructor
public class RotationEvent extends Event {
   public static Entity currentEntity;
   private float[] rotation;
   private float[] lastRotation;
}
