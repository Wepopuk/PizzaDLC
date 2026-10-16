package pizza.dlc.client.modules.impl.visuals.animatedhands;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public final class HmiParticles {
   private static final int MAX_PARTICLES = 256;

   private HmiParticles() {
   }

   public static void add(
      List<HmiParticle> list,
      boolean gravity,
      double[] motion,
      double scale,
      Identifier texture,
      List<Identifier> keyframes,
      String space,
      InteractionHand hand,
      String lifecycle,
      String renderType,
      double lifetime,
      double alpha,
      Consumer<HmiParticle> ticker,
      PoseStack matrices
   ) {
      if (!AnimatedHands.particlesEnabled() || list.size() >= MAX_PARTICLES) return;
      list.add(new HmiParticle(gravity, motion, scale, texture, keyframes, space.intern(), hand, lifecycle.intern(), renderType.intern(), lifetime, alpha, ticker, matrices));
   }

   static void draw(List<HmiParticle> list, PoseStack pose, SubmitNodeCollector collector, String space, InteractionHand hand, int light) {
      Iterator<HmiParticle> it = list.iterator();
      while (it.hasNext()) {
         HmiParticle particle = it.next();
         if (particle.dead) {
            it.remove();
            continue;
         }
         if (!space.equals(particle.space) || particle.hand != hand) continue;
         particle.render(particle.p_matrices != null ? particle.p_matrices : pose, collector, light);
         particle.tick();
      }
   }
}
