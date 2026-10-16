package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

public final class PotionsHud implements QuickImports {
   private static final String HEADER_TEXT = "Effects";
   private static final int ICON = 9;
   private static final int ICON_SLOT = 13;
   private static final float TIME_GAP = 10.0F;
   private static final int BLINK_TICKS = 150;
   private static final long ROW_MS = 300L;
   private static final long PANEL_MS = 260L;
   private static final Easing ROW_EASE = Easing.EASE_OUT_CUBIC;
   private static final Easing PANEL_EASE = Easing.EASE_OUT_CUBIC;
   private final Map<Holder<MobEffect>, Fx> entries = new LinkedHashMap<>();
   private final Draggable drag = new Draggable(HEADER_TEXT, 0.0F, 0.0F, 0.0F, 0.0F);
   private final Animation animW = new Animation(PANEL_EASE, PANEL_MS);
   private final Animation panelVis = new Animation(PANEL_EASE, PANEL_MS);
   private boolean defaultAnchored;
   private String phFxPath;
   private String phFxName;
   private static final String[][] PREVIEW_FX = new String[][]{
      {"speed", "Speed"},
      {"strength", "Strength"},
      {"regeneration", "Regeneration"},
      {"jump_boost", "Jump Boost"},
      {"fire_resistance", "Resistance"},
      {"night_vision", "Night Vision"}
   };

   public void render(GuiGraphicsExtractor ctx, boolean editMode, boolean icons) {
      if (mc.player == null || mc.level == null) return;

      Map<Holder<MobEffect>, MobEffectInstance> active = mc.player.getActiveEffectsMap();

      for (Holder<MobEffect> h : active.keySet()) {
         this.entries.computeIfAbsent(h, k -> new Fx());
      }

      MsdfFont font = HudStyle.body();
      int iconW = icons ? ICON_SLOT : 0;
      float maxRowW = 0.0F;
      float contentH = 0.0F;
      int activeCount = 0;
      Iterator<Entry<Holder<MobEffect>, Fx>> it = this.entries.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Holder<MobEffect>, Fx> en = it.next();
         Fx fx = en.getValue();
         MobEffectInstance inst = active.get(en.getKey());
         boolean act = inst != null;
         if (act) {
            activeCount++;
            fx.name = ((MobEffect)en.getKey().value()).getDisplayName().getString() + level(inst.getAmplifier());
            fx.time = duration(inst);
            fx.harmful = ((MobEffect)en.getKey().value()).getCategory() == MobEffectCategory.HARMFUL;
            fx.remaining = inst.isInfiniteDuration() ? Integer.MAX_VALUE : inst.getDuration();
         }

         float v = fx.anim.run(act ? 1.0F : 0.0F);
         if (!act && v <= 0.001F) {
            it.remove();
         } else {
            contentH += v * HudStyle.ROW_H;
            float rowW = iconW + font.width(fx.name, HudStyle.ROW_FS) + TIME_GAP + HudStyle.pillWidth(fx.time);
            maxRowW = Math.max(maxRowW, rowW);
         }
      }

      boolean placeholder = this.entries.isEmpty() && editMode;
      if (placeholder && icons && this.phFxName == null) {
         this.pickPreviewFx();
      }

      String phName = placeholder && icons ? this.phFxName : "Effect";
      String phTime = "0:00";
      if (placeholder) {
         contentH = HudStyle.ROW_H;
         maxRowW = Math.max(maxRowW, iconW + font.width(phName, HudStyle.ROW_FS) + TIME_GAP + HudStyle.pillWidth(phTime));
      }

