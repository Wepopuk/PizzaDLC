package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.combat.CombatSync;
import pizza.dlc.client.modules.impl.misc.FixHP;

public final class TargetHud implements QuickImports {
   private static final int W = 128;
   private static final int H = 38;
   private static final int PAD = 6;
   private static final int HEAD = 26;
   private static final float HEAD_RADIUS = 5.0F;
   private static final int RING = 30;
   private static final float RING_RADIUS_N = 0.45F;
   private static final float RING_THICK_N = 0.09F;
   private static final float ABSORB_RADIUS_N = 0.34F;
   private static final float ABSORB_THICK_N = 0.06F;
   private static final int ABSORB_COLOR = 0xFFFFC94D;
   private static final float NAME_SIZE = 8.5F;
   private static final float INFO_SIZE = 6.5F;
   private static final float HP_SIZE = 7.0F;
   private static final float BAR_H = 3.0F;
   private static final int ITEM_SLOT = 10;
   private static final int MARGIN_TOP = 30;
   private static final int HURT_COLOR = 0xFFFF3434;
   private static final int HURT_TINT = 0xFFFF4A4A;
   private static final int HURT_TEXT = 0xFFFF5A5A;
   private static final float HURT_FADE = 0.55F;
   private static final float SHAKE_DECAY = 7.0F;
   private static final float SHAKE_X = 2.6F;
   private static final float SHAKE_Y = 1.2F;
   private static final float SHAKE_TILT = 0.14F;
   private static final float RING_TIME = 0.45F;
   private static final float HEAD_PUNCH = 9.0F;
   private static final float HEAD_STIFFNESS = 260.0F;
   private static final float HEAD_DAMPING = 14.0F;
   private static final float DAMAGE_TEXT_TIME = 0.9F;
   private static final float DAMAGE_TEXT_SIZE = 7.0F;
   private static final float PART_GRAVITY = 48.0F;
   private static final int STAR_POINTS = 4;
   private static final float STAR_M = 2.3F;
   private final Draggable drag = new Draggable("TargetHUD", 0.0F, 0.0F, 0.0F, 0.0F);
   private LivingEntity displayTarget = null;
   private UUID lastUuid = null;
   private float hudVis = 0.0F;
   private float animHp = 0.0F;
   private float animAbsorb = 0.0F;
   private float trailHp = 0.0F;
   private long lastNs = 0L;
   private int lastHurtTime = 0;
   private float hurtFlash = 0.0F;
   private float hurtAge = 10.0F;
   private float headScale = 1.0F;
   private float headScaleVel;
   private float lastTotalHp = -1.0F;
   private final List<TargetHud.DamageText> damageTexts = new ArrayList<>();
   private final List<Particle> particles = new ArrayList<>();
   private final Random rng = new Random();

