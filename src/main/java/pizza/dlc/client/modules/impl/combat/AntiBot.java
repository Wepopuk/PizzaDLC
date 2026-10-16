package pizza.dlc.client.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AntiBot",
   description = "Не атаковать ботов анти-чита (AttackAura)",
   category = ModuleCategory.COMBAT
)
public class AntiBot extends Module {
   public static AntiBot INSTANCE;
   private static final String CUSTOM = "Custom";
   private static final String REALLYWORLD = "ReallyWorld";
   private static final String BHVH = "BHvH";
   public final ModeSetting mode = new ModeSetting("Режим", this, CUSTOM, REALLYWORLD, BHVH);
   public final BooleanSetting tabCheck = new BooleanSetting("Таб-чек", this);
   public final BooleanSetting skinCheck = new BooleanSetting("Скин-чек", this);
   public final BooleanSetting pingCheck = new BooleanSetting("Пинг 0", this);
   public final BooleanSetting idCheck = new BooleanSetting("Entity ID", this);
   public final BooleanSetting uuidCheck = new BooleanSetting(
      "Оффлайн-UUID",
      "Не-v4 UUID. Премиум-игроки Mojang всегда v4, боты/оффлайн = v2/v3. НА КРЯК-СЕРВЕРАХ (offline-mode) все v3 → ложняки на реальных игроках!",
      this
   );
   public final BooleanSetting prefixCheck = new BooleanSetting("Без префикса", this);
   public final BooleanSetting nameCheck = new BooleanSetting("Кривой ник", this);
   private static final Set<UUID> knownBots = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> firstSeen = new ConcurrentHashMap<>();
   private static final long VERIFY_GRACE_MS = 2000L;
   public final BooleanSetting removeFromWorld = new BooleanSetting("Удалять из мира", this);
   public final BooleanSetting bhvhPingCheck = new BooleanSetting(
      "Норм. пинг = бот", "Обычный/видимый пинг = бот (реальным сервер поставил огромный, чтобы скрыть).", this
   );
   public final BooleanSetting bhvhTabCheck = new BooleanSetting("Таб «»» = бот", "Таб-имя с префиксом «»» = бот (у реальных tab-display скрыт/null).", this);

