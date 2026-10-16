package pizza.dlc.api.events.impl.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import pizza.dlc.api.events.Event;

public class EventMotion extends Event {
   public static float lastYaw;
   public static float lastPitch;
   private double x;
   private double y;
   private double z;
   private float yaw;
   private float pitch;
   private boolean onGround;
   private boolean sneaking;
   private boolean sprinting;
   private boolean forceFlying;
   private boolean rotationOnly;
   private final List<Packet<?>> replacement = new ArrayList<>();
   private final List<Runnable> postActions = new ArrayList<>();

   public EventMotion(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean sneaking, boolean sprinting) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.onGround = onGround;
      this.sneaking = sneaking;
      this.sprinting = sprinting;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
      lastYaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
      lastPitch = pitch;
   }

   public void addPostAction(Runnable action) {
      this.postActions.add(action);
   }

   public void executePostActions() {
      for (Runnable action : this.postActions) {
         action.run();
      }

      this.postActions.clear();
   }

   public boolean isGround() {
      return this.onGround;
   }

   public void setGround(boolean ground) {
      this.onGround = ground;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public boolean isForceFlying() {
      return this.forceFlying;
   }

   public List<Runnable> getPostActions() {
      return this.postActions;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   public void setSneaking(boolean sneaking) {
      this.sneaking = sneaking;
   }

   public void setSprinting(boolean sprinting) {
      this.sprinting = sprinting;
   }

   public void setForceFlying(boolean forceFlying) {
      this.forceFlying = forceFlying;
   }

   public boolean isRotationOnly() {
      return this.rotationOnly;
   }

   public void setRotationOnly(boolean rotationOnly) {
      this.rotationOnly = rotationOnly;
   }

   public List<Packet<?>> getReplacement() {
      return this.replacement;
   }

   public void replaceWith(Packet<?>... packets) {
      this.setCancelled(true);
      this.replacement.clear();
      this.replacement.addAll(List.of(packets));
   }
}
