package pizza.dlc.client.modules.impl.visuals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3fc;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Arrows",
   description = "Показывает направление к сущностям",
   category = ModuleCategory.VISUALS
)
public class Arrows extends Module {
   private static final Identifier ARROW_TEXTURE = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/arrows/arrow.png");
   private static final String PLAYERS = "Игроки";
   private static final String MOBS = "Мобы";
   private static final String ANIMALS = "Животные";
   private static final String INVISIBLES = "Невидимые";
   private static final String FRIENDS = "Друзья";
   private static final String NAKED_PLAYERS = "Голые игроки";
   private static final float SHIMMER_PERIOD_MS = 5000.0F;
   private static final float SHIMMER_SPAN = 0.28F;
   private static final float SHIMMER_DEPTH = 0.7F;
   private static final float FADE_OUT_PER_SEC = 11.0F;
   private static final float TURN_PER_SEC = 18.0F;
   private static final float FLIP_DEAD_ZONE = 0.18F;
   private static final float POP_SCALE = 0.45F;
   public final BooleanSetting lines = new BooleanSetting("Линии", "Рисует 3D-линии вместо HUD-стрелок", this);
   public final BooleanSetting onlyOffscreen = new BooleanSetting("Только вне экрана", this);
   public final BooleanSetting showDistance = new BooleanSetting("Дистанция", this);
   public final SliderSetting range = new SliderSetting("Радиус поиска", this, 96.0F, 8.0F, 256.0F, 1.0F);
   public final SliderSetting radius = new SliderSetting("Радиус стрелок", this, 44.0F, 16.0F, 140.0F, 1.0F).setVisible(() -> !this.lines.isEnabled());
   public final SliderSetting size = new SliderSetting("Размер стрелки", this, 15.0F, 6.0F, 32.0F, 0.5F).setVisible(() -> !this.lines.isEnabled());
   public final MultiModeSetting targets = new MultiModeSetting("Цели", this, PLAYERS, MOBS, ANIMALS, INVISIBLES, FRIENDS, NAKED_PLAYERS);
   private final BooleanSetting tPlayers = this.targets.get(PLAYERS);
   private final BooleanSetting tMobs = this.targets.get(MOBS);
   private final BooleanSetting tAnimals = this.targets.get(ANIMALS);
   private final BooleanSetting tInvisibles = this.targets.get(INVISIBLES);
   private final BooleanSetting tFriends = this.targets.get(FRIENDS);
   private final BooleanSetting tNaked = this.targets.get(NAKED_PLAYERS);
   private final Vector2f screenScratch = new Vector2f();
   private float dirX;
   private float dirY;
   private final Map<Integer, ArrowState> states = new HashMap<>();
   private long lastFrameMs;

   public Arrows() {
      this.onlyOffscreen.setEnabled(true);
      this.targets.get(PLAYERS).setEnabled(true);
      this.targets.get(INVISIBLES).setEnabled(true);
      this.targets.get(FRIENDS).setEnabled(true);
      this.targets.get(NAKED_PLAYERS).setEnabled(true);
   }

   @EventHandler
   public void onRender2D(EventRenderWorld2D event) {
      if (this.lines.isEnabled() || mc.level == null || mc.player == null) return;

      GuiGraphicsExtractor ctx = event.getContext();
      Camera camera = mc.gameRenderer.mainCamera();
      Vec3 cameraPos = camera.position();
      Vector3fc forward = camera.forwardVector();
      Vector3fc left = camera.leftVector();
      float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      float cx = ctx.guiWidth() * 0.5F;
      float cy = ctx.guiHeight() * 0.5F;
      float arrowRadius = this.radius.getValue();
      float arrowSize = this.size.getValue();
      MsdfFont font = PizzaDLCFonts.sfpro();
      float rangeSq = this.range.getValue() * this.range.getValue();
      boolean offscreenOnly = this.onlyOffscreen.isEnabled();
      boolean withDistance = this.showDistance.isEnabled();
      int accent = ThemeManager.accent();
      int secondary = ThemeManager.secondary();
      float guiW = ctx.guiWidth();
      float guiH = ctx.guiHeight();
      float phase = (float)(System.currentTimeMillis() % 5000L) / SHIMMER_PERIOD_MS;
      float dt = this.frameDelta();

      for (ArrowState s : this.states.values()) {
         s.seen = false;
      }

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!(entity instanceof LivingEntity living) || living == mc.player) continue;