   public AntiBot() {
      INSTANCE = this;
      this.tabCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.skinCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.pingCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.idCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.uuidCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.prefixCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.nameCheck.setVisible(() -> this.mode.is(CUSTOM));
      this.bhvhPingCheck.setVisible(() -> this.mode.is(BHVH));
      this.bhvhTabCheck.setVisible(() -> this.mode.is(BHVH));
      this.tabCheck.setEnabled(true);
      this.skinCheck.setEnabled(true);
      this.pingCheck.setEnabled(true);
      this.idCheck.setEnabled(true);
      this.nameCheck.setEnabled(true);
      this.bhvhPingCheck.setEnabled(true);
      this.bhvhTabCheck.setEnabled(true);
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         if (this.removeFromWorld.isEnabled()) {
            List<Entity> bots = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (Entity ent : mc.level.entitiesForRendering()) {
               if (isBot(ent)) {
                  UUID id = ent.getUUID();
                  if (ent instanceof Player && mc.getConnection() != null
                     && mc.getConnection().getPlayerInfo(id) == null) {
                     firstSeen.putIfAbsent(id, now);
                     continue;
                  }
                  long seen = firstSeen.computeIfAbsent(id, k -> now);
                  if (now - seen < VERIFY_GRACE_MS) {
                     continue;
                  }
                  bots.add(ent);
               } else {
                  firstSeen.remove(ent.getUUID());
               }
            }

            for (Entity entx : bots) {
               knownBots.add(entx.getUUID());
               mc.level.removeEntity(entx.getId(), RemovalReason.DISCARDED);
            }
         }
      }
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      knownBots.clear();
      firstSeen.clear();
   }

   @Override
   public void onDisable() {
      knownBots.clear();
      firstSeen.clear();
      super.onDisable();
   }

   @EventHandler
   public void onReceivePacket(PacketEvent.Receive e) {
      if (!this.removeFromWorld.isEnabled()) return;

      if (e.getPacket() instanceof ClientboundAddEntityPacket add) {
         if (knownBots.contains(add.getUUID()) && !isConfirmedBotNow(add.getUUID(), add.getId())) {
            knownBots.remove(add.getUUID());
            return;
         }
         if (add.getType().getDescriptionId().equals("entity.minecraft.player")) {
            if (isConfirmedBotNow(add.getUUID(), add.getId())) {
               e.setCancelled(true);
            }
         }
      }
   }

   private static boolean isConfirmedBotNow(UUID uuid, int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null || mc.getConnection().getPlayerInfo(uuid) == null) {
         return false;
      }
      return isBotProfile(uuid, entityId);
   }

   public static boolean isBot(Entity entity) {
      if (pizza.dlc.api.neuro.NeuroSystem.isBot(entity)) return false;
      if (INSTANCE == null || !INSTANCE.isEnabled()) {
         return false;
      }

      if (entity instanceof Player player) {
         if (player == Minecraft.getInstance().player) {
            return false;
         }

         if (INSTANCE.mode.is(CUSTOM) && INSTANCE.nameCheck.isEnabled()) {
            String n = player.getName().getString();
            if (n == null || !n.matches("[A-Za-z0-9_]{1,16}")) {
               return true;
            }
         }

         return isBotProfile(player.getUUID(), player.getId());
      }

      return false;
   }

   public static boolean isBotProfile(UUID uuid, int entityId) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return false;
         }

         return INSTANCE.mode.is(REALLYWORLD) ? isReallyWorldBot(uuid) : (INSTANCE.mode.is(BHVH) ? isBHvHBot(uuid) : isCustomBot(uuid, entityId));
      }

      return false;
   }

   private static boolean isCustomBot(UUID uuid, int entityId) {
      Minecraft mc = Minecraft.getInstance();
      PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(uuid) : null;
      if (INSTANCE.tabCheck.isEnabled() && info == null) {
         return true;
      }

      if (info != null) {
         if (INSTANCE.nameCheck.isEnabled()) {
            String pname = info.getProfile() != null ? info.getProfile().name() : null;
            if (pname == null || !pname.matches("[A-Za-z0-9_]{1,16}")) {
               return true;
            }
         }

         if (INSTANCE.pingCheck.isEnabled() && info.getLatency() == 0 && !mc.hasSingleplayerServer()) {
            return true;
         }

         if (INSTANCE.skinCheck.isEnabled()) {
            GameProfile profile = info.getProfile();
            if (profile == null || profile.properties().isEmpty() || !profile.properties().containsKey("textures")) {
               return true;
            }
         }

         if (info.getGameMode() == GameType.SPECTATOR) {
            return true;
         }
      }

      if (INSTANCE.idCheck.isEnabled() && entityId >= 100000000) {
         return true;
      }

      if (INSTANCE.uuidCheck.isEnabled()) {
         try {
            if (uuid.version() != 4) {
               return true;
            }
         } catch (Exception ignored) {
         }
      }

      return INSTANCE.prefixCheck.isEnabled() && isReallyWorldBot(uuid);
   }

   private static boolean isReallyWorldBot(UUID uuid) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null) {
         return false;
      }

      PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
      if (info == null) {
         return true;
      }

      Component displayName = info.getTabListDisplayName();
      if (displayName == null) {
         return true;
      }

      GameProfile profile = info.getProfile();
      String name = profile != null ? profile.name() : null;
      if (name == null) {
         return true;
      }

      String raw = displayName.getString();
      if (raw.trim().equals(name.trim())) {
         return true;
      }

      String stripped = ChatFormatting.stripFormatting(raw);
      return stripped != null
         && stripped.trim().equals(name.trim())
         && !raw.contains(" ")
         && !raw.contains("|")
         && !raw.contains("»")
         && !raw.contains("›")
         && !raw.contains("]");
   }

   private static boolean isBHvHBot(UUID uuid) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null) {
         return false;
      }

      PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
      if (info == null) {
         return false;
      }

      if (INSTANCE.bhvhPingCheck.isEnabled() && info.getLatency() >= 0 && info.getLatency() < 100000) {
         return true;
      }

      if (INSTANCE.bhvhTabCheck.isEnabled()) {
         Component tab = info.getTabListDisplayName();
         if (tab != null) {
            String raw = tab.getString();
            String stripped = ChatFormatting.stripFormatting(raw);
            if ((stripped != null ? stripped : raw).contains("»")) {
               return true;
            }
         }
      }

      return false;
   }
}
