package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiParticles;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiParticle;

public final class LuaParticles {
   public void addParticle(
      List<HmiParticle> list, boolean gravity,
      double x, double y, double z, double dx, double dy, double dz,
      double rx, double ry, double rz, double drx, double dry, double drz,
      double scale, Identifier texture, String space, InteractionHand hand, String lifecycle, String renderType,
      double lifetime, double alpha, LuaValue ticker
   ) {
      HmiParticles.add(list, gravity, new double[]{x, y, z, dx, dy, dz, rx, ry, rz, drx, dry, drz}, scale, texture, null, space, hand, lifecycle, renderType, lifetime, alpha, consumer(ticker), null);
   }

   public void addParticle(
      List<HmiParticle> list, boolean gravity,
      double x, double y, double z, double dx, double dy, double dz,
      double rx, double ry, double rz, double drx, double dry, double drz,
      double scale, Identifier texture, String space, InteractionHand hand, String lifecycle, String renderType,
      double lifetime, double alpha
   ) {
      HmiParticles.add(list, gravity, new double[]{x, y, z, dx, dy, dz, rx, ry, rz, drx, dry, drz}, scale, texture, null, space, hand, lifecycle, renderType, lifetime, alpha, null, null);
   }

   public void addParticle(
      List<HmiParticle> list, boolean gravity,
      double x, double y, double z, double dx, double dy, double dz,
      double rx, double ry, double rz, double drx, double dry, double drz,
      double scale, Identifier texture, List<Identifier> keyframes, String space, InteractionHand hand, String lifecycle, String renderType,
      double lifetime, double alpha, LuaValue ticker
   ) {
      boolean exists = list.stream().anyMatch(p -> "KEYFRAME".equals(p.lifecycleType) && p.hand == hand);
      if (!exists) {
         HmiParticles.add(list, gravity, new double[]{x, y, z, dx, dy, dz, rx, ry, rz, drx, dry, drz}, scale, texture, keyframes, space, hand, lifecycle, renderType, lifetime, alpha, consumer(ticker), null);
      }
   }

   public void addParticle(
      List<HmiParticle> list, boolean gravity,
      double x, double y, double z, double dx, double dy, double dz,
      double rx, double ry, double rz, double drx, double dry, double drz,
      double scale, Identifier texture, String space, InteractionHand hand, String lifecycle, String renderType,
      double lifetime, double alpha, LuaValue ticker, PoseStack matrices
   ) {
      HmiParticles.add(list, gravity, new double[]{x, y, z, dx, dy, dz, rx, ry, rz, drx, dry, drz}, scale, texture, null, space, hand, lifecycle, renderType, lifetime, alpha, consumer(ticker), matrices);
   }

   private static Consumer<HmiParticle> consumer(LuaValue ticker) {
      if (ticker == null || !ticker.isfunction()) return null;
      LuaFunction function = (LuaFunction)ticker;
      return particle -> function.call(CoerceJavaToLua.coerce(particle));
   }
}
