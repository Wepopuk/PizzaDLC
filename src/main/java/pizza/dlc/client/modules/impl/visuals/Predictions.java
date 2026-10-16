package pizza.dlc.client.modules.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector2f;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Predictions",
   description = "Траектории метательных (в руке и в полёте)",
   category = ModuleCategory.VISUALS
)
public class Predictions extends Module {
   private static final String PEARLS = "Жемчуг";
   private static final String ARROWS = "Стрелы";
   private static final String SNOWBALLS = "Снежки";
   private static final String POTIONS = "Зелья";
   private static final String TRIDENT = "Трезубец";
   private static final String OTHER = "Прочее";
   public final BooleanSetting inHand = new BooleanSetting("В руке", "Траектория метательного в руке", this);
   public final BooleanSetting worldProj = new BooleanSetting("В полёте", "Траектория летящих снарядов", this);
   public final BooleanSetting path = new BooleanSetting("Путь", "Линия траектории", this);
   public final BooleanSetting landing = new BooleanSetting("Маркер", "Метка в точке падения", this);
   public final BooleanSetting timer = new BooleanSetting("Таймер", "Время до приземления", this);
   public final BooleanSetting themeColor = new BooleanSetting("Цвет темы", "Цвет акцента вместо белого", this);
   public final MultiModeSetting types = new MultiModeSetting("Типы", this, PEARLS, ARROWS, SNOWBALLS, POTIONS, TRIDENT, OTHER);
   private final List<LandingTag> landingTags = new ArrayList<>();
   private Vec3 smoothVel = Vec3.ZERO;

   public Predictions() {
      this.inHand.setEnabled(true);
      this.worldProj.setEnabled(true);
      this.path.setEnabled(true);
      this.landing.setEnabled(true);
      this.timer.setEnabled(true);
      this.themeColor.setEnabled(true);

      for (BooleanSetting b : this.types.getBoolSettings()) {
         b.setEnabled(true);
      }
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      this.landingTags.clear();
      if (mc.level != null && mc.player != null) {
         if (this.inHand.isEnabled()) {
            this.predictInHand(e);
         }

         if (this.worldProj.isEnabled()) {
            this.predictWorld(e);
         }
      }
   }

   @EventHandler
   public void onRender2D(EventRenderWorld2D e) {
      if (!this.timer.isEnabled() || this.landingTags.isEmpty()) return;

      GuiGraphicsExtractor ctx = e.getContext();
      MsdfFont font = PizzaDLCFonts.sfpro();

      for (LandingTag tag : this.landingTags) {
         Vector2f s = ProjectionUtil.worldToScreen(tag.pos());
         if (s != null) {
            this.drawTimer(ctx, font, String.format(Locale.ROOT, "%.1fs", tag.ticks() / 20.0F), s.x, s.y);
         }
      }
   }

   private void drawTimer(GuiGraphicsExtractor ctx, MsdfFont font, String text, float x, float y) {
      float size = 7.5F;
      float pad = 2.5F;
      float tw = Render2DUtil.msdfWidth(font, text, size);
      float th = 0.7F * size;
      float bw = tw + pad * 2.0F;
      float bh = th + pad * 2.0F;
      float bx = x - bw / 2.0F;
      float by = y - bh / 2.0F;
      Render2DUtil.glassRect(ctx, bx, by, bw, bh, 2.5F, 0x70101319);
      Render2DUtil.msdfText(ctx, font, text, bx + pad, by + pad, size, -1);
   }

   private void predictInHand(EventRender3D e) {
      ItemStack stack = this.pickHandThrowable();
      if (stack != null) {
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 start = mc.player.getEyePosition(pt).add(0.0, -0.1, 0.0);
         this.smoothVel = this.smoothVel.add(mc.player.getDeltaMovement().subtract(this.smoothVel).scale(0.3));
         Vec3 pvel = mc.player.onGround() ? new Vec3(this.smoothVel.x, 0.0, this.smoothVel.z) : this.smoothVel;
         if (stack.is(Items.CROSSBOW)) {
            this.predictCrossbow(e, stack, start, pvel, pt);
         } else {
            Spec spec = this.specFor(stack);
            if (spec != null) {
               Vec3 look = mc.player.getViewVector(pt).normalize();
               Vec3 motion = look.scale(spec.speed).add(pvel);
               this.simulate(e, start, motion, spec.gravity, spec.drag, stack.is(Items.BOW), stack.is(Items.TRIDENT));
            }
         }
      }
   }

