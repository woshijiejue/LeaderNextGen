package cn.leader.event.impl;

import cn.leader.event.base.Event;
import net.minecraft.world.entity.Entity;

public class RotationEvent extends Event {
   public static Entity currentEntity;
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

    public RotationEvent(float[] rotation, float[] lastRotation) {
        this.rotation = rotation;
        this.lastRotation = lastRotation;
    }

}
