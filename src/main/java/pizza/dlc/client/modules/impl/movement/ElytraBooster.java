package pizza.dlc.client.modules.impl.movement;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.client.modules.impl.combat.AttackAura;

import com.google.gson.JsonObject;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.api.util.render.combat.CombatColors;
import pizza.dlc.api.util.render.combat.CombatHudText;
import pizza.dlc.api.events.impl.combat.RocketBoostEvent;
import pizza.dlc.api.events.impl.combat.CombatHudEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class ElytraBooster extends Module {

   public final ModeSetting serverProfile;
   public final BooleanSetting showAngles;
   public final CombatSliderSetting yawBoost0To5;
   public final CombatSliderSetting yawBoost5To10;
   public final CombatSliderSetting yawBoost10To15;
   public final CombatSliderSetting yawBoost15To20;
   public final CombatSliderSetting yawBoost20To25;
   public final CombatSliderSetting yawBoost25To30;
   public final CombatSliderSetting yawBoost30To35;
   public final CombatSliderSetting yawBoost35To40;
   public final CombatSliderSetting yawBoost40To45;
   public final CombatSliderSetting pitchBoost0To5;
   public final CombatSliderSetting pitchBoost5To10;
   public final CombatSliderSetting pitchBoost10To15;
   public final CombatSliderSetting pitchBoost15To20;
   public final CombatSliderSetting pitchBoost20To25;
   public final CombatSliderSetting pitchBoost25To30;
   public final CombatSliderSetting pitchBoost30To35;
   public final CombatSliderSetting pitchBoost35To40;
   public final CombatSliderSetting pitchBoost40To45;
   private final ElytraBooster.DigitAnimation yawDigits;
   private final ElytraBooster.DigitAnimation pitchDigits;

   private static final double f_14198 = Math.PI;

   @Override
   public void load(JsonObject var1) {
      if (var1 != null && var1.has("Settings") && var1.get("Settings").isJsonObject()) {
         var1 = var1.deepCopy();
         JsonObject var2 = var1.getAsJsonObject("Settings");
         if (var2.has(this.serverProfile.getName())
            && var2.get(this.serverProfile.getName()).isJsonPrimitive()
            && "ReallyWorld".equals(var2.get(this.serverProfile.getName()).getAsString())) {
            var2.addProperty(this.serverProfile.getName(), "ReallyWorld 1.16.5");
         }

         String var3 = this.serverProfile.getName() + "";
         if (var2.has(var3) && var2.get(var3).isJsonObject()) {
            JsonObject var4 = var2.getAsJsonObject(var3);
            if (var4.has("ReallyWorld") && !var4.has("ReallyWorld 1.16.5")) {
               var4.add("ReallyWorld 1.16.5", var4.get("ReallyWorld"));
            }
         }
      }

      super.load(var1);
   }

   public ElytraBooster() {
      super("ElytraBooster", "Усиливает ускорение от фейерверков при полёте на элитрах", ModuleCategory.MOVEMENT);
      this.serverProfile = new ModeSetting("Мод", this, "ReallyWorld 1.16.5", "ReallyWorld 1.21.11", "BravoHvH", "Custom");
      this.showAngles = CombatSettings.bool("Рисовать текущий Yaw или Pitch", this, false);

      this.yawBoost0To5 = new CombatSliderSetting("0-5 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost5To10 = new CombatSliderSetting("5-10 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost10To15 = new CombatSliderSetting("10-15 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost15To20 = new CombatSliderSetting("15-20 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost20To25 = new CombatSliderSetting("20-25 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost25To30 = new CombatSliderSetting("25-30 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost30To35 = new CombatSliderSetting("30-35 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost35To40 = new CombatSliderSetting("35-40 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawBoost40To45 = new CombatSliderSetting("40-45 По X", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));

      this.pitchBoost0To5 = new CombatSliderSetting("0-5 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost5To10 = new CombatSliderSetting("5-10 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost10To15 = new CombatSliderSetting("10-15 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost15To20 = new CombatSliderSetting("15-20 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost20To25 = new CombatSliderSetting("20-25 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost25To30 = new CombatSliderSetting("25-30 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost30To35 = new CombatSliderSetting("30-35 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost35To40 = new CombatSliderSetting("35-40 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.pitchBoost40To45 = new CombatSliderSetting("40-45 По Y", this, 1.5F, 1.5F, 2.5F).withStep(0.01F).setVisible(() -> this.serverProfile.is("Custom"));
      this.yawDigits = new ElytraBooster.DigitAnimation();
      this.pitchDigits = new ElytraBooster.DigitAnimation();

      for (CombatSliderSetting slider : new CombatSliderSetting[]{this.yawBoost0To5, this.yawBoost5To10, this.yawBoost10To15, this.yawBoost15To20, this.yawBoost20To25, this.yawBoost25To30, this.yawBoost30To35, this.yawBoost35To40, this.yawBoost40To45}) slider.group("Бустер по X");
      for (CombatSliderSetting slider : new CombatSliderSetting[]{this.pitchBoost0To5, this.pitchBoost5To10, this.pitchBoost10To15, this.pitchBoost15To20, this.pitchBoost20To25, this.pitchBoost25To30, this.pitchBoost30To35, this.pitchBoost35To40, this.pitchBoost40To45}) slider.group("Бустер по Y");
      this.getSettings().clear();
      this.addSettings(
         this.serverProfile,

         this.yawBoost0To5,
         this.yawBoost5To10,
         this.yawBoost10To15,
         this.yawBoost15To20,
         this.yawBoost20To25,
         this.yawBoost25To30,
         this.yawBoost30To35,
         this.yawBoost35To40,
         this.yawBoost40To45,

         this.pitchBoost0To5,
         this.pitchBoost5To10,
         this.pitchBoost10To15,
         this.pitchBoost15To20,
         this.pitchBoost20To25,
         this.pitchBoost25To30,
         this.pitchBoost30To35,
         this.pitchBoost35To40,
         this.pitchBoost40To45,
         this.showAngles
      );
   }

   private float m_5038(String var1) {
      return CombatHudText.width(var1, 20.0F, 0.0F);
   }

   public float horizontalBoost() {
      RocketBoostEvent var1 = new RocketBoostEvent(1.5F, 1.5F, 1.5F);
      this.onRocketBoost(var1);
      return var1.getXPower();
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onHud(CombatHudEvent var1) {
      if (this.showAngles.isEnabled() && mc.player != null && mc.level != null) {
         int var2 = (int)this.m_5049(mc.player.getYRot());
         int var3 = (int)this.m_5049(mc.player.getXRot());
         this.yawDigits.m_5025(var2);
         this.pitchDigits.m_5025(var3);
         float var4 = mc.getWindow().getScreenWidth() / 2.0F;
         float var5 = mc.getWindow().getScreenHeight() / 2.0F;

         this.m_5051("Yaw: ", this.yawDigits, var4, var5 + 40.0F);
         this.m_5051("Pitch: ", this.pitchDigits, var4, var5 + 70.0F);

      }
   }

   private float m_5049(float var1) {
      float var2 = var1 % 180.0F;
      if (var2 > 90.0F) {
         var2 -= 180.0F;
      } else if (var2 < -90.0F) {
         var2 += 180.0F;
      }

      return var2;
   }

   private void m_5051(String var1, ElytraBooster.DigitAnimation var2, float var3, float var4) {
      String var5 = var2.m_5023();
      String var6 = var1 + var5;
      float var7 = var3 - CombatHudText.width(var6, 20.0F, 1.05F) / 2.0F;
      int var8 = CombatColors.m_2310(235, 235, 235);
      int var9 = pizza.dlc.api.theme.ThemeManager.accent();
      CombatHudText.draw(var1, var7, var4, 20, var8);
      float var10 = var7 + this.m_5038(var1);
      long var11 = System.nanoTime();

      for (int var13 = 0; var13 < var2.f_14045.size(); var13++) {
         ElytraBooster.DigitState var14 = var2.f_14045.get(var13);
         float var15 = var10 + this.m_5038(var5.substring(0, var13));
         float var16 = var14.m_5027(var11);
         float var17 = 1.0F - (float)Math.pow(1.0F - var16, 3.0);
         if (var16 < 1.0F) {
            CombatHudText.draw(var14.f_14048, var15, var4 + var17 * 6.0F, 20, CombatColors.m_2306(var8, 1.0F - var17)
            );
            float var18 = (float)Math.sin(var16 * f_14198);
            int var19 = CombatColors.m_2312(var8, var9, var18 * 0.28F);
            CombatHudText.draw(var14.f_14049, var15, var4 - 6.0F + var17 * 6.0F, 20, CombatColors.m_2306(var19, var17)
            );
         } else {
            CombatHudText.draw(var14.f_14049, var15, var4, 20, var8);
         }
      }

      float var20 = var10 + this.m_5038(var5);
      CombatHudText.draw("°", var20, var4, 20, var8);
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onRocketBoost(RocketBoostEvent var1) {
      ElytraTarget var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
      LivingEntity var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
      float var4 = Mth.wrapDegrees(mc.player.getYRot());
      float var5 = mc.player.getXRot();
      float var6 = this.m_5049(var4);
      float var7 = Math.abs(var6);
      float var8 = 1.5F;
      float var9 = 1.5F;
      if (var7 > 45.0F) {
         var7 = 90.0F - var7;
      }

      float var10 = this.m_5049(var5);
      float var11 = Math.abs(var10);
      if (var11 > 45.0F) {
         var11 = 90.0F - var11;
      }

      if (this.serverProfile.is("ReallyWorld 1.16.5")) {
         if (var7 <= 5.0F && var11 <= 5.0F) {
            var8 = 1.6F;
         } else if (var7 <= 10.0F && var11 <= 10.0F) {
            var8 = 1.63F;
         } else if (var7 <= 15.0F && var11 <= 15.0F) {
            var8 = 1.66F;
         } else if (var7 <= 20.0F && var11 <= 20.0F) {
            var8 = 1.71F;
         } else if (var7 <= 25.0F && var11 <= 25.0F) {
            var8 = 1.73F;
         } else if (var7 <= 30.0F && var11 <= 30.0F) {
            var8 = 1.81F;
         } else if (var7 <= 35.0F && var11 <= 35.0F) {
            var8 = 1.81F;
         } else if (var7 <= 40.0F && var11 <= 40.0F) {
            var8 = 1.81F;
         } else if (var7 <= 45.0F || var11 <= 45.0F) {
            var8 = 1.81F;
         }

         if (var11 <= 5.0F) {
            var9 = 1.6F;
         } else if (var11 <= 10.0F) {
            var9 = 1.62F;
         } else if (var11 <= 15.0F) {
            var9 = 1.6F;
         } else if (var11 <= 20.0F) {
            var9 = 1.62F;
         } else if (var11 <= 25.0F) {
            var9 = 1.64F;
         } else if (var11 <= 30.0F) {
            var9 = 1.68F;
         } else if (var11 <= 35.0F) {
            var9 = 1.75F;
         } else if (var11 <= 40.0F) {
            var9 = 1.9F;
         } else if (var11 >= 43.0F && var11 <= 45.0F) {
            var9 = 2.26F;
         } else {
            var9 = 2.16F;
         }

         if (var9 > var8) {
            var8 = var9;
         }
      }

      if (this.serverProfile.is("ReallyWorld 1.21.11")) {
         if (var7 <= 30.0F && var11 <= 30.0F) {
            var8 = 1.53F;
         } else if (var7 <= 40.0F && var11 <= 40.0F) {
            var8 = 2.04F;
         } else {
            var8 = 2.059F;
         }

         var9 = var11 <= 30.0F ? 1.53F : 2.02F;
         var8 = Math.max(var8, var9);
      }

      if (this.serverProfile.is("BravoHvH")) {
         if (var7 <= 5.0F && var11 <= 5.0F) {
            var8 = 1.64F;
         } else if (var7 <= 10.0F && var11 <= 10.0F) {
            var8 = 1.67F;
         } else if (var7 <= 15.0F && var11 <= 15.0F) {
            var8 = 1.68F;
         } else if (var7 <= 20.0F && var11 <= 20.0F) {
            var8 = 1.73F;
         } else if (var7 <= 25.0F && var11 <= 25.0F) {
            var8 = 1.77F;
         } else if (var7 <= 30.0F && var11 <= 30.0F) {
            var8 = 1.87F;
         } else if (var7 <= 35.0F && var11 <= 35.0F) {
            var8 = 2.01F;
         } else if (var7 <= 40.0F && var11 <= 40.0F) {
            var8 = 2.05F;
         } else if (var7 <= 45.0F || var11 <= 45.0F) {
            var8 = 2.05F;
         }

         if (var11 <= 5.0F) {
            var9 = 1.65F;
         } else if (var11 <= 10.0F) {
            var9 = 1.7F;
         } else if (var11 <= 15.0F) {
            var9 = 1.71F;
         } else if (var11 <= 20.0F) {
            var9 = 1.74F;
         } else if (var11 <= 25.0F) {
            var9 = 1.79F;
         } else if (var11 <= 30.0F) {
            var9 = 1.91F;
         } else if (var11 <= 35.0F) {
            var9 = 2.01F;
         } else if (var11 <= 40.0F) {
            var9 = 2.02F;
         } else {
            var9 = 2.02F;
         }
      }

      if (this.serverProfile.is("Custom")) {
         if (var7 <= 5.0F && var11 <= 5.0F) {
            var8 = this.yawBoost0To5.getValue();
         } else if (var7 <= 10.0F && var11 <= 10.0F) {
            var8 = this.yawBoost5To10.getValue();
         } else if (var7 <= 15.0F && var11 <= 15.0F) {
            var8 = this.yawBoost10To15.getValue();
         } else if (var7 <= 20.0F && var11 <= 20.0F) {
            var8 = this.yawBoost15To20.getValue();
         } else if (var7 <= 25.0F && var11 <= 25.0F) {
            var8 = this.yawBoost20To25.getValue();
         } else if (var7 <= 30.0F && var11 <= 30.0F) {
            var8 = this.yawBoost25To30.getValue();
         } else if (var7 <= 35.0F && var11 <= 35.0F) {
            var8 = this.yawBoost30To35.getValue();
         } else if (var7 <= 40.0F && var11 <= 40.0F) {
            var8 = this.yawBoost35To40.getValue();
         } else if (var7 <= 45.0F || var11 <= 45.0F) {
            var8 = this.yawBoost40To45.getValue();
         }

         if (var11 <= 5.0F) {
            var9 = this.pitchBoost0To5.getValue();
         } else if (var11 <= 10.0F) {
            var9 = this.pitchBoost5To10.getValue();
         } else if (var11 <= 15.0F) {
            var9 = this.pitchBoost10To15.getValue();
         } else if (var11 <= 20.0F) {
            var9 = this.pitchBoost15To20.getValue();
         } else if (var11 <= 25.0F) {
            var9 = this.pitchBoost20To25.getValue();
         } else if (var11 <= 30.0F) {
            var9 = this.pitchBoost25To30.getValue();
         } else if (var11 <= 35.0F) {
            var9 = this.pitchBoost30To35.getValue();
         } else if (var11 <= 40.0F) {
            var9 = this.pitchBoost35To40.getValue();
         } else {
            var9 = this.pitchBoost40To45.getValue();
         }

         if (var9 > var8) {
            var8 = var9;
         }
      }

      var1.setZPower(var8);
      var1.setXPower(var9);
      var1.setYPower(var8);
   }

   private static final class DigitAnimation {
      public final List<ElytraBooster.DigitState> f_14045 = new ArrayList<>();
      public String f_14046 = "";
      public Integer f_14047;

      public boolean m_5021() {
         long var1 = System.nanoTime();

         for (ElytraBooster.DigitState var4 : this.f_14045) {
            if (var4.m_5027(var1) < 1.0F) {
               return true;
            }
         }

         return false;
      }

      public void m_5022(String var1) {
         if (var1.length() != this.f_14045.size()) {
            this.f_14046 = var1;
            this.m_5024(var1);
         } else {
            long var2 = System.nanoTime();

            for (int var4 = 0; var4 < var1.length(); var4++) {
               this.f_14045.get(var4).m_5026(var1.charAt(var4), var2);
            }

            this.f_14046 = var1;
         }
      }

      public String m_5023() {
         return this.f_14046;
      }

      public DigitAnimation() {
      }

      public void m_5024(String var1) {
         this.f_14045.clear();

         for (int var2 = 0; var2 < var1.length(); var2++) {
            this.f_14045.add(new ElytraBooster.DigitState(var1.charAt(var2)));
         }
      }

      public void m_5025(int var1) {
         String var2 = Integer.toString(var1);
         if (this.f_14046.isEmpty()) {
            this.f_14046 = var2;
            this.m_5024(var2);
         } else {
            this.f_14047 = this.f_14046.equals(var2) ? null : var1;
            if (!this.m_5021() && this.f_14047 != null) {
               String var3 = Integer.toString(this.f_14047);
               this.f_14047 = null;
               this.m_5022(var3);
            }
         }
      }
   }

   private static final class DigitState {
      public String f_14048;
      public String f_14049;
      public long f_14050;

      public void m_5026(char var1, long var2) {
         String var4 = Character.toString(var1);
         if (!this.f_14049.equals(var4)) {
            this.f_14048 = this.f_14049;
            this.f_14049 = var4;
            this.f_14050 = var2;
         }
      }

      public float m_5027(long var1) {
         if (this.f_14050 == 0L) {
            return 1.0F;
         }

         float var3 = (float)(var1 - this.f_14050) / 1.9E8F;
         return Math.max(0.0F, Math.min(1.0F, var3));
      }

      public DigitState(char var1) {
         this.f_14048 = Character.toString(var1);
         this.f_14049 = this.f_14048;
         this.f_14050 = 0L;
      }
   }

   public void resetState() {}

   @Override public void onEnable() {
      super.onEnable();
      CombatRuntime.enabled(this);
   }
   @Override public void onDisable() {
      try { resetState(); } finally { CombatRuntime.disabled(this); super.onDisable(); }
   }
}