   public void render(GuiGraphicsExtractor ctx, boolean editMode, boolean stars, boolean hpBar) {
      if (mc.player == null || mc.level == null) return;

      LivingEntity target = this.currentTarget();
      if (target == null && editMode) {
         target = mc.player;
      }

      if (target != null) {
         this.displayTarget = target;
      }

      long now = System.nanoTime();
      if (this.lastNs == 0L) {
         this.lastNs = now;
      }

      float dt = clamp((float)(now - this.lastNs) / 1.0E9F, 0.0F, 0.1F);
      this.lastNs = now;
      boolean visible = target != null;
      this.hudVis = HudStyle.expLerp(this.hudVis, visible ? 1.0F : 0.0F, visible ? 12.0F : 16.0F, dt);
      if (target == null && this.hudVis < 0.02F) {
         this.displayTarget = null;
         this.lastUuid = null;
         this.lastNs = 0L;
         this.hudVis = 0.0F;
         this.particles.clear();
         this.damageTexts.clear();
         this.hurtFlash = 0.0F;
         this.hurtAge = 10.0F;
         this.headScale = 1.0F;
         this.headScaleVel = 0.0F;
         this.lastTotalHp = -1.0F;
         this.lastHurtTime = 0;
      } else if (this.displayTarget != null) {
         LivingEntity t = this.displayTarget;
         UUID uuid = t.getUUID();
         float hpNow = Math.max(0.0F, FixHP.getHealth(t));
         if (!uuid.equals(this.lastUuid)) {
            this.lastUuid = uuid;
            this.animHp = hpNow;
            this.trailHp = hpNow;
            this.animAbsorb = Math.max(0.0F, t.getAbsorptionAmount());
            this.lastHurtTime = t.hurtTime;
            this.hurtFlash = 0.0F;
            this.hurtAge = 10.0F;
            this.lastTotalHp = -1.0F;
            this.particles.clear();
            this.damageTexts.clear();
         }

         this.animHp = HudStyle.expLerp(this.animHp, hpNow, 12.0F, dt);
         this.trailHp = this.trailHp < this.animHp ? this.animHp : HudStyle.expLerp(this.trailHp, this.animHp, 3.0F, dt);
         this.animAbsorb = HudStyle.expLerp(this.animAbsorb, Math.max(0.0F, t.getAbsorptionAmount()), 10.0F, dt);
         int ht = t.hurtTime;
         boolean justHit = ht > this.lastHurtTime;
         this.lastHurtTime = ht;
         if (justHit) {
            this.hurtFlash = 1.0F;
            this.hurtAge = 0.0F;
            this.headScaleVel = -HEAD_PUNCH;
         }

         float totalHp = hpNow + Math.max(0.0F, t.getAbsorptionAmount());
         if (this.lastTotalHp >= 0.0F && totalHp < this.lastTotalHp - 0.05F) {
            this.spawnDamageText(this.lastTotalHp - totalHp);
         }
         this.lastTotalHp = totalHp;
         this.hurtFlash = Math.max(0.0F, this.hurtFlash - dt / HURT_FADE);
         this.hurtAge = Math.min(10.0F, this.hurtAge + dt);
         this.stepHeadSpring(dt);
         this.drag.setWidth(W);
         this.drag.setHeight(H);
         if (!this.drag.isPositioned()) {
            this.drag.setX((ctx.guiWidth() - W) / 2.0F);
            this.drag.setY(ctx.guiHeight() / 2.0F + MARGIN_TOP);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
         }

         this.drag.update(ctx);
         int x = (int)this.drag.getRenderX();
         int y = (int)this.drag.getRenderY();
         float alpha = smoothstep(this.hudVis);
         if (!(alpha < 0.01F)) {
            float scale = 0.92F + 0.08F * alpha;
            ctx.pose().pushMatrix();
            ctx.pose().translate(x + W / 2.0F, y + H / 2.0F);
            ctx.pose().scale(scale, scale);
            ctx.pose().translate(-(x + W / 2.0F), -(y + H / 2.0F));
            HudStyle.panel(ctx, x, y, W, H, HudStyle.RADIUS + 2.0F, alpha);
            int headX = x + PAD;
            int headY = y + (H - HEAD) / 2;
            if (justHit) {
               this.spawnHurtParticles(headX + HEAD / 2.0F, headY + HEAD / 2.0F);
            }

            this.updateAndRenderParticles(ctx, dt, alpha, stars);
            this.drawHead(ctx, t, headX, headY, alpha);
            this.drawDamageTexts(ctx, headX, headY, dt, alpha);
            float max = Math.max(1.0F, FixHP.getMaxHealth(t));
            float frac = clamp(this.animHp / max, 0.0F, 1.0F);
            float trailFrac = clamp(this.trailHp / max, 0.0F, 1.0F);
            float absorbFrac = clamp(this.animAbsorb / max, 0.0F, 1.0F);
            if (hpBar) {
               this.drawBarLayout(ctx, t, x, y, headX, frac, trailFrac, absorbFrac, alpha);
            } else {
               this.drawRingLayout(ctx, t, x, y, headX, frac, absorbFrac, alpha);
            }

            ctx.pose().popMatrix();
         }
      }
   }