         double distSq = mc.player.distanceToSqr(living);
         if (!(distSq > rangeSq)) {
            boolean friend = isFriend(living);
            if (this.isTargetValid(living, friend)) {
               Vec3 pos = living.getPosition(pt);
               double wy = pos.y + living.getBbHeight() * 0.55;
               if ((!offscreenOnly || !this.isOnScreen(pos.x, wy, pos.z, guiW, guiH)) && this.screenDirection(pos.x, wy, pos.z, cameraPos, forward, left)
               )
               {
                  ArrowState s = this.states.computeIfAbsent(living.getId(), id -> new ArrowState());
                  s.seen = true;
                  s.colorA = friend ? 0xFF55FF77 : accent;
                  s.colorB = friend ? 0xFFB6FFCB : secondary;
                  s.shimmer = phase + living.getId() * 0.137F;
                  s.distance = (float)Math.sqrt(distSq);
                  float target = (float)Math.atan2(this.dirY, this.dirX);
                  if (!s.spawned) {
                     s.spawned = true;
                     s.angle = target;
                     s.flip = this.dirY < 0.0F;
                     s.flipT = 1.0F;
                  } else {
                     s.angle = s.angle + wrapPi(target - s.angle) * (1.0F - (float)Math.exp(-TURN_PER_SEC * dt));
                     boolean want = this.dirY < 0.0F;
                     if (want != s.flip && Math.abs(this.dirY) > FLIP_DEAD_ZONE) {
                        s.flip = want;
                        s.flipT = 0.0F;
                     }
                  }

                  s.alpha = Math.min(1.0F, s.alpha + dt * 7.0F);
                  s.flipT = Math.min(1.0F, s.flipT + dt * 6.0F);
               }
            }
         }
      }

      Iterator<Entry<Integer, ArrowState>> it = this.states.entrySet().iterator();

      while (it.hasNext()) {
         ArrowState s = it.next().getValue();
         if (!s.seen) {
            s.alpha -= dt * FADE_OUT_PER_SEC;
            if (s.alpha <= 0.0F) {
               it.remove();
               continue;
            }
         }

         float dx = Mth.cos(s.angle);
         float dy = Mth.sin(s.angle);
         float scale = 1.0F - POP_SCALE * (1.0F - s.alpha);
         this.drawArrow(ctx, cx, cy, dx, dy, arrowRadius, arrowSize * scale, s.colorA, s.colorB, s.shimmer, s.alpha);
         if (withDistance && s.seen) {
            this.drawDistance(ctx, font, cx, cy, dx, dy, arrowRadius, s.distance, s.flip, s.alpha * s.flipT);
         }
      }
   }

   private float frameDelta() {
      long now = System.currentTimeMillis();
      float dt = this.lastFrameMs == 0L ? 0.016666668F : (float)(now - this.lastFrameMs) / 1000.0F;
      this.lastFrameMs = now;
      return Mth.clamp(dt, 0.0F, 0.1F);
   }

   private static float wrapPi(float radians) {
      float r = radians % (float) (Math.PI * 2);
      if (r > (float) Math.PI) {
         r -= (float) (Math.PI * 2);
      }

      if (r < (float) -Math.PI) {
         r += (float) (Math.PI * 2);
      }

      return r;
   }

   @Override
   public void onEnable() {
      this.states.clear();
      this.lastFrameMs = 0L;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.states.clear();
      super.onDisable();
   }

   @EventHandler
   public void onRender3D(EventRender3D event) {
      if (!this.lines.isEnabled() || mc.level == null || mc.player == null) return;

      float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      Vec3 start = mc.player.getEyePosition();
      int baseColor = ThemeManager.accent();
      float rangeSq = this.range.getValue() * this.range.getValue();
      boolean offscreenOnly = this.onlyOffscreen.isEnabled();
      float guiW = mc.getWindow().getGuiScaledWidth();
      float guiH = mc.getWindow().getGuiScaledHeight();
      Render3DUtil.drawLines(event, w -> {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || mc.player.distanceToSqr(living) > rangeSq) continue;

            boolean friend = isFriend(living);
            if (this.isTargetValid(living, friend)) {
               Vec3 pos = living.getPosition(pt);
               double wy = pos.y + living.getBbHeight() * 0.55;
               if (!offscreenOnly || !this.isOnScreen(pos.x, wy, pos.z, guiW, guiH)) {
                  w.line(start.x, start.y, start.z, pos.x, wy, pos.z, friend ? 0xFF55FF77 : baseColor);
               }
            }
         }
      });
   }

   private boolean screenDirection(double tx, double ty, double tz, Vec3 cameraPos, Vector3fc forward, Vector3fc left) {
      double dx = tx - cameraPos.x;
      double dy = ty - cameraPos.y;
      double dz = tz - cameraPos.z;
      float screenX = (float)(-(dx * left.x() + dy * left.y() + dz * left.z()));
      float screenY = (float)(-(dx * forward.x() + dy * forward.y() + dz * forward.z()));
      float len = (float)Math.sqrt(screenX * screenX + screenY * screenY);
      if (!(len < 1.0E-4F) && Float.isFinite(len)) {
         this.dirX = screenX / len;
         this.dirY = screenY / len;
         return true;
      }

      return false;
   }

   private void drawArrow(
      GuiGraphicsExtractor ctx, float cx, float cy, float dx, float dy, float radius, float size, int colorA, int colorB, float phase, float alpha
   ) {
      int side = Math.max(1, Math.round(size));
      int x = Math.round(-side * 0.5F);
      int y = Math.round(radius);
      float angle = (float)(Math.atan2(dy, dx) - (Math.PI / 2));
      int top = mulAlpha(Render2DUtil.lerpColor(colorA, colorB, shimmer(phase)), alpha);
      int bottom = mulAlpha(Render2DUtil.lerpColor(colorA, colorB, shimmer(phase + SHIMMER_SPAN)), alpha);
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().rotate(angle);
      Render2DUtil.imageGradient(ctx, ARROW_TEXTURE, x, y, x + side, y + side, 1.0F, 0.0F, 1.0F, 0.0F, top, bottom);
      ctx.pose().popMatrix();
   }

   private static float shimmer(float phase) {
      float wave = 0.5F + 0.5F * Mth.sin(phase * 2.0F * (float) Math.PI);
      return 0.5F + (wave - 0.5F) * SHIMMER_DEPTH;
   }

   private void drawDistance(
      GuiGraphicsExtractor ctx, MsdfFont font, float cx, float cy, float dx, float dy, float arrowRadius, float distance, boolean flip, float alpha
   ) {
      if (alpha <= 0.02F) return;

      String text = Math.round(distance) + "m";
      float textSize = 7.0F;
      float textW = Render2DUtil.msdfWidth(font, text, textSize);
      float br = arrowRadius - textSize;
      float tcx = cx + dx * br;
      float tcy = cy + dy * br;
      float perp = (float)(Math.atan2(dy, dx) - (Math.PI / 2));
      ctx.pose().pushMatrix();
      ctx.pose().translate(tcx, tcy);
      ctx.pose().rotate(flip ? perp + (float) Math.PI : perp);
      Render2DUtil.msdfText(ctx, font, text, -textW * 0.5F, -textSize * 0.5F, textSize, mulAlpha(0xE6FFFFFF, alpha));
      ctx.pose().popMatrix();
   }

   private static int mulAlpha(int argb, float alpha01) {
      int a = argb >>> 24 & 0xFF;
      int scaled = Mth.clamp(Math.round(a * Mth.clamp(alpha01, 0.0F, 1.0F)), 0, 255);
      return scaled << 24 | argb & 0xFFFFFF;
   }

   private boolean isOnScreen(double wx, double wy, double wz, float width, float height) {
      return !ProjectionUtil.worldToScreen(wx, wy, wz, this.screenScratch)
         ? false
         : this.screenScratch.x >= 0.0F && this.screenScratch.x <= width && this.screenScratch.y >= 0.0F && this.screenScratch.y <= height;
   }

   private boolean isTargetValid(LivingEntity entity, boolean friend) {
      if (!entity.isAlive() || entity.isSpectator()) {
         return false;
      }

      if (!this.tInvisibles.isEnabled() && entity.isInvisible()) {
         return false;
      }

      if (entity instanceof Player player) {
         if (friend && !this.tFriends.isEnabled()) {
            return false;
         }

         boolean allowPlayers = this.tPlayers.isEnabled();
         boolean allowNaked = this.tNaked.isEnabled();
         if (allowPlayers && allowNaked) {
            return true;
         }

         if (!allowPlayers && !allowNaked) {
            return false;
         }

         boolean naked = isNaked(player);
         return allowNaked ? naked : !naked;
      }

      if (entity instanceof Animal) {
         return this.tAnimals.isEnabled();
      }

      return (entity instanceof Enemy || entity instanceof Mob) && this.tMobs.isEnabled();
   }

   private static boolean isNaked(Player player) {
      return empty(player.getItemBySlot(EquipmentSlot.HEAD))
         && empty(player.getItemBySlot(EquipmentSlot.CHEST))
         && empty(player.getItemBySlot(EquipmentSlot.LEGS))
         && empty(player.getItemBySlot(EquipmentSlot.FEET));
   }

   private static boolean empty(ItemStack stack) {
      return stack == null || stack.isEmpty();
   }

   private static boolean isFriend(LivingEntity entity) {
      return entity instanceof Player
         && PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(entity.getName().getString());
   }

   private static final class ArrowState {
      float angle;
      float alpha;
      int colorA;
      int colorB;
      float shimmer;
      float distance;
      boolean flip;
      float flipT;
      boolean seen;
      boolean spawned;
   }
}
