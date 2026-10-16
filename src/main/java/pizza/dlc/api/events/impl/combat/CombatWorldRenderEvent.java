package pizza.dlc.api.events.impl.combat;
import pizza.dlc.api.util.render.combat.CombatDrawQueue;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Generated;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CombatWorldRenderEvent extends pizza.dlc.api.events.Event {
   private final DeltaTracker deltaTracker;
   private final PoseStack pose;
   private final CombatDrawQueue buffer;
   private final Vec3 cameraPosition;
   private final float partialTick;
   private final Matrix4f projection;
   private final Matrix4f view;

   @Generated
   public float getPartialTick() {
      return this.partialTick;
   }

   @Generated
   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   @Generated
   public PoseStack getPose() {
      return this.pose;
   }

   @Generated
   public Matrix4f getProjection() {
      return this.view;
   }

   @Generated
   public CombatWorldRenderEvent(DeltaTracker var1, PoseStack var2, CombatDrawQueue var3, Vec3 var4, float var5, Matrix4f var6, Matrix4f var7) {
      this.deltaTracker = var1;
      this.pose = var2;
      this.buffer = var3;
      this.cameraPosition = var4;
      this.partialTick = var5;
      this.projection = var6;
      this.view = var7;
   }

   @Generated
   public CombatDrawQueue getBuffer() {
      return this.buffer;
   }

   @Generated
   public Matrix4f getView() {
      return this.projection;
   }

   @Generated
   public Vec3 getCameraPosition() {
      return this.cameraPosition;
   }
}