   private void drawHead(GuiGraphicsExtractor ctx, LivingEntity t, int headX, int headY, float alpha) {
      Identifier skin = this.skinOf(t);
      float red = this.hurtFlash * this.hurtFlash * (3.0F - 2.0F * this.hurtFlash);
      float decay = (float)Math.exp(-this.hurtAge * SHAKE_DECAY);
      float shakeX = (float)Math.sin(this.hurtAge * 58.0F) * SHAKE_X * decay;
      float shakeY = (float)Math.cos(this.hurtAge * 47.0F) * SHAKE_Y * decay;
      float tilt = (float)Math.sin(this.hurtAge * 40.0F) * SHAKE_TILT * decay;
      float cx = headX + HEAD / 2.0F;
      float cy = headY + HEAD / 2.0F;

      if (this.hurtAge < RING_TIME) {
         float p = this.hurtAge / RING_TIME;
         float grow = (1.0F - (1.0F - p) * (1.0F - p)) * 9.0F;
         int ringA = Math.round(200.0F * (1.0F - p) * (1.0F - p) * alpha);
         int ringSize = Math.round(HEAD + 2 + grow * 2.0F);
         Render2DUtil.roundedOutline(ctx, Math.round(headX - 1 - grow), Math.round(headY - 1 - grow), ringSize, ringSize,
            HEAD_RADIUS + 1.0F + grow, 1.2F, ringA << 24 | HURT_COLOR & 0xFFFFFF);
      }

      ctx.pose().pushMatrix();
      ctx.pose().translate(cx + shakeX, cy + shakeY);
      ctx.pose().rotate(tilt);
      ctx.pose().scale(this.headScale, this.headScale);
      ctx.pose().translate(-cx, -cy);
      int glowColor = Render2DUtil.lerpColor(Accent.color(1.0F), HURT_COLOR, red);
      Render2DUtil.glow(ctx, headX - 1, headY - 1, HEAD + 2, HEAD + 2, Math.round(HEAD_RADIUS + 1.0F),
         HudStyle.fade(glowColor & 0xFFFFFF | 0x73000000, alpha * (0.35F + 0.65F * red)), 5);
      int frameA = Render2DUtil.lerpColor(Accent.color(1.0F), HURT_COLOR, red);
      int frameB = Render2DUtil.lerpColor(Accent.secondary(1.0F), 0xFFB01E1E, red);
      Render2DUtil.gradientRectDiag(ctx, headX - 1, headY - 1, HEAD + 2, HEAD + 2, HudStyle.fade(frameA, alpha), HudStyle.fade(frameB, alpha), HEAD_RADIUS + 1.0F);
      int faceTint = HudStyle.fade(Render2DUtil.lerpColor(-1, HURT_TINT, red), alpha);
      Render2DUtil.roundedFace(ctx, skin, headX, headY, HEAD, Math.round(HEAD_RADIUS), 8, 8, faceTint);
      Render2DUtil.roundedFace(ctx, skin, headX, headY, HEAD, Math.round(HEAD_RADIUS), 40, 8, faceTint);
      if (red > 0.01F) {
         Render2DUtil.roundedRect(ctx, headX, headY, HEAD, HEAD, HEAD_RADIUS, HudStyle.fade(HURT_COLOR & 0xFFFFFF | 0x66000000, alpha * red));
      }
      ctx.pose().popMatrix();
   }

   private void stepHeadSpring(float dt) {
      int steps = Math.max(1, (int)Math.ceil(dt / 0.004F));
      float h = dt / steps;
      for (int i = 0; i < steps; i++) {
         float force = (1.0F - this.headScale) * HEAD_STIFFNESS - this.headScaleVel * HEAD_DAMPING;
         this.headScaleVel += force * h;
         this.headScale += this.headScaleVel * h;
      }
      this.headScale = clamp(this.headScale, 0.7F, 1.25F);
   }

