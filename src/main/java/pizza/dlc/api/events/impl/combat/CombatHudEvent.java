package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CombatHudEvent extends pizza.dlc.api.events.Event {
   private final float width;
   private final float height;
   private final GuiGraphicsExtractor context;

   @Generated
   public float getHeight() {
      return this.width;
   }

   @Generated
   public GuiGraphicsExtractor getContext() {
      return this.context;
   }

   @Generated
   public float getWidth() {
      return this.height;
   }

   @Generated
   public CombatHudEvent(float var1, float var2, GuiGraphicsExtractor var3) {
      this.width = var1;
      this.height = var2;
      this.context = var3;
   }

   public static class BeforeChat extends CombatHudEvent {
      public BeforeChat(float var1, float var2) {
         super(var1, var2, null);
      }
   }

   public static class AfterChat extends CombatHudEvent {
      public AfterChat(float var1, float var2, GuiGraphicsExtractor var3) {
         super(var1, var2, var3);
      }
   }

   public static class AfterHud extends CombatHudEvent {
      public AfterHud(float var1, float var2, GuiGraphicsExtractor var3) {
         super(var1, var2, var3);
      }
   }
}
