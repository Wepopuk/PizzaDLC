package pizza.dlc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.util.game.BoatLimiter;
import pizza.dlc.client.modules.impl.combat.CrystalAura;

@Mixin({ClientLevel.class})
public class ClientWorldMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$onWorldLoad(CallbackInfo ci) {
      new EventWorldLoad().call();
   }

   @Inject(
      method = {"addEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$boatsAdd(Entity entity, CallbackInfo ci) {
      if (BoatLimiter.shouldBlockSpawn(entity)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addEntity"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$crystalSpawn(Entity entity, CallbackInfo ci) {
      CrystalAura.onEntityAdded(entity);
   }

   @Inject(
      method = {"tickNonPassenger"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$boatsTick(Entity entity, CallbackInfo ci) {
      if (BoatLimiter.shouldCull(entity)) {
         entity.discard();
         BoatLimiter.invalidate();
         ci.cancel();
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$boatsPurge(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
      if (!pizza.dlc.client.modules.impl.visuals.Removals.active("Лодки")) return;
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      if (mc.level == null || mc.player == null) return;
      if (BoatLimiter.getBoatCount() <= BoatLimiter.MAX_BOATS) return;
      java.util.List<net.minecraft.world.entity.vehicle.boat.AbstractBoat> boats = new java.util.ArrayList<>();
      for (Entity e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat b) boats.add(b);
      }
      if (boats.size() <= BoatLimiter.MAX_BOATS) return;
      double px = mc.player.getX();
      double py = mc.player.getY();
      double pz = mc.player.getZ();
      boats.sort(java.util.Comparator.comparingDouble(b -> {
         double dx = b.getX() - px;
         double dy = b.getY() - py;
         double dz = b.getZ() - pz;
         return dx * dx + dy * dy + dz * dz;
      }));
      for (int i = BoatLimiter.MAX_BOATS; i < boats.size(); i++) {
         boats.get(i).discard();
      }
      BoatLimiter.invalidate();
   }
}
