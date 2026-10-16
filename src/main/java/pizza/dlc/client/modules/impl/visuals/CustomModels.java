package pizza.dlc.client.modules.impl.visuals;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Custom Models", description = "Заменяет модель выбранных игроков", category = ModuleCategory.VISUALS)
public class CustomModels extends Module {
   public static final String CHICKEN = "Chicken";
   public static final String VERITY = "Verity";
   public static final String RABBIT = "Rabbit";
   public static final String PUTIN = "Putin";
   public static final String BOYKISSER = "Boykisser";
   public static final String HITLER = "Hitler";

   private final ModeSetting model = new ModeSetting("Модель", this, CHICKEN, CHICKEN, VERITY, RABBIT, PUTIN, BOYKISSER, HITLER);
   private final BooleanSetting self = new BooleanSetting("На себя", this);
   private final BooleanSetting friends = new BooleanSetting("На друзей", this);
   private final BooleanSetting others = new BooleanSetting("На всех игроков", this);
   private final BooleanSetting hand = new BooleanSetting("Рука от модели", this);
   private final SliderSetting size = new SliderSetting("Размер", this, 1.0F, 0.3F, 3.0F, 0.05F);
   private final ModeSetting face = new ModeSetting(
      "Лицо", this, "Обычное", "Обычное", "Говорит", "Странное", "Странное говорит", "Странное кричит", "Плохое", "Очень плохое"
   );

   public CustomModels() {
      this.self.setEnabled(true);
      this.hand.setEnabled(true);
      this.hand.setVisible(() -> this.model.is(CHICKEN) && this.self.isEnabled());
      this.size.setVisible(() -> this.model.is(VERITY) || this.model.is(PUTIN) || this.model.is(BOYKISSER) || this.model.is(HITLER));
      this.face.setVisible(() -> this.model.is(VERITY));
   }

   public boolean handFromModel() {
      if (mc.player != null && EmoteEngine.INSTANCE.skinOverrideFor(mc.player.getUUID()) != null) return false;
      return this.isEnabled() && this.model.is(CHICKEN) && this.self.isEnabled() && this.hand.isEnabled();
   }

   public String modelFor(Avatar avatar) {
      if (!this.isEnabled() || avatar == null || mc.player == null) {
         return null;
      }
      boolean wanted;
      if (avatar.getUUID().equals(mc.player.getUUID())) {
         wanted = this.self.isEnabled();
      } else if (avatar instanceof Player player && isFriend(player.getGameProfile().name())) {
         wanted = this.friends.isEnabled();
      } else {
         wanted = this.others.isEnabled();
      }
      return wanted ? this.model.get() : null;
   }

   private static boolean isFriend(String name) {
      return PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(name);
   }

   public float size() {
      return this.size.getValue();
   }

   public Identifier verityTexture() {
      String file = switch (this.face.get()) {
         case "Говорит" -> "veritytalk";
         case "Странное" -> "veritydisturbing";
         case "Странное говорит" -> "veritydisturbingtalk";
         case "Странное кричит" -> "veritydisturbingyell";
         case "Плохое" -> "veritybad";
         case "Очень плохое" -> "verityverybad";
         default -> "verity";
      };
      return Identifier.fromNamespaceAndPath("pizzadlc", "models/verity/" + file + ".png");
   }
}