   private void spawnDamageText(float amount) {
      if (this.damageTexts.size() >= 6) this.damageTexts.remove(0);
      TargetHud.DamageText d = new TargetHud.DamageText();
      d.text = String.format(Locale.US, "-%.1f", amount);
      d.drift = (this.rng.nextFloat() - 0.5F) * 10.0F;
      this.damageTexts.add(d);
   }

   private void drawDamageTexts(GuiGraphicsExtractor ctx, int headX, int headY, float dt, float alpha) {
      Iterator<TargetHud.DamageText> it = this.damageTexts.iterator();
      MsdfFont font = HudStyle.title();
      while (it.hasNext()) {
         TargetHud.DamageText d = it.next();
         d.age += dt;
         float p = d.age / DAMAGE_TEXT_TIME;
         if (p >= 1.0F) {
            it.remove();
            continue;
         }
         float pop = p < 0.15F ? 1.0F + 0.45F * (float)Math.sin(p / 0.15F * Math.PI) : 1.0F;
         float fade = 1.0F - p * p;
         float size = DAMAGE_TEXT_SIZE * pop;
         float tw = font.width(d.text, size);
         float tx = headX + HEAD - tw / 2.0F + d.drift * p;
         float ty = headY - 2.0F - p * 16.0F;
         Render2DUtil.msdfText(ctx, font, d.text, tx + 0.6F, ty + 0.6F, size, HudStyle.fade(0xC0000000, alpha * fade));
         Render2DUtil.msdfText(ctx, font, d.text, tx, ty, size, HudStyle.fade(HURT_TEXT, alpha * fade));
      }
   }

   private void drawBarLayout(GuiGraphicsExtractor ctx, LivingEntity t, int x, int y, int headX, float frac, float trailFrac, float absorbFrac, float alpha) {
      MsdfFont titleFont = HudStyle.title();
      MsdfFont bodyFont = HudStyle.body();
      float cx = headX + HEAD + 7;
      float right = x + W - PAD;
      String hp = String.format(Locale.US, "%.1f", this.animHp + this.animAbsorb);
      float hpW = HudStyle.pillWidth(hp);
      float topY = y + 7;
      HudStyle.pill(ctx, right - hpW, topY - 1.0F, hpW, hp, absorbFrac > 0.001F ? ABSORB_COLOR : HudStyle.TEXT, alpha);
      String name = HudStyle.truncate(titleFont, t.getName().getString(), NAME_SIZE, right - hpW - 5.0F - cx);
      Render2DUtil.msdfText(ctx, titleFont, name, cx, HudStyle.textY(topY - 1.0F, HudStyle.PILL_H, NAME_SIZE), NAME_SIZE, HudStyle.fade(HudStyle.TEXT, alpha));
      float infoY = y + 19;
      float itemsW = this.drawEquipment(ctx, t, right, infoY - 2.0F, alpha);
      String info = this.distanceInfo(t);
      String shownInfo = HudStyle.truncate(bodyFont, info, INFO_SIZE, right - itemsW - 4.0F - cx);
      Render2DUtil.msdfText(ctx, bodyFont, shownInfo, cx, infoY, INFO_SIZE, HudStyle.fade(HudStyle.TEXT_DIM, alpha));
      float barY = y + H - 7 - BAR_H;
      float bw = right - cx;
      Render2DUtil.roundedRect(ctx, cx, barY, bw, BAR_H, BAR_H / 2.0F, HudStyle.fade(HudStyle.TRACK, alpha));
      if (trailFrac > frac + 0.002F) {
         Render2DUtil.roundedRect(ctx, cx, barY, bw * trailFrac, BAR_H, BAR_H / 2.0F, HudStyle.fade(0x66FFFFFF, alpha));
      }

      float fw = bw * frac;
      if (fw >= BAR_H) {
         Accent.glow(ctx, cx, barY, fw, BAR_H, BAR_H / 2.0F, alpha, 4);
         Accent.fill(ctx, cx, barY, fw, BAR_H, BAR_H / 2.0F, alpha);
      }

      if (absorbFrac > 0.001F) {
         float aw = Math.max(BAR_H, bw * absorbFrac);
         Render2DUtil.roundedRect(ctx, cx + bw - aw, barY, aw, BAR_H, BAR_H / 2.0F, HudStyle.fade(ABSORB_COLOR, alpha));
      }
   }

