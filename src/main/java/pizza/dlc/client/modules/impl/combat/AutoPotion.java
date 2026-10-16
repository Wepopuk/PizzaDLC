package pizza.dlc.client.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AutoPotion",
   category = ModuleCategory.COMBAT,
   description = "Кидает сплэш-зелья выбранных эффектов (макс. уровень) себе под ноги"
)
public class AutoPotion extends Module {
   private static final String FIRE_RESISTANCE = "Огнестойкость";
   private static final String STRENGTH = "Сила";
   private static final String SPEED = "Скорость";
   public final MultiModeSetting effects = new MultiModeSetting("Эффекты", this, FIRE_RESISTANCE, STRENGTH, SPEED);
   public final BooleanSetting auto = new BooleanSetting("Авто-поддержка", this);
   public final SliderSetting threshold = new SliderSetting("Порог (сек)", this, 0.0F, 0.0F, 60.0F, 1.0F);
   public final SliderSetting delay = new SliderSetting("Задержка (мс)", this, 1000.0F, 200.0F, 5000.0F, 100.0F);
   public final BooleanSetting groundOnly = new BooleanSetting("Только на земле", this);
   public final BindSetting bind = new BindSetting("Бинд (ручной)", this, -1);
   private long lastThrow;
   private boolean bindPending;

   public AutoPotion() {
      this.auto.setEnabled(true);
      this.groundOnly.setEnabled(true);
      this.threshold.setVisible(this.auto::isEnabled);
      this.delay.setVisible(this.auto::isEnabled);
   }

   private static Holder<MobEffect> effectFor(String name) {
      return switch (name) {
         case FIRE_RESISTANCE -> MobEffects.FIRE_RESISTANCE;
         case STRENGTH -> MobEffects.STRENGTH;
         case SPEED -> MobEffects.SPEED;
         default -> null;
      };
   }

   @EventHandler
   public void onKey(EventKey e) {
      if (e.getAction() != 1 || mc.gui.screen() != null) return;
      if (this.bind.getKey() != -1 && this.bind.getKey() == e.getKey()) {
         this.bindPending = true;
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;
      boolean manual = this.bindPending;
      this.bindPending = false;
      long now = System.currentTimeMillis();
      if (!manual && !this.autoAllowed(now)) return;

      for (BooleanSetting b : this.effects.getBoolSettings()) {
         if (!b.isEnabled()) continue;
         Holder<MobEffect> eff = effectFor(b.getName());
         if (eff == null || !this.needsThrow(eff, manual)) continue;
         int slot = this.findBestPotion(eff);
         if (slot == -1) continue;
         SwapSystem.useItemFromSlot(slot, 90.0F);
         this.lastThrow = now;
         return;
      }
   }

   private boolean autoAllowed(long now) {
      if (!this.auto.isEnabled() || now - this.lastThrow < (long)this.delay.getValue()) return false;
      if (mc.player.isUsingItem() || CombatSync.userBusy() || mc.gui.screen() != null) return false;
      if (mc.player.containerMenu != mc.player.inventoryMenu) return false;
      return !this.groundOnly.isEnabled() || mc.player.onGround();
   }

   private boolean needsThrow(Holder<MobEffect> eff, boolean manual) {
      MobEffectInstance cur = mc.player.getEffect(eff);
      if (cur == null) return true;
      if (manual) return false;
      float sec = this.threshold.getValue();
      return sec > 0.0F && cur.getDuration() < sec * 20.0F;
   }

   private int findBestPotion(Holder<MobEffect> eff) {
      NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
      int best = -1;
      int bestAmp = -1;
      for (int i = 0; i < main.size(); i++) {
         ItemStack st = main.get(i);
         if (st.getItem() != Items.SPLASH_POTION) continue;
         int amp = potionAmplifier(st, eff);
         if (amp > bestAmp) {
            bestAmp = amp;
            best = i < 9 ? i + 36 : i;
         }
      }
      return best;
   }

   private static int potionAmplifier(ItemStack st, Holder<MobEffect> eff) {
      PotionContents pc = st.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      int amp = -1;
      for (MobEffectInstance mi : pc.getAllEffects()) {
         if (!mi.getEffect().equals(eff)) continue;

         amp = Math.max(amp, mi.getAmplifier());
      }
      return amp;
   }
}
