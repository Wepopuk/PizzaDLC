package pizza.dlc.client.modules.impl.visuals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Chams",
   description = "Подсвечивает модели сущностей сквозь стены",
   category = ModuleCategory.VISUALS
)
public class Chams extends Module {
   private static final String M_DEFAULT = "Дефолт";
   private static final String M_TOTEM = "Тотем";
   private static final String T_PLAYERS = "Игроки";
   private static final String T_MOBS = "Мобы";
   private static final String T_ANIMALS = "Животные";
   private static final String T_FRIENDS = "Друзья";
   private static final int TOTEM_EVENT_ID = 35;
   private static final String S_BOTH = "Заливка + свечение";
   private static final String S_SOLID = "Заливка";
   private static final String S_GLOW = "Свечение";
   public final ModeSetting mode = new ModeSetting("Режим", this, M_DEFAULT, M_TOTEM);
   public final ModeSetting style = new ModeSetting("Стиль", this, S_BOTH, S_SOLID, S_GLOW);
   public final MultiModeSetting targets = new MultiModeSetting("Цели", this, T_PLAYERS, T_MOBS, T_ANIMALS, T_FRIENDS).setVisible(() -> this.mode.is(M_DEFAULT));
   public final ColorSetting color = new ColorSetting("Цвет", this, 0xFF8E8BFF);
   public final SliderSetting intensity = new SliderSetting("Яркость", this, 0.55F, 0.05F, 1.0F, 0.05F).setVisible(() -> !this.style.is(S_SOLID));
   public final SliderSetting fill = new SliderSetting("Плотность", this, 0.85F, 0.1F, 1.0F, 0.05F).setVisible(() -> !this.style.is(S_GLOW));
   public final BooleanSetting hideSkin = new BooleanSetting(
      "Прятать скин", "Не рисовать саму модель — остаётся только наша заливка (и свечение ShaderESP, если включён)", this
   );
   public final SliderSetting totemTime = new SliderSetting("Держать (сек)", this, 4.0F, 0.5F, 15.0F, 0.5F).setVisible(() -> this.mode.is(M_TOTEM));
   private final BooleanSetting tPlayers = this.targets.get(T_PLAYERS);
   private final BooleanSetting tMobs = this.targets.get(T_MOBS);
   private final BooleanSetting tAnimals = this.targets.get(T_ANIMALS);
   private final BooleanSetting tFriends = this.targets.get(T_FRIENDS);
   private final Map<Integer, Long> totemPops = new HashMap<>();

   public Chams() {
      this.targets.get(T_PLAYERS).setEnabled(true);
   }

   @Override
   public void onEnable() {
      this.totemPops.clear();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.totemPops.clear();
      super.onDisable();
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!this.mode.is(M_TOTEM) || !(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;

      if (packet.getEventId() == TOTEM_EVENT_ID) {
         long now = System.currentTimeMillis();
         mc.execute(() -> {
            if (this.isEnabled() && mc.level != null) {
               Entity entity = packet.getEntity(mc.level);
               if (entity instanceof Player && entity != mc.player) {
                  this.totemPops.put(entity.getId(), now);
               }
            }
         });
      }
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (mc.level == null) {
         this.totemPops.clear();
      } else {
         long limit = (long)(this.totemTime.getValue() * 1000.0F);
         long now = System.currentTimeMillis();
         Iterator<Entry<Integer, Long>> it = this.totemPops.entrySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().getValue() > limit) {
               it.remove();
            }
         }
      }
   }

   private float visibility(Entity entity) {
      if (entity == null || entity == mc.player || !entity.isAlive()) {
         return 0.0F;
      }

      if (this.mode.is(M_TOTEM)) {
         Long popped = this.totemPops.get(entity.getId());
         if (popped == null) {
            return 0.0F;
         }

         long limit = (long)(this.totemTime.getValue() * 1000.0F);
         return 1.0F - Mth.clamp((float)(System.currentTimeMillis() - popped) / (float)Math.max(1L, limit), 0.0F, 1.0F);
      }

      return this.accept(entity) ? 1.0F : 0.0F;
   }

   public int solidColor(Entity entity) {
      return this.style.is(S_GLOW) ? 0 : this.tint(this.visibility(entity) * this.fill.getValue());
   }

   public int glowColor(Entity entity) {
      return this.style.is(S_SOLID) ? 0 : this.tint(this.visibility(entity) * this.intensity.getValue());
   }

   public boolean hidesSkin() {
      return this.hideSkin.isEnabled();
   }

   private int tint(float alpha) {
      int a = Math.round((this.color.getArgb() >>> 24 & 0xFF) * Mth.clamp(alpha, 0.0F, 1.0F));
      return a <= 2 ? 0 : a << 24 | this.color.getArgb() & 0xFFFFFF;
   }

   private boolean accept(Entity entity) {
      if (entity instanceof Player player) {
         return isFriend(player) ? this.tFriends.isEnabled() : this.tPlayers.isEnabled();
      }

      if (entity instanceof Animal) {
         return this.tAnimals.isEnabled();
      }

      return (entity instanceof Enemy || entity instanceof Mob) && this.tMobs.isEnabled();
   }

   private static boolean isFriend(Player player) {
      return PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(player.getName().getString());
   }
}