   private void drawRingLayout(GuiGraphicsExtractor ctx, LivingEntity t, int x, int y, int headX, float frac, float absorbFrac, float alpha) {
      MsdfFont titleFont = HudStyle.title();
      MsdfFont bodyFont = HudStyle.body();
      float cx = headX + HEAD + 7;
      float ringX = x + W - PAD - RING + 1.0F;
      float ringY = y + (H - RING) / 2.0F;
      Render2DUtil.arc(ctx, ringX, ringY, RING, RING, RING_RADIUS_N, RING_THICK_N, 360.0F, HudStyle.fade(HudStyle.TRACK, alpha));
      if (frac > 0.001F) {
         Render2DUtil.arcGradient(ctx, ringX, ringY, RING, RING, RING_RADIUS_N, RING_THICK_N, 360.0F * frac, Accent.color(alpha), Accent.secondary(alpha), 0.0F);
      }

      if (absorbFrac > 0.001F) {
         Render2DUtil.arc(ctx, ringX, ringY, RING, RING, ABSORB_RADIUS_N, ABSORB_THICK_N, 360.0F * absorbFrac, HudStyle.fade(ABSORB_COLOR, alpha));
      }

      String hp = String.format(Locale.US, "%.0f", this.animHp + this.animAbsorb);
      float hpW = HudStyle.mono().width(hp, HP_SIZE);
      Render2DUtil.msdfText(ctx, HudStyle.mono(), hp, ringX + (RING - hpW) / 2.0F, HudStyle.textY(ringY, RING, HP_SIZE), HP_SIZE, HudStyle.fade(HudStyle.TEXT, alpha));
      float right = ringX - 5.0F;
      String name = HudStyle.truncate(titleFont, t.getName().getString(), NAME_SIZE, right - cx);
      Render2DUtil.msdfText(ctx, titleFont, name, cx, y + 7, NAME_SIZE, HudStyle.fade(HudStyle.TEXT, alpha));
      String info = HudStyle.truncate(bodyFont, this.distanceInfo(t), INFO_SIZE, right - cx);
      Render2DUtil.msdfText(ctx, bodyFont, info, cx, y + 16.5F, INFO_SIZE, HudStyle.fade(HudStyle.TEXT_DIM, alpha));
      this.drawEquipmentLeft(ctx, t, cx, y + H - 4 - ITEM_SLOT, right, alpha);
   }

   private float drawEquipment(GuiGraphicsExtractor ctx, LivingEntity t, float right, float y, float alpha) {
      if (alpha < 0.5F) {
         return 0.0F;
      }

      float x = right;
      ItemStack off = t.getOffhandItem();
      ItemStack main = t.getMainHandItem();
      if (!off.isEmpty()) {
         x -= ITEM_SLOT;
         drawItem(ctx, off, x, y);
      }

      if (!main.isEmpty()) {
         x -= ITEM_SLOT + (off.isEmpty() ? 0 : 1);
         drawItem(ctx, main, x, y);
      }

      return right - x;
   }

