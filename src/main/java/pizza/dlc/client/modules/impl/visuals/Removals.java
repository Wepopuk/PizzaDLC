package pizza.dlc.client.modules.impl.visuals;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Removals",
   category = ModuleCategory.VISUALS
)
public class Removals extends Module {
   public static final String HURT_CAM = "HurtCam";
   public static final String FIRE = "Огонь";
   public static final String PUMPKIN = "Тыква";
   public static final String PORTAL = "Портал";
   public static final String POWDER_SNOW = "Снег";
   public static final String WEATHER = "Погода";
   public static final String FOG = "Туман";
   public static final String VIGNETTE = "Виньетка";
   public static final String CAMERA = "Камера";
   public static final String TOTEM_ANIMATION = "Анимация тотема";
   public static final String BAD_EFFECTS = "Плохие эффекты";
   public static final String MAP_MARKERS = "Метки карты";
   public static final String BOATS = "Лодки";
   public final MultiModeSetting elements = new MultiModeSetting(
      "Элементы",
      this,
      HURT_CAM,
      FIRE,
      PUMPKIN,
      PORTAL,
      POWDER_SNOW,
      WEATHER,
      FOG,
      VIGNETTE,
      CAMERA,
      TOTEM_ANIMATION,
      BAD_EFFECTS,
      MAP_MARKERS,
      BOATS
   );

   public boolean enabled(String element) {
      BooleanSetting setting = this.elements.get(element);
      return this.isEnabled() && setting != null && setting.isEnabled();
   }

   public static boolean active(String element) {
      Removals removals = get();
      return removals != null && removals.enabled(element);
   }

   public static boolean shouldRemoveFog(Entity entity) {
      return active(FOG) || active(BAD_EFFECTS) && hasBadFogEffect(entity);
   }

   public static boolean shouldRemoveNausea(Entity entity) {
      return active(BAD_EFFECTS) && entity instanceof LivingEntity living && living.hasEffect(MobEffects.NAUSEA);
   }

   private static boolean hasBadFogEffect(Entity entity) {
      return entity instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
   }

   private static Removals get() {
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      return client != null && client.getModuleManager() != null ? client.getModuleManager().get(Removals.class) : null;
   }
}
