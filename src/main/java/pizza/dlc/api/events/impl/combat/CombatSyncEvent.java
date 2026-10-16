package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import net.minecraft.world.phys.Vec3;

public class CombatSyncEvent extends pizza.dlc.api.events.Event {
   private Vec3 f_24793;
   private float f_24794;
   private float f_24795;
   private boolean f_24796;

   @Generated
   public void m_7393(boolean var1) {
      this.f_24796 = var1;
   }

   @Generated
   public boolean m_7394() {
      return this.f_24796;
   }

   @Generated
   public CombatSyncEvent(Vec3 var1, float var2, float var3, boolean var4) {
      this.f_24793 = var1;
      this.f_24794 = var2;
      this.f_24795 = var3;
      this.f_24796 = var4;
   }

   @Generated
   public void m_7395(float var1) {
      this.f_24794 = var1;
   }

   @Generated
   public float m_7396() {
      return this.f_24794;
   }

   @Generated
   public float m_7397() {
      return this.f_24795;
   }

   @Generated
   public void m_7398(Vec3 var1) {
      this.f_24793 = var1;
   }

   @Generated
   public Vec3 m_7399() {
      return this.f_24793;
   }

   @Generated
   public void m_7400(float var1) {
      this.f_24795 = var1;
   }

   public static class PostSync extends CombatSyncEvent {
      public PostSync(Vec3 var1, float var2, float var3, boolean var4) {
         super(var1, var2, var3, var4);
      }
   }
}