   private void predictCrossbow(EventRender3D e, ItemStack stack, Vec3 start, Vec3 pvel, float pt) {
      ChargedProjectiles charged = (ChargedProjectiles)stack.get(DataComponents.CHARGED_PROJECTILES);
      if (charged != null && !charged.isEmpty()) {
         List<ItemStack> projs = charged.itemCopies();
         if (!projs.isEmpty()) {
            double speed = projs.get(0).is(Items.FIREWORK_ROCKET) ? 1.6 : 3.15;
            float yaw = mc.player.getViewYRot(pt);
            float pitch = mc.player.getViewXRot(pt);
            this.fireCrossbow(e, start, pvel, pitch, yaw, speed);
            if (projs.size() > 1) {
               this.fireCrossbow(e, start, pvel, pitch, yaw - 10.0F, speed);
               this.fireCrossbow(e, start, pvel, pitch, yaw + 10.0F, speed);
            }
         }
      }
   }

   private void fireCrossbow(EventRender3D e, Vec3 start, Vec3 pvel, float pitch, float yaw, double speed) {
      Vec3 motion = dirFromAngles(pitch, yaw).scale(speed).add(pvel);
      this.simulate(e, start, motion, 0.05, 0.99, true, false);
   }

   private static Vec3 dirFromAngles(float pitch, float yaw) {
      double f = pitch * (Math.PI / 180.0);
      double g = -yaw * (Math.PI / 180.0);
      double h = Math.cos(g);
      double i = Math.sin(g);
      double j = Math.cos(f);
      double k = Math.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   private ItemStack pickHandThrowable() {
      for (ItemStack s : new ItemStack[]{mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
         if (s == null || s.isEmpty()) continue;

         if (s.is(Items.CROSSBOW)) {
            ChargedProjectiles c = (ChargedProjectiles)s.get(DataComponents.CHARGED_PROJECTILES);
            if (c != null && !c.isEmpty() && this.typeEnabledFor(s)) {
               return s;
            }
         } else if (this.specFor(s) != null
            && this.typeEnabledFor(s)
            && (
               !s.is(Items.BOW) && !s.is(Items.TRIDENT)
                  || mc.player.isUsingItem() && mc.player.getUseItem() == s && (!s.is(Items.TRIDENT) || mc.player.getTicksUsingItem() >= 10)
            )) {
            return s;
         }
      }

      return null;
   }

   private Spec specFor(ItemStack s) {
      if (s.is(Items.ENDER_PEARL) || s.is(Items.SNOWBALL) || s.is(Items.EGG)) {
         return new Spec(1.5, 0.03, 0.99);
      }

      if (s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION)) {
         return new Spec(0.5, 0.05, 0.99);
      }

      if (s.is(Items.EXPERIENCE_BOTTLE)) {
         return new Spec(0.7, 0.07, 0.99);
      }

      if (s.is(Items.TRIDENT)) {
         return new Spec(2.5, 0.05, 0.99);
      }

      if (s.is(Items.BOW)) {
         float power = Math.min(1.0F, (mc.player.getTicksUsingItem() + 0.5F) / 20.0F);
         return power < 0.1F ? null : new Spec(3.0 * power, 0.05, 0.99);
      }

      return null;
   }

   private void predictWorld(EventRender3D e) {
      float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

      for (Entity ent : mc.level.entitiesForRendering()) {
         if (!(ent instanceof AbstractArrow || ent instanceof ThrowableItemProjectile) || !this.typeEnabledForWorld(ent)) continue;

         Vec3 motion = ent.getDeltaMovement();
         if (!(motion.lengthSqr() < 1.0E-6)) {
            boolean trident = ent instanceof ThrownTrident;
            boolean arrow = ent instanceof AbstractArrow && !trident;
            Spec spec = !arrow && !trident ? new Spec(0.0, 0.03, 0.99) : new Spec(0.0, 0.05, 0.99);
            this.simulate(e, ent.getPosition(pt), motion, spec.gravity, spec.drag, arrow, trident);
         }
      }
   }

   private void simulate(EventRender3D e, Vec3 pos, Vec3 motion, double gravity, double airDrag, boolean arrow, boolean trident) {
      int color = this.color();

      boolean throwable = !arrow && !trident;
      for (int i = 0; i < 200; i++) {
         Vec3 prev = pos;
         boolean inWater = mc.level.getFluidState(BlockPos.containing(prev)).is(FluidTags.WATER);
         if (throwable) {
            motion = motion.add(0.0, -gravity, 0.0).scale(inWater ? 0.8 : airDrag);
            pos = pos.add(motion);
         } else {
            pos = pos.add(motion);
            double drag = trident ? 0.99 : (inWater ? 0.6 : airDrag);
            motion = motion.scale(drag).add(0.0, -gravity, 0.0);
         }
         HitResult blockHit = mc.level.clip(new ClipContext(prev, pos, Block.COLLIDER, Fluid.NONE, mc.player));
         Vec3 segEnd = blockHit.getType() != Type.MISS ? blockHit.getLocation() : pos;
         EntityHitResult entHit = ProjectileUtil.getEntityHitResult(
            mc.level,
            mc.player,
            prev,
            segEnd,
            new AABB(prev, segEnd).inflate(1.0),
            ent -> ent != mc.player && ent.canBeHitByProjectile() && !ent.isSpectator(),
            0.3F
         );
         HitResult hit = (HitResult)(entHit != null ? entHit : blockHit);
         Vec3 end = hit.getType() != Type.MISS ? hit.getLocation() : pos;
         if (this.path.isEnabled()) {
            Render3DUtil.drawLine(e, prev.x, prev.y, prev.z, end.x, end.y, end.z, color);
         }

         if (hit.getType() != Type.MISS) {
            if (this.landing.isEnabled()) {
               Direction face = hit instanceof BlockHitResult bhr ? bhr.getDirection() : Direction.UP;
               this.drawMarker(e, end, face, color);
            }

            if (this.timer.isEnabled()) {
               this.landingTags.add(new LandingTag(end, i + 1));
            }

            return;
         }

         if (pos.y < -256.0) {
            return;
         }
      }
   }

   private void drawMarker(EventRender3D e, Vec3 center, Direction face, int color) {
      double r = 0.3;
      int steps = 24;
      Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
      if (n.lengthSqr() < 1.0E-6) {
         n = new Vec3(0.0, 1.0, 0.0);
      }

      n = n.normalize();
      center = center.add(n.scale(0.01));
      Vec3 a = Math.abs(n.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 u = n.cross(a).normalize();
      Vec3 v = n.cross(u).normalize();
      Vec3 prev = null;

      for (int i = 0; i <= steps; i++) {
         double t = (Math.PI * 2) * i / steps;
         Vec3 p = center.add(u.scale(Math.cos(t) * r)).add(v.scale(Math.sin(t) * r));
         if (prev != null) {
            line(e, prev, p, color);
         }

         prev = p;
      }

      line(e, center.add(u.scale(-r)), center.add(u.scale(r)), color);
      line(e, center.add(v.scale(-r)), center.add(v.scale(r)), color);
   }

   private static void line(EventRender3D e, Vec3 a, Vec3 b, int c) {
      Render3DUtil.drawLine(e, a.x, a.y, a.z, b.x, b.y, b.z, c);
   }

   private int color() {
      return this.themeColor.isEnabled() ? ThemeManager.accent() : -1;
   }

   private boolean typeEnabled(String type) {
      BooleanSetting b = this.types.get(type);
      return b == null || b.isEnabled();
   }

   private boolean typeEnabledFor(ItemStack s) {
      if (s.is(Items.ENDER_PEARL)) {
         return this.typeEnabled(PEARLS);
      }

      if (s.is(Items.SNOWBALL)) {
         return this.typeEnabled(SNOWBALLS);
      }

      if (s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION)) {
         return this.typeEnabled(POTIONS);
      }

      if (s.is(Items.TRIDENT)) {
         return this.typeEnabled(TRIDENT);
      }

      return !s.is(Items.BOW) && !s.is(Items.CROSSBOW) ? this.typeEnabled(OTHER) : this.typeEnabled(ARROWS);
   }

   private boolean typeEnabledForWorld(Entity ent) {
      if (!(ent instanceof AbstractArrow)) {
         if (ent instanceof ThrowableItemProjectile thrown) {
            Item it = thrown.getItem().getItem();
            if (it == Items.ENDER_PEARL) {
               return this.typeEnabled(PEARLS);
            }

            if (it == Items.SNOWBALL) {
               return this.typeEnabled(SNOWBALLS);
            }

            return it != Items.SPLASH_POTION && it != Items.LINGERING_POTION ? this.typeEnabled(OTHER) : this.typeEnabled(POTIONS);
         }

         return this.typeEnabled(OTHER);
      }

      return this.typeEnabled(ARROWS) || this.typeEnabled(TRIDENT);
   }

   private record LandingTag(Vec3 pos, int ticks) {
   }

   private record Spec(double speed, double gravity, double drag) {
   }
}
