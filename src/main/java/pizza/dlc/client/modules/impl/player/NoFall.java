package pizza.dlc.client.modules.impl.player;

import pizza.dlc.api.events.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.game.SentMotion;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "NoFall",
   category = ModuleCategory.PLAYER,
   description = "Убирает урон от падения"
)
public class NoFall extends Module {
   private static final String GRIM = "Grim";
   private static final String VANILLA = "Vanilla";
   private static final double VANILLA_SPOOF_FALL = 2.0;
   private static final double MIN_LANDING_DROP = 0.05;
   private static final double SPOOF_OFFSET = 0.25;
   private static final double HARD_ENTITY_RANGE = 5.5;
   private static final int TELEPORT_TIMEOUT = 12;
   private static final int VELOCITY_WAIT = 2;
   private static final int MAX_OWED_TICKS = 4;
   private static NoFall instance;
   private final ModeSetting mode = new ModeSetting("Режим", this, GRIM, VANILLA);
   private double lastFallDistance;
   private int waitTicks = -1;
   private boolean teleported;
   private boolean velocityApplied;
   private int velocityWait;
   private boolean jumpNow;
   private int owedTicks;
   private boolean movedThisTick;

   public NoFall() {
      instance = this;
   }

   public boolean isBusy() {
      return this.waitTicks >= 0 || this.jumpNow;
   }

   public static void onTeleportApplied() {
      NoFall module = instance;
      if (module != null && module.isEnabled() && module.waitTicks >= 0) {
         module.teleported = true;
         module.velocityApplied = false;
      }
   }

   public static void onMotionApplied(int entityId) {
      NoFall module = instance;
      if (module != null && module.teleported && mc.player != null && entityId == mc.player.getId()) {
         module.velocityApplied = true;
      }
   }

   @Override
   public void onDisable() {
      this.reset();
      this.jumpNow = false;
      this.owedTicks = 0;
      super.onDisable();
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      this.reset();
      this.owedTicks = 0;
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      LocalPlayer self = mc.player;
      this.jumpNow = false;
      if (self == null) {
         this.reset();
         return;
      }

      if (this.waitTicks >= 0) {
         if (this.teleported && (this.velocityApplied || ++this.velocityWait > VELOCITY_WAIT)) {
            this.jumpNow = self.onGround();
            this.reset();
         } else if (++this.waitTicks > TELEPORT_TIMEOUT) {
            this.reset();
         }
      }

      this.lastFallDistance = self.fallDistance;
   }

   @EventHandler
   public void onInputMove(EventInputMove e) {
      if (this.jumpNow) {
         e.setJump(true);
      }
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      LocalPlayer self = mc.player;
      if (self == null || e.isCancelled()) return;

      if (this.mode.is(VANILLA)) {
         if (self.fallDistance > VANILLA_SPOOF_FALL && !self.isFallFlying()) {
            e.setOnGround(true);
         }
         return;
      }

      if (this.waitTicks >= 0) {
         e.setCancelled(true);
         return;
      }

      if (!this.landingHurts(self)) return;

      if (this.nearHardEntity(self)) {
         e.replaceWith(this.landingPacket(self, e, e.getY() + SPOOF_OFFSET));
      } else {
         e.replaceWith(
            new ServerboundMovePlayerPacket.StatusOnly(true, self.horizontalCollision),
            ServerboundClientTickEndPacket.INSTANCE,
            this.landingPacket(self, e, e.getY())
         );
         this.owedTicks = Math.min(MAX_OWED_TICKS, this.owedTicks + 1);
      }

      this.waitTicks = 0;
      this.teleported = false;
      this.velocityApplied = false;
      this.velocityWait = 0;
   }

   @EventHandler
   public void onSend(PacketEvent.Send e) {
      if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
         this.movedThisTick = true;
         return;
      }

      if (!(e.getPacket() instanceof ServerboundClientTickEndPacket)) return;

      if (!this.movedThisTick && this.owedTicks > 0) {
         this.owedTicks--;
         e.setCancelled(true);
      }

      this.movedThisTick = false;
   }

   private boolean landingHurts(LocalPlayer self) {
      if (!self.onGround()
         || self.isPassenger()
         || self.isFallFlying()
         || self.isInWater()
         || self.isInLava()
         || self.getAbilities().flying
         || self.getAbilities().invulnerable) {
         return false;
      }

      return this.lastFallDistance > self.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) && self.yo - self.getY() > MIN_LANDING_DROP;
   }

   private boolean nearHardEntity(LocalPlayer self) {
      AABB area = self.getBoundingBox().inflate(HARD_ENTITY_RANGE);
      for (Entity entity : mc.level.getEntities(self, area)) {
         if (entity instanceof AbstractBoat || entity instanceof Shulker || entity instanceof HappyGhast) return true;
      }

      return false;
   }

   private ServerboundMovePlayerPacket landingPacket(LocalPlayer self, EventMotion e, double y) {
      SentMotion sent = (SentMotion)self;
      boolean rotated = e.getYaw() != sent.pizzadlc$sentYaw() || e.getPitch() != sent.pizzadlc$sentPitch();
      return rotated
         ? new ServerboundMovePlayerPacket.PosRot(e.getX(), y, e.getZ(), e.getYaw(), e.getPitch(), true, self.horizontalCollision)
         : new ServerboundMovePlayerPacket.Pos(e.getX(), y, e.getZ(), true, self.horizontalCollision);
   }

   private void reset() {
      this.waitTicks = -1;
      this.teleported = false;
      this.velocityApplied = false;
      this.velocityWait = 0;
   }
}
