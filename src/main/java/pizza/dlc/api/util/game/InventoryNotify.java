package pizza.dlc.api.util.game;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InventoryNotify {
   private static final long COOLDOWN_MS = 2500L;
   private static final Map<String, Long> lastShown = new HashMap<>();
   private static final Map<Item, String> RU_NAMES = new HashMap<>();

   private InventoryNotify() {
   }

   public static void missing(Item item) {
      String ru = RU_NAMES.get(item);
      missing(ru != null ? ru : new ItemStack(item).getHoverName().getString());
   }

   public static void missing(String itemName) {
      long now = System.currentTimeMillis();
      Long last = lastShown.get(itemName);
      if (last == null || now - last >= COOLDOWN_MS) {
         lastShown.put(itemName, now);
         ChatUtility.sendError(itemName + " нет в инвентаре!");
      }
   }

   static {
      RU_NAMES.put(Items.COBWEB, "Паутины");
      RU_NAMES.put(Items.FIREWORK_ROCKET, "Фейерверка");
      RU_NAMES.put(Items.ENDER_PEARL, "Эндер-жемчуга");
      RU_NAMES.put(Items.EXPERIENCE_BOTTLE, "Бутылька опыта");
      RU_NAMES.put(Items.SNOWBALL, "Снежка");
      RU_NAMES.put(Items.EGG, "Яйца");
      RU_NAMES.put(Items.WIND_CHARGE, "Заряда ветра");
      RU_NAMES.put(Items.TOTEM_OF_UNDYING, "Тотема бессмертия");
      RU_NAMES.put(Items.SHIELD, "Щита");
      RU_NAMES.put(Items.GOLDEN_APPLE, "Золотого яблока");
      RU_NAMES.put(Items.ELYTRA, "Элитры");
      RU_NAMES.put(Items.OBSIDIAN, "Обсидиана");
      RU_NAMES.put(Items.END_CRYSTAL, "Кристалла Края");
   }
}
