package pizza.dlc.client.modules.impl.player;

import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "NoPush",
   description = "Отключает выталкивание (сущности/жидкости/блоки)",
   category = ModuleCategory.PLAYER
)
public class NoPush extends Module {
   public static NoPush INSTANCE;
   private static final String ENTITIES = "Сущности";
   private static final String FLUIDS = "Жидкости";
   private static final String BLOCKS = "Блоки";
   private final MultiModeSetting removePushFrom = new MultiModeSetting("Отключить от", this, ENTITIES, FLUIDS, BLOCKS);

   public NoPush() {
      INSTANCE = this;
      this.removePushFrom.get(ENTITIES).setEnabled(true);
      this.removePushFrom.get(BLOCKS).setEnabled(true);
   }

   private boolean flag(String name) {
      BooleanSetting s = this.removePushFrom.get(name);
      return s != null && s.isEnabled();
   }

   public boolean entities() {
      return this.flag(ENTITIES);
   }

   public boolean fluids() {
      return this.flag(FLUIDS);
   }

   public boolean blocks() {
      return this.flag(BLOCKS);
   }

   public static boolean noEntities() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.entities();
   }

   public static boolean noFluids() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.fluids();
   }

   public static boolean noBlocks() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.blocks();
   }
}
