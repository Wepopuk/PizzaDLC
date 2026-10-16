package pizza.dlc.client.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Elytra Predict",
   description = "Предикт полёта цели на элитрах для наводки AttackAura: точка перехвата и рамка, где её ждать",
   category = ModuleCategory.COMBAT
)
public class ElytraSample extends Module {
   public final ModeSetting mode = new ModeSetting("Режим предикта", this, "Физика", "Adaptive", "Fixed");
   public final SliderSetting distance = new SliderSetting("Упреждение", this, 2.0F, 0.5F, 5.0F, 0.1F);
   public final SliderSetting leadCap = new SliderSetting("Предикт: макс. тиков", this, 20.0F, 1.0F, 40.0F, 1.0F);
   public final SliderSetting climb = new SliderSetting("Превышение", this, 4.0F, -6.0F, 10.0F, 0.5F);
   private LivingEntity velTarget;
   private Vec3 lastServerPos;
   private int ticksSinceServerMove;
   private Vec3 serverVel;
   private int lastSampleTick;
   private static final int STALE_TICKS = 3;
   private static final float ATTACK_R = 3.2F;
   private static final double MAX_AIM_PITCH = 70.0;

   public ElytraSample() {
      this.distance.setVisible(() -> this.mode.is("Fixed"));
      this.leadCap.setVisible(() -> !this.mode.is("Fixed"));
      this.climb.setVisible(() -> this.mode.is("Adaptive"));
      this.serverVel = Vec3.ZERO;
      this.lastSampleTick = Integer.MIN_VALUE;
   }

   public static Vec3 getVelocity(LivingEntity e) {
      if (e == null) {
         return Vec3.ZERO;
      }

      ElytraSample m = module();
      if (m == null) {
         return rawVelocity(e);
      }

      m.sampleServerVelocity(e);
      return m.serverVel.lengthSqr() > 1.0E-6 ? m.serverVel : rawVelocity(e);
   }

   private static Vec3 rawVelocity(LivingEntity e) {
      return new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
   }

   private void sampleServerVelocity(LivingEntity e) {
      int now = mc.player != null ? mc.player.tickCount : 0;
      if (e != this.velTarget) {
         this.velTarget = e;
         this.lastServerPos = serverPos(e);
         this.ticksSinceServerMove = 0;
         this.serverVel = Vec3.ZERO;
         this.lastSampleTick = now;
      } else if (now != this.lastSampleTick) {
         int dt = Math.max(1, now - this.lastSampleTick);
         this.lastSampleTick = now;
         this.ticksSinceServerMove += dt;
         Vec3 sp = serverPos(e);
         if (this.lastServerPos == null) {
            this.lastServerPos = sp;
         } else {
            if (!sp.equals(this.lastServerPos)) {
               this.serverVel = sp.subtract(this.lastServerPos).scale(1.0 / Math.max(1, this.ticksSinceServerMove));
               this.lastServerPos = sp;
               this.ticksSinceServerMove = 0;
            } else if (this.ticksSinceServerMove > STALE_TICKS) {
               this.serverVel = Vec3.ZERO;
            }
         }
      }
   }

   static Vec3 serverPos(LivingEntity e) {
      InterpolationHandler h = e.getInterpolation();
      return h != null ? h.position() : e.position();
   }

   private static ElytraSample module() {
      return PizzaDLCClient.getInstance() != null && PizzaDLCClient.getInstance().getModuleManager() != null
         ? PizzaDLCClient.getInstance().getModuleManager().get(ElytraSample.class)
         : null;
   }

   public static boolean isAdaptive() {
      ElytraSample m = module();
      return m != null && m.isEnabled() && m.mode.is("Adaptive");
   }