   private void drawEquipmentLeft(GuiGraphicsExtractor ctx, LivingEntity t, float x, float y, float right, float alpha) {
      if (alpha < 0.5F) return;

      ItemStack[] stacks = new ItemStack[]{t.getMainHandItem(), t.getOffhandItem()};

      for (ItemStack s : stacks) {
         if (!s.isEmpty() && x + ITEM_SLOT <= right) {
            drawItem(ctx, s, x, y);
            x += ITEM_SLOT + 1;
         }
      }
   }

   private String distanceInfo(LivingEntity t) {
      if (t == mc.player) {
         return pizza.dlc.api.translation.Translations.text("Это вы");
      }

      String dist = String.format(Locale.US, "%.1f м", mc.player.distanceTo(t));
      return t instanceof Player ? dist : dist + " · " + t.getType().getDescription().getString();
   }

   private static void drawItem(GuiGraphicsExtractor ctx, ItemStack stack, float x, float y) {
      ctx.pose().pushMatrix();
      ctx.pose().translate(x, y);
      ctx.pose().scale(0.625F);
      ctx.item(stack, 0, 0);
      ctx.pose().popMatrix();
   }

   private void spawnHurtParticles(float cx, float cy) {
      if (this.particles.size() <= 140) {
         int n = 6 + this.rng.nextInt(4);

         for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.x = cx;
            p.y = cy;
            double ang = this.rng.nextDouble() * Math.PI * 2.0;
            float spd = 12.0F + this.rng.nextFloat() * 30.0F;
            p.vx = (float)Math.cos(ang) * spd;
            p.vy = (float)Math.sin(ang) * spd - 18.0F;
            p.life = 0.0F;
            p.maxLife = 0.8F + this.rng.nextFloat() * 0.8F;
            p.size = 2.5F + this.rng.nextFloat() * 3.0F;
            p.rot = this.rng.nextFloat() * 6.2832F;
            p.spin = (this.rng.nextFloat() - 0.5F) * 6.0F;
            p.color = this.rng.nextBoolean() ? ThemeManager.accent() : ThemeManager.secondary();
            this.particles.add(p);
         }
      }
   }

   private void updateAndRenderParticles(GuiGraphicsExtractor ctx, float dt, float alpha, boolean stars) {
      Iterator<Particle> it = this.particles.iterator();

      while (it.hasNext()) {
         Particle p = it.next();
         p.life += dt;
         if (p.life >= p.maxLife) {
            it.remove();
         } else {
            p.vy += PART_GRAVITY * dt;
            p.x = p.x + p.vx * dt;
            p.y = p.y + p.vy * dt;
            p.rot = p.rot + p.spin * dt;
            float fade = 1.0F - smoothstep(p.life / p.maxLife);
            int col = HudStyle.fade(p.color, alpha * fade);
            if (stars) {
               Render2DUtil.star(ctx, p.x, p.y, p.size, STAR_POINTS, STAR_M, p.rot, col);
            } else {
               dot(ctx, p.x, p.y, p.size, col);
            }
         }
      }
   }

   private static void dot(GuiGraphicsExtractor ctx, float cx, float cy, float size, int color) {
      int s = Math.max(1, Math.round(size));
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx - s * 0.5F, cy - s * 0.5F);
      Render2DUtil.roundedRect(ctx, 0, 0, s, s, s / 2.0F, color);
      ctx.pose().popMatrix();
   }

   private LivingEntity currentTarget() {
      LivingEntity target = pizza.dlc.client.modules.impl.combat.CombatTarget.current();
      return target != null ? target : CombatSync.sharedTarget(12.0);
   }

   private Identifier skinOf(LivingEntity e) {
      return e instanceof AbstractClientPlayer p ? p.getSkin().body().texturePath() : mc.player.getSkin().body().texturePath();
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float smoothstep(float t) {
      t = clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static final class DamageText {
      String text;
      float drift;
      float age;
   }

   private static final class Particle {
      float x;
      float y;
      float vx;
      float vy;
      float life;
      float maxLife;
      float size;
      float rot;
      float spin;
      int color;
   }
}
