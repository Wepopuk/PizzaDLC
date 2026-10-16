package pizza.dlc.client.modules.impl.visuals;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.api.util.render.targetesp.ImageTargetRenderer;
import pizza.dlc.api.util.render.targetesp.ScanRingRenderType;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.combat.CombatSync;

@ModuleInfo(
   name = "Target ESP",
   category = ModuleCategory.VISUALS
)
public class TargetEsp extends Module {
   public static TargetEsp INSTANCE;
   public final ModeSetting style = new ModeSetting("Стиль", this, "Спиннер", "Jello");
   public final SliderSetting size = new SliderSetting("Размер", this, 0.5F, 0.2F, 1.5F, 0.05F);
   public final SliderSetting rotationSpeed = new SliderSetting("Скорость вращения", this, 2.0F, 0.0F, 6.0F, 0.1F);
   public final SliderSetting scanSpeed = new SliderSetting("Скорость скана", this, 1.2F, 0.2F, 4.0F, 0.1F);
   public final SliderSetting trail = new SliderSetting("Трейл", this, 0.6F, 0.0F, 1.0F, 0.05F);
   public final BooleanSetting hitBoost = new BooleanSetting("Рывок при ударе", this);
   private final ImageTargetRenderer imageRenderer = new ImageTargetRenderer();
   private float animation = 0.0F;
   private LivingEntity lastTarget = null;
   private long lastMs = 0L;

   public TargetEsp() {
      INSTANCE = this;
      this.hitBoost.setEnabled(true);
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.level == null || mc.player == null) return;

      LivingEntity target = this.currentTarget();
      long now = System.currentTimeMillis();
      if (this.lastMs == 0L) {
         this.lastMs = now;
      }

      float delta = (float)(now - this.lastMs) / 1000.0F * 4.0F;
      this.lastMs = now;
      if (target != null) {
         this.lastTarget = target;
         this.animation = Math.min(1.0F, this.animation + delta);
      } else {
         this.animation = Math.max(0.0F, this.animation - delta);
      }

      if (!(this.animation <= 0.0F) && this.lastTarget != null) {
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 ip = this.lastTarget.getPosition(pt);
         AABB bb = this.lastTarget.getBoundingBox().move(ip.subtract(this.lastTarget.position()));
         if (this.style.is("Jello")) {
            this.renderJello(e, bb);
         } else {
            Vec3 worldCenter = new Vec3((bb.minX + bb.maxX) / 2.0, (bb.minY + bb.maxY) / 2.0, (bb.minZ + bb.maxZ) / 2.0);
            this.imageRenderer.render(e, worldCenter, this.animation, this.size.getValue(), this.rotationSpeed.getValue());
         }
      }
   }

   private void renderJello(EventRender3D e, AABB bb) {
      float inv = 1.0F - this.animation;
      float eased = 1.0F - inv * inv * inv;
      int a = (int)(255.0F * eased);
      if (a > 0) {
         double cx = (bb.minX + bb.maxX) / 2.0;
         double cz = (bb.minZ + bb.maxZ) / 2.0;
         double h = bb.maxY - bb.minY;
         float quadR = (float)Math.max(bb.getXsize(), bb.getZsize()) * (0.5F + this.size.getValue() * 0.5F);
         float cylR = quadR * 0.8F;
         int rgb = ThemeManager.accentRGB();
         double phase = System.nanoTime() / 1.0E9 * this.scanSpeed.getValue();
         float t = (float)(0.5 - 0.5 * Math.cos(phase * Math.PI));
         double ringY = bb.minY + t * h;
         double s = Math.sin(phase * Math.PI);
         double trailEndY = Math.max(bb.minY, Math.min(bb.maxY, ringY - h * this.trail.getValue() * s));
         double loY = Math.min(ringY, trailEndY);
         double hiY = Math.max(ringY, trailEndY);
         boolean ringLow = ringY <= trailEndY;
         int bright = (int)(a * 0.6F) << 24 | rgb;
         Render3DUtil.drawCylinder(e, cx, cz, loY, hiY, cylR, ringLow ? bright : rgb, ringLow ? rgb : bright, 40, true);
         int ring = a << 24 | rgb;
         Render3DUtil.drawTexturedGround(e, new Vec3(cx, ringY, cz), quadR, 0.0F, ScanRingRenderType.get(), ring, ring, ring, ring);
      }
   }

   private LivingEntity currentTarget() {
      LivingEntity target = pizza.dlc.client.modules.impl.combat.CombatTarget.current();
      return target != null ? target : CombatSync.sharedTarget(12.0);
   }

   public static void notifyHit() {
      if (INSTANCE == null || !INSTANCE.isEnabled()) return;

      if (INSTANCE.hitBoost.isEnabled()) {
         INSTANCE.imageRenderer.queueExtraTurns(1);
      }
   }

   @Override
   public void onEnable() {
      this.animation = 0.0F;
      this.lastTarget = null;
      this.lastMs = 0L;
      this.imageRenderer.reset();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.animation = 0.0F;
      this.lastTarget = null;
      super.onDisable();
   }
}
