package pizza.dlc.client.modules.impl.misc;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Optimization",
   description = "Оптимизирует отрисовку серверных голограмм (FPS)",
   category = ModuleCategory.OTHER
)
public class Optimization extends Module {
   public final SliderSetting holoDistance = new SliderSetting("Дистанция голограмм", this, 28.0F, 8.0F, 64.0F, 1.0F);
   public final SliderSetting holoMax = new SliderSetting("Макс. голограмм", this, 64.0F, 8.0F, 256.0F, 1.0F);
   public final BooleanSetting singlePass = new BooleanSetting("Текст в один проход", "Не рисовать текст голограмм сквозь стены (≈вдвое меньше глифов)", this);
   public final BooleanSetting nameCache = new BooleanSetting(
      "Кэш текста имён", "Кэшировать раскладку текста имён/голограмм: строится раз, дальше повтор (меньше CPU)", this
   );
   private static double effectiveDistSq = Double.MAX_VALUE;
   private static long lastComputeNanos = 0L;

   public Optimization() {
      this.singlePass.setEnabled(true);
      this.nameCache.setEnabled(true);
   }

   private static Optimization active() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         Optimization o = cc.getModuleManager().get(Optimization.class);
         return o != null && o.isEnabled() ? o : null;
      }

      return null;
   }

   public static boolean isHologram(Entity e) {
      return e instanceof ArmorStand as && as.isInvisible() && as.isCustomNameVisible();
   }

   public static boolean cull(Entity e, double camX, double camY, double camZ) {
      Optimization o = active();
      if (o != null && isHologram(e)) {
         recompute(o);
         double dx = e.getX() - camX;
         double dy = e.getY() - camY;
         double dz = e.getZ() - camZ;
         return dx * dx + dy * dy + dz * dz > effectiveDistSq;
      }

      return false;
   }

   public static boolean singlePass(Entity e) {
      Optimization o = active();
      return o != null && o.singlePass.isEnabled() && isHologram(e);
   }

   public static boolean nameCacheOn() {
      Optimization o = active();
      return o != null && o.nameCache.isEnabled();
   }

   private static void recompute(Optimization o) {
      long now = System.nanoTime();
      if (now - lastComputeNanos >= 50000000L) {
         lastComputeNanos = now;
         double maxDistSq = (double)o.holoDistance.getValue() * o.holoDistance.getValue();
         int cap = (int)o.holoMax.getValue();
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.gameRenderer.mainCamera() != null) {
            Vec3 cam = mc.gameRenderer.mainCamera().position();
            ArrayList<Double> dists = new ArrayList<>();

            for (Entity e : mc.level.entitiesForRendering()) {
               if (!isHologram(e)) continue;

               double dx = e.getX() - cam.x;
               double dy = e.getY() - cam.y;
               double dz = e.getZ() - cam.z;
               double d = dx * dx + dy * dy + dz * dz;
               if (d <= maxDistSq) {
                  dists.add(d);
               }
            }

            if (dists.size() <= cap) {
               effectiveDistSq = maxDistSq;
            } else {
               dists.sort(null);
               effectiveDistSq = dists.get(cap - 1);
            }
         } else {
            effectiveDistSq = maxDistSq;
         }
      }
   }
}