      boolean wantVisible = !this.entries.isEmpty() || editMode;
      float pa = this.panelVis.run(wantVisible ? 1.0F : 0.0F);
      if (!wantVisible && pa < 0.01F) {
         this.animW.setValue(0.0F);
      } else {
         String badge = String.valueOf(activeCount);
         int targetW = (int)Math.ceil(Math.max(HudStyle.headerWidth(HEADER_TEXT, badge), HudStyle.PAD * 2 + maxRowW));
         if (this.animW.getValue() < 1.0F) {
            this.animW.setValue(targetW);
         }

         int panelW = Math.round(this.animW.run(targetW));
         float panelHf = HudStyle.listPanelH(contentH);
         int panelH = Math.round(panelHf);
         this.drag.setWidth(panelW);
         this.drag.setHeight(panelH);
         if (!this.drag.isPositioned()) {
            this.drag.setX(ctx.guiWidth() - 4 - panelW);
            this.drag.setY(81.0F);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
            this.defaultAnchored = true;
         } else if (this.defaultAnchored && !this.drag.isDragging()) {
            this.drag.setX(ctx.guiWidth() - 4 - panelW);
            this.drag.syncRenderPosition();
         }

         this.drag.update(ctx);
         if (this.drag.isDragging()) {
            this.defaultAnchored = false;
         }

         int panelX = (int)this.drag.getRenderX();
         int panelY = (int)this.drag.getRenderY();
         HudStyle.panel(ctx, panelX, panelY, panelW, panelHf, HudStyle.RADIUS, pa);
         HudStyle.header(ctx, panelX, panelY, panelW, HEADER_TEXT, badge, pa);
         float rowY = panelY + HudStyle.HEADER_H + HudStyle.ROWS_TOP;
         ctx.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
         if (placeholder) {
            Identifier sprite = icons && this.phFxPath != null ? Identifier.fromNamespaceAndPath("minecraft", "mob_effect/" + this.phFxPath) : null;
            this.drawRow(ctx, font, sprite, iconW, phName, phTime, false, panelX, panelW, rowY, HudStyle.ROW_H, 0.0F, 0.4F * pa, 0.4F * pa);
         } else {
            List<Entry<Holder<MobEffect>, Fx>> ordered = new ArrayList<>(this.entries.entrySet());
            ordered.sort(Comparator.comparingInt(e -> e.getValue().harmful ? 1 : 0));
            float blinkPulse = 0.3F + 0.7F * (0.5F + 0.5F * (float)Math.cos(System.nanoTime() / 1.0E9 * (Math.PI * 2)));

            for (Entry<Holder<MobEffect>, Fx> enx : ordered) {
               Fx fxx = enx.getValue();
               float v = fxx.anim.getValue();
               float slotH = v * HudStyle.ROW_H;
               if (v > 0.001F) {
                  Identifier sprite = null;
                  if (icons) {
                     Identifier eid = BuiltInRegistries.MOB_EFFECT.getKey((MobEffect)enx.getKey().value());
                     sprite = eid != null ? eid.withPrefix("mob_effect/") : null;
                  }

                  float timeA = v * pa * (fxx.remaining < BLINK_TICKS ? blinkPulse : 1.0F);
                  this.drawRow(ctx, font, sprite, iconW, fxx.name, fxx.time, fxx.harmful, panelX, panelW, rowY, slotH, (1.0F - v) * HudStyle.SLIDE, v * pa, timeA);
               }

               rowY += slotH;
            }
         }

         ctx.disableScissor();
      }
   }

   private void drawRow(
      GuiGraphicsExtractor ctx,
      MsdfFont font,
      Identifier sprite,
      int iconW,
      String name,
      String time,
      boolean harmful,
      int panelX,
      int panelW,
      float rowY,
      float slotH,
      float slide,
      float a,
      float timeA
   ) {
      float x = panelX + HudStyle.PAD + slide;
      if (sprite != null) {
         drawSprite(ctx, sprite, x, rowY + (slotH - ICON) / 2.0F, HudStyle.fade(-1, a));
      }

      float pillW = HudStyle.pillWidth(time);
      float pillX = panelX + panelW - HudStyle.PAD - pillW + slide;
      float nameX = x + iconW;
      String shown = HudStyle.truncate(font, name, HudStyle.ROW_FS, pillX - 6.0F - nameX);
      int nameCol = harmful ? HudStyle.BAD : HudStyle.TEXT;
      Render2DUtil.msdfText(ctx, font, shown, nameX, HudStyle.textY(rowY, slotH, HudStyle.ROW_FS), HudStyle.ROW_FS, HudStyle.fade(nameCol, a));
      HudStyle.pill(ctx, pillX, rowY + (slotH - HudStyle.PILL_H) / 2.0F, pillW, time, HudStyle.TEXT, timeA);
   }

   private static String level(int amp) {
      int lvl = amp + 1;
      return lvl <= 1 ? "" : " " + lvl;
   }

   private static String duration(MobEffectInstance e) {
      if (e.isInfiniteDuration()) {
         return "**:**";
      }

      int t = e.getDuration() / 20;
      int s = t % 60;
      return t / 60 + ":" + (s < 10 ? "0" : "") + s;
   }

   private static void drawSprite(GuiGraphicsExtractor ctx, Identifier sprite, float x, float y, int tint) {
      ctx.pose().pushMatrix();
      ctx.pose().translate(x, y);
      ctx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, 9, 9, tint);
      ctx.pose().popMatrix();
   }

   private void pickPreviewFx() {
      String[] pick = PREVIEW_FX[(int)(Math.random() * PREVIEW_FX.length)];
      this.phFxPath = pick[0];
      this.phFxName = pick[1];
   }

   private static final class Fx {
      final Animation anim = new Animation(PotionsHud.ROW_EASE, ROW_MS);
      String name = "";
      String time = "";
      boolean harmful = false;
      int remaining = Integer.MAX_VALUE;
   }
}