   public static Vec3 getOffset(LivingEntity entity) {
      if (mc.player == null || !mc.player.isFallFlying()) {
         return Vec3.ZERO;
      }

      if (entity != null && entity.isFallFlying()) {
         ElytraSample module = module();
         if (module == null || !module.isEnabled() || module.mode.is("Физика")) {
            int ticks = module != null && module.isEnabled() ? Math.round(module.leadCap.getValue()) : 20;
            return ElytraPredict.interceptOffset(entity, ticks);
         }
         if (module != null && module.isEnabled()) {
            Vec3 velocity = getVelocity(entity);
            if (module.mode.is("Adaptive")) {
               return module.adaptiveOffset(entity, velocity);
            }

            double horizLen = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizLen < 0.01) {
               return Vec3.ZERO;
            }

            double dist = module.distance.getValue();
            return new Vec3(velocity.x / horizLen * dist, 0.0, velocity.z / horizLen * dist);
         }

         return Vec3.ZERO;
      }

      return Vec3.ZERO;
   }

   private Vec3 adaptiveOffset(LivingEntity entity, Vec3 tVel) {
      Vec3 eye = mc.player.getEyePosition();
      double dist = Math.sqrt(entity.getBoundingBox().distanceToSqr(eye));
      double fade = Mth.clamp((dist - ATTACK_R) / 5.0, 0.0, 1.0);
      if (fade <= 0.0) {
         return Vec3.ZERO;
      }

      double t = this.interceptTicks(eye, entity.position(), tVel, this.leadCap.getValue());
      Vec3 lead = tVel.scale(t * fade);
      lead = lead.add(0.0, this.climbBias(eye.y - entity.getEyeY()) * fade, 0.0);
      return this.limitPitch(eye, entity.position().add(lead), lead);
   }

   private double climbBias(double have) {
      double want = this.climb.getValue();
      return want >= 0.0 ? Math.max(0.0, want - Math.max(0.0, have)) : Math.min(0.0, want - Math.min(0.0, have));
   }

   private double interceptTicks(Vec3 eye, Vec3 targetPos, Vec3 tVel, double tMax) {
      Vec3 rel = targetPos.subtract(eye);
      double s = mc.player.getDeltaMovement().length();
      if (s < 1.0E-4) {
         return tMax;
      }

      double a = tVel.lengthSqr() - s * s;
      double b = 2.0 * rel.dot(tVel);
      double c = rel.lengthSqr();
      double t;
      if (Math.abs(a) < 1.0E-6) {
         if (Math.abs(b) < 1.0E-6) {
            return tMax;
         }

         t = -c / b;
         if (t <= 0.0) {
            return tMax;
         }
      } else {
         double disc = b * b - 4.0 * a * c;
         if (disc < 0.0) {
            return tMax;
         }

         double sq = Math.sqrt(disc);
         double t1 = (-b - sq) / (2.0 * a);
         double t2 = (-b + sq) / (2.0 * a);
         t = Double.MAX_VALUE;
         if (t1 > 0.0) {
            t = t1;
         }

         if (t2 > 0.0 && t2 < t) {
            t = t2;
         }

         if (t == Double.MAX_VALUE) {
            return tMax;
         }
      }

      return Mth.clamp(t, 0.0, tMax);
   }

   private Vec3 limitPitch(Vec3 eye, Vec3 aim, Vec3 lead) {
      Vec3 d = aim.subtract(eye);
      double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
      double maxVert = horiz * Math.tan(Math.toRadians(MAX_AIM_PITCH));
      return Math.abs(d.y) <= maxVert ? lead : lead.add(0.0, Math.signum(d.y) * maxVert - d.y, 0.0);
   }

   public static AABB getAdjustedBox(LivingEntity entity) {
      Vec3 offset = getOffset(entity);
      return offset == Vec3.ZERO ? entity.getBoundingBox() : entity.getBoundingBox().move(offset);
   }

   public static Vec3 getAdjustedPos(LivingEntity entity) {
      return entity.position().add(getOffset(entity));
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.level == null) return;

      AttackAura aura = PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      if (aura != null && aura.isEnabled()) {
         LivingEntity target = aura.getTarget();
         if (target != null && target.isFallFlying()) {
            Vec3 offset = getOffset(target);
            if (offset != Vec3.ZERO) {
               float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
               AABB box = target.getBoundingBox().move(offset).move(target.getPosition(pt).subtract(target.position()));
               Render3DUtil.drawBox(e, box, 0x80FF0000, true);
            }
         }
      }
   }
}
