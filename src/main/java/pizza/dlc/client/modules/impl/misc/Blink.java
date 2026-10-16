package pizza.dlc.client.modules.impl.misc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.phys.AABB;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Blink",
   description = "Задержка пакетов: сервер видит вас на месте, отпуск — телепорт",
   category = ModuleCategory.OTHER
)
public class Blink extends Module {
   private final BooleanSetting pulse = new BooleanSetting("Пульсировать", this);
   private final SliderSetting interval = new SliderSetting("Интервал (мс)", this, 500.0F, 50.0F, 3000.0F, 50.0F);
   private final BooleanSetting render = new BooleanSetting("Отображать", this);
   private final List<Packet<?>> queue = new CopyOnWriteArrayList<>();
   private AABB frozenBox;
   private long lastFlush;
   private volatile boolean resnap;
   private static boolean flushing;

   public Blink() {
      this.interval.setVisible(() -> this.pulse.isEnabled());
      this.render.setEnabled(true);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null) {
         this.queue.clear();
         this.frozenBox = mc.player.getBoundingBox();
         this.lastFlush = System.currentTimeMillis();
         this.resnap = false;
         super.onEnable();
      } else {
         this.setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      this.flush();
      this.frozenBox = null;
      super.onDisable();
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!this.isEnabled() || flushing) return;

      Packet<?> p = e.getPacket();
      if (p instanceof ServerboundClientCommandPacket) {
         this.setEnabled(false);
      } else {
         this.queue.add(p);
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!this.isEnabled()) return;

      Packet<?> p = e.getPacket();
      if (p instanceof ClientboundRespawnPacket || p instanceof ClientboundLoginPacket) {
         this.queue.clear();
         this.setEnabled(false);
      } else if (p instanceof ClientboundPlayerPositionPacket) {
         this.flush();
         this.resnap = true;
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (!this.isEnabled() || mc.player == null) return;

      long now = System.currentTimeMillis();
      if (this.resnap) {
         this.frozenBox = mc.player.getBoundingBox();
         this.lastFlush = now;
         this.resnap = false;
      }

      if (this.pulse.isEnabled() && now - this.lastFlush >= (long)this.interval.getValue()) {
         this.flush();
         this.frozenBox = mc.player.getBoundingBox();
         this.lastFlush = now;
      }
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (!this.isEnabled() || !this.render.isEnabled() || this.frozenBox == null) return;

      Render3DUtil.drawBox(e, this.frozenBox, ThemeManager.accentAlpha(160), true);
   }

   private void flush() {
      if (mc.player != null && mc.player.connection != null) {
         flushing = true;

         try {
            for (Packet<?> p : this.queue) {
               mc.player.connection.send(p);
            }
         } finally {
            flushing = false;
         }

         this.queue.clear();
      } else {
         this.queue.clear();
      }
   }
}
