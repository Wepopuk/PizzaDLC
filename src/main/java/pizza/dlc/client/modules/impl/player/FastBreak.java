package pizza.dlc.client.modules.impl.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.mixin.client.MultiPlayerGameModeAccessor;

@ModuleInfo(
   name = "FastBreak",
   description = "Ускоряет копание блоков",
   category = ModuleCategory.PLAYER
)
public class FastBreak extends Module {
   private final ModeSetting mode = new ModeSetting("Режим", this, "Default", "Burst");
   private final SliderSetting speed = new SliderSetting("Скорость", this, 2.0F, 1.0F, 10.0F, 0.1F);
   private final SliderSetting buffer = new SliderSetting("Буфер Grim", this, 700.0F, 100.0F, 950.0F, 50.0F);
   private double breakBalance = 0.0;
   private double delayBalance = 0.0;
   private boolean armed = true;
   private long blockStartMs;
   private long lastFinishMs;
   private double predictedMs;
   private boolean wasDestroying;
   private float lastDefaultProgress;

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.level == null || mc.gameMode == null) return;

      MultiPlayerGameModeAccessor acc = (MultiPlayerGameModeAccessor)mc.gameMode;
      boolean destroying = mc.gameMode.isDestroying();
      if (this.mode.is("Default")) {
         if (destroying) {
            acc.setDestroyDelay(0);
            float p = acc.getDestroyProgress();
            if (p < this.lastDefaultProgress) {
               this.lastDefaultProgress = 0.0F;
            }

            if (p > 0.0F && p < 1.0F) {
               float delta = p - this.lastDefaultProgress;
               float boosted = Math.min(1.0F, p + delta * (this.speed.getValue() - 1.0F));
               acc.setDestroyProgress(boosted);
               this.lastDefaultProgress = boosted;
            } else {
               this.lastDefaultProgress = p;
            }
         } else {
            this.lastDefaultProgress = 0.0F;
         }
      } else {
         float buf = this.buffer.getValue();
         if (this.breakBalance >= buf * 0.9) {
            this.armed = false;
         } else if (this.breakBalance <= buf * 0.35) {
            this.armed = true;
         }

         if (this.wasDestroying && !destroying && this.delayBalance > buf * 0.5) {
            acc.setDestroyDelay(7);
         }

         if (this.armed && destroying && this.predictedMs > 0.0) {
            double elapsed = System.currentTimeMillis() - this.blockStartMs;
            double room = buf - this.breakBalance;
            float px = acc.getDestroyProgress();
            if (room > 0.0 && this.predictedMs - elapsed <= room && px > 0.0F && px < 1.0F) {
               acc.setDestroyProgress(1.0F);
            }
         }
      }

      this.wasDestroying = destroying;
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!this.mode.is("Burst")) return;

      if (e.getPacket() instanceof ServerboundPlayerActionPacket packet) {
         if (mc.player != null && mc.level != null) {
            long now = System.currentTimeMillis();
            Action action = packet.getAction();
            if (action == Action.START_DESTROY_BLOCK) {
               double gap = now - this.lastFinishMs;
               if (gap >= 275.0) {
                  this.delayBalance *= 0.9;
               } else {
                  this.delayBalance += 300.0 - gap;
               }

               this.delayBalance = clamp(this.delayBalance);
               this.blockStartMs = now;
               BlockPos pos = packet.getPos();
               double perTick = mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos);
               this.predictedMs = perTick > 0.0 ? Math.ceil(1.0 / perTick) * 50.0 : 0.0;
            } else if (action == Action.STOP_DESTROY_BLOCK) {
               double realMs = now - this.blockStartMs;
               double diff = this.predictedMs - realMs;
               if (diff >= 25.0) {
                  this.breakBalance += diff;
               } else {
                  this.breakBalance *= 0.9;
               }

               this.breakBalance = clamp(this.breakBalance);
               this.lastFinishMs = now;
            }
         }
      }
   }

   private static double clamp(double v) {
      return Math.max(-1000.0, Math.min(1000.0, v));
   }

   @Override
   public void onDisable() {
      this.wasDestroying = false;
      this.predictedMs = 0.0;
      super.onDisable();
   }
}
