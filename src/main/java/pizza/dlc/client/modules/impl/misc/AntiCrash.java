package pizza.dlc.client.modules.impl.misc;

import java.util.HashSet;
import java.util.Set;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.world.level.saveddata.maps.MapId;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AntiCrash",
   description = "Защита от серверных крашеров (утечки клиентских коллекций)",
   category = ModuleCategory.OTHER
)
public class AntiCrash extends Module {
   private static final String MAPS = "Карты";
   private static final String TAB = "Таб";
   private static final String BOSS = "Боссы";
   private static final String SCORE = "Скорборд";
   public final MultiModeSetting protections = new MultiModeSetting("Защиты", this, MAPS, TAB, BOSS, SCORE);
   public final SliderSetting capMaps = new SliderSetting("Лимит карт", this, 256.0F, 8.0F, 1024.0F, 1.0F);
   public final SliderSetting capTab = new SliderSetting("Лимит игроков в табе", this, 5000.0F, 500.0F, 50000.0F, 100.0F);
   public final SliderSetting bossRate = new SliderSetting("Боссов/сек", this, 16.0F, 1.0F, 128.0F, 1.0F);
   public final SliderSetting capTeams = new SliderSetting("Лимит команд", this, 5000.0F, 500.0F, 50000.0F, 100.0F);
   public final SliderSetting capObjectives = new SliderSetting("Лимит целей скорборда", this, 1000.0F, 100.0F, 20000.0F, 100.0F);
   public final BooleanSetting notify = new BooleanSetting("Уведомлять", this);
   private final Set<MapId> acceptedMaps = new HashSet<>();
   private ClientLevel lastLevel;
   private long bossWindowStart;
   private int bossInWindow;
   private long lastNotify;

   public AntiCrash() {
      this.protections.get(MAPS).setEnabled(true);
      this.protections.get(TAB).setEnabled(true);
      this.protections.get(BOSS).setEnabled(true);
      this.protections.get(SCORE).setEnabled(true);
      this.capMaps.setVisible(() -> this.on(MAPS));
      this.capTab.setVisible(() -> this.on(TAB));
      this.bossRate.setVisible(() -> this.on(BOSS));
      this.capTeams.setVisible(() -> this.on(SCORE));
      this.capObjectives.setVisible(() -> this.on(SCORE));
   }

   private boolean on(String name) {
      BooleanSetting s = this.protections.get(name);
      return s != null && s.isEnabled();
   }

   @EventHandler
   public void onReceivePacket(PacketEvent.Receive e) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() != null) {
         Packet<?> p = e.getPacket();

         try {
            if (p instanceof ClientboundMapItemDataPacket map && this.on(MAPS)) {
               if (mc.level != this.lastLevel) {
                  this.acceptedMaps.clear();
                  this.lastLevel = mc.level;
               }

               MapId id = map.mapId();
               if (!this.acceptedMaps.contains(id)) {
                  if (this.acceptedMaps.size() >= (int)this.capMaps.getValue()) {
                     e.setCancelled(true);
                     this.drop("карта");
                     return;
                  }

                  this.acceptedMaps.add(id);
               }

               return;
            }

            if (p instanceof ClientboundPlayerInfoUpdatePacket tab && this.on(TAB)) {
               if (tab.actions().contains(Action.ADD_PLAYER) && mc.getConnection().getSeenPlayers().size() >= (int)this.capTab.getValue()) {
                  e.setCancelled(true);
                  this.drop("таб");
                  return;
               }

               return;
            }

            if (p instanceof ClientboundBossEventPacket && this.on(BOSS)) {
               long now = System.currentTimeMillis();
               if (now - this.bossWindowStart >= 1000L) {
                  this.bossWindowStart = now;
                  this.bossInWindow = 0;
               }

               if (++this.bossInWindow > (int)this.bossRate.getValue()) {
                  e.setCancelled(true);
                  this.drop("босс");
               }

               return;
            }

            if (mc.level != null && this.on(SCORE)) {
               if (p instanceof ClientboundSetObjectivePacket obj
                  && obj.getMethod() == 0
                  && mc.level.getScoreboard().getObjectives().size() >= (int)this.capObjectives.getValue()) {
                  e.setCancelled(true);
                  this.drop("цель");
                  return;
               }

               if (p instanceof ClientboundSetPlayerTeamPacket team
                  && team.getTeamAction() == net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action.ADD
                  && mc.level.getScoreboard().getPlayerTeams().size() >= (int)this.capTeams.getValue()) {
                  e.setCancelled(true);
                  this.drop("команда");
               }
            }
         } catch (Throwable ignored) {
         }
      }
   }

   private void drop(String what) {
      if (!this.notify.isEnabled()) return;

      long now = System.currentTimeMillis();
      if (now - this.lastNotify >= 1500L) {
         this.lastNotify = now;
         NotificationManager.add("AntiCrash", "Дроп: " + what + " (лимит)", NotificationType.INFO, 2000L);
      }
   }
}
