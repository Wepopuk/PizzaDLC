package pizza.dlc.api.util.render.targetesp;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render3DUtil;

public final class ImageTargetRenderer {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/target.png");
   private static final long DIRECTION_PERIOD_NS = 3000000000L;
   private static final float ANGULAR_RESPONSE = 1.8F;
   private static final float HIT_BOOST_DURATION_S = 0.28F;
   private float angleDeg = 0.0F;
   private float velDegPerSec = 0.0F;
   private int dir = 1;
   private long lastUpdateNs = 0L;
   private long nextFlipNs = 0L;
   private int hitBoostQueuedTurns = 0;
   private int hitBoostActiveTurns = 0;
   private int hitBoostSign = 1;
   private float hitBoostT01 = 0.0F;

   private static int accentRgb() {
      return ThemeManager.accentRGB();
   }

   public void render(EventRender3D e, Vec3 worldCenter, float animation, float size, float rotationSpeed) {
      float inv = 1.0F - animation;
      float eased = 1.0F - inv * inv * inv;
      int alpha = (int)(255.0F * eased);
      if (alpha > 0) {
         float drawSize = size * eased;
         float angle = this.tickPropeller(rotationSpeed);
         int argb = alpha << 24 | accentRgb();
         Render3DUtil.drawTexturedBillboard(e, worldCenter, drawSize, angle, TargetEspRenderType.image(TEXTURE), argb, argb, argb, argb);
      }
   }

   public void queueExtraTurns(int turns) {
      if (turns > 0) {
         this.hitBoostQueuedTurns = Math.min(16, this.hitBoostQueuedTurns + turns);
      }
   }

   public void reset() {
      this.lastUpdateNs = 0L;
   }

   private float tickPropeller(float rotationSpeed) {
      float baseVel = rotationSpeed * 100.0F;
      long now = System.nanoTime();
      if (this.lastUpdateNs == 0L) {
         this.lastUpdateNs = now;
         this.nextFlipNs = now + DIRECTION_PERIOD_NS;
         this.velDegPerSec = baseVel;
      }

      float dt = (float)(now - this.lastUpdateNs) / 1.0E9F;
      this.lastUpdateNs = now;

      for (dt = clamp(dt, 0.0F, 0.05F); now >= this.nextFlipNs; this.nextFlipNs += DIRECTION_PERIOD_NS) {
         this.dir = -this.dir;
      }

      float targetVel = this.dir * baseVel;
      float k = 1.0F - (float)Math.exp(-ANGULAR_RESPONSE * dt);
      this.velDegPerSec = this.velDegPerSec + (targetVel - this.velDegPerSec) * k;
      float extra = this.tickHitBoost(dt);
      this.angleDeg = (this.angleDeg + (this.velDegPerSec + extra) * dt) % 360.0F;
      if (this.angleDeg < 0.0F) {
         this.angleDeg += 360.0F;
      }

      return this.angleDeg;
   }

   private float tickHitBoost(float dt) {
      if (this.hitBoostActiveTurns <= 0 && this.hitBoostQueuedTurns > 0) {
         this.hitBoostActiveTurns = this.hitBoostQueuedTurns;
         this.hitBoostQueuedTurns = 0;
         this.hitBoostT01 = 0.0F;
         this.hitBoostSign = this.dir != 0 ? this.dir : (this.velDegPerSec >= 0.0F ? 1 : -1);
      }

      if (this.hitBoostActiveTurns <= 0) {
         return 0.0F;
      }

      float dur = Math.max(0.05F, HIT_BOOST_DURATION_S);
      this.hitBoostT01 = clamp(this.hitBoostT01 + dt / dur, 0.0F, 1.0F);
      float amp = 360.0F * this.hitBoostActiveTurns * (float) Math.PI / (2.0F * dur);
      float extra = this.hitBoostSign * amp * (float)Math.sin(Math.PI * this.hitBoostT01);
      if (this.hitBoostT01 >= 0.9999F) {
         this.hitBoostActiveTurns = 0;
         this.hitBoostT01 = 0.0F;
      }

      return extra;
   }

   private static float clamp(float v, float min, float max) {
      return v < min ? min : (v > max ? max : v);
   }
}
