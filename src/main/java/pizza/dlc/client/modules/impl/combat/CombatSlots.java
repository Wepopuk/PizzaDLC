package pizza.dlc.client.modules.impl.combat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class CombatSlots {
   private static int userSlot = -1;
   private static int moduleSlot = -1;
   private static final Map<Object, Long> HOLDS = new HashMap<>();

   private CombatSlots() {
   }

   public static void hold(Object owner, int ticks) {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p == null) return;
      if (userSlot == -1) userSlot = p.getInventory().getSelectedSlot();
      HOLDS.put(owner, System.currentTimeMillis() + ticks * 50L);
   }

   public static void select(Object owner, int slot, int ticks) {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p == null) return;
      hold(owner, ticks);
      if (p.getInventory().getSelectedSlot() != slot) p.getInventory().setSelectedSlot(slot);
      moduleSlot = slot;
   }

   public static void tick() {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p == null) {
         reset();
         return;
      }
      if (userSlot == -1) return;
      if (moduleSlot != -1 && p.getInventory().getSelectedSlot() != moduleSlot) {
         userSlot = -1;
         moduleSlot = -1;
         HOLDS.clear();
         return;
      }
      long now = System.currentTimeMillis();
      HOLDS.values().removeIf(t -> t <= now);
      if (HOLDS.isEmpty()) {
         if (p.getInventory().getSelectedSlot() != userSlot) p.getInventory().setSelectedSlot(userSlot);
         userSlot = -1;
         moduleSlot = -1;
      }
   }

   public static void release(Object owner) {
      HOLDS.remove(owner);
      tick();
   }

   public static void restoreNow() {
      HOLDS.clear();
      tick();
   }

   public static void reset() {
      userSlot = -1;
      moduleSlot = -1;
      HOLDS.clear();
   }
}
