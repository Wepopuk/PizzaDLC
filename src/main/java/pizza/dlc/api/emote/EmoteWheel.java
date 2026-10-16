package pizza.dlc.api.emote;

import java.util.List;
import pizza.dlc.api.translation.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;

// Radial emote picker. The OS cursor stays grabbed: camera-turn deltas are redirected into a virtual cursor (MouseMixin),
// so the world keeps rendering and moving behind the wheel.
public final class EmoteWheel {
   public static final int NONE = -1;
   public static final int CANCELLED = -2;
   private static final List<EmoteDefinition> ENTRIES = EmoteRegistry.ALL;
   private static final int PAGE_SIZE = 12;
   private static int page;
   private static double scrollRemainder;
   private static final float SENSITIVITY = 3.4F;
   private static final float MAX_OFFSET = 66.0F;
   private static final float DEADZONE = 15.0F;
   private static final float BAND = 22.0F;
   private static final float HUB_RADIUS = 28.0F;
   private static final float ICON_SIZE = 32.0F;
   private static final float SLICE_GAP_DEG = 3.0F;
   private static final float EDGE = 2.0F;
   private static final float HAIRLINE = 0.75F;
   private static final float POINTER_SWEEP_DEG = 46.0F;
   private static final float DIM_ALPHA = 0.35F;
   private static final float STAGGER = 0.022F;
   private static final float POP_TIME = 0.2F;
   private static final float NAME_FS = 8.5F;
   private static final float HINT_FS = 6.0F;
   private static final int OUTLINE = 0x1CFFFFFF;
   private static boolean open;
   private static float cursorX;
   private static float cursorY;
   private static int hoveredIndex = NONE;
   private static long openedAt;
   private static long lastFrame;
   private static float openT;
   private static float hubT;
   private static float highlightT;
   private static float highlightAngle;
   private static final float[] hover = new float[ENTRIES.size()];

   private EmoteWheel() {
   }

   public static boolean isOpen() {
      return open;
   }

   public static void open() {
      if (open) return;

      open = true;
      cursorX = 0.0F;
      cursorY = 0.0F;
      hoveredIndex = NONE;
      scrollRemainder = 0.0;
      openedAt = System.nanoTime();
   }

   // Index of the picked emote, NONE when released over the hub; the wheel is closed afterwards.
   public static int close() {
      if (!open) return CANCELLED;

      open = false;
      return hoveredIndex;
   }

   public static void cancel() {
      open = false;
   }

   public static EmoteDefinition entry(int index) {
      return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
   }

   // Keep picked indices global: changing pages must never play a different entry.
   private static int visibleCount() {
      return Math.min(PAGE_SIZE, ENTRIES.size() - page * PAGE_SIZE);
   }

   public static boolean onScroll(double vertical) {
      if (!open) return false;

      int pages = (ENTRIES.size() + PAGE_SIZE - 1) / PAGE_SIZE;
      scrollRemainder += vertical;
      int steps = (int)scrollRemainder;
      if (steps != 0) {
         scrollRemainder -= steps;
         page = Math.floorMod(page - steps % pages, pages);
         highlightT = 0.0F;
         java.util.Arrays.fill(hover, 0.0F);
         openedAt = System.nanoTime();
         updateHovered();
      }
      return true;
   }

   public static boolean onMouseTurn(double yawDelta, double pitchDelta) {
      if (!open) return false;

      cursorX += (float)yawDelta * SENSITIVITY;
      cursorY += (float)pitchDelta * SENSITIVITY;
      float length = (float)Math.sqrt(cursorX * cursorX + cursorY * cursorY);
      if (length > MAX_OFFSET) {
         float scale = MAX_OFFSET / length;
         cursorX *= scale;
         cursorY *= scale;
      }

      updateHovered();
      return true;
   }

   public static void render(GuiGraphicsExtractor ctx) {
      long now = System.nanoTime();
      float dt = lastFrame == 0L ? 0.016F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
      lastFrame = now;
      openT = HudStyle.expLerp(openT, open ? 1.0F : 0.0F, open ? 16.0F : 22.0F, dt);
      if (!open && openT < 0.004F) {
         openT = 0.0F;
         hubT = 0.0F;
         highlightT = 0.0F;
         java.util.Arrays.fill(hover, 0.0F);
         return;
      }

      if (open) updateHovered();

      int count = visibleCount();
      int offset = page * PAGE_SIZE;
      float step = (float)(Math.PI * 2.0 / count);
      boolean hovering = open && hoveredIndex >= 0;
      hubT = HudStyle.expLerp(hubT, hovering ? 1.0F : 0.0F, 18.0F, dt);
      if (hovering) {
         float target = sliceAngle(hoveredIndex - offset, count);
         if (highlightT < 0.02F) {
            highlightAngle = target;
         } else {
            highlightAngle += angleDiff(target, highlightAngle) * (1.0F - (float)Math.exp(-24.0F * dt));
         }
      }

      highlightT = HudStyle.expLerp(highlightT, hovering ? 1.0F : 0.0F, 20.0F, dt);
      for (int i = 0; i < count; i++) {
         int index = offset + i;
         hover[index] = HudStyle.expLerp(hover[index], hovering && index == hoveredIndex ? 1.0F : 0.0F, 20.0F, dt);
      }

      float a = Math.min(1.0F, openT);
      float w = ctx.guiWidth();
      float h = ctx.guiHeight();
      float cx = w / 2.0F;
      float cy = h / 2.0F;
      float radius = Math.max(64.0F, Math.min(96.0F, Math.min(w, h) * 0.2F));
      float outer = radius + BAND;
      float inner = radius - BAND;
      Render2DUtil.rect(ctx, 0, 0, (int)Math.ceil(w), (int)Math.ceil(h), HudStyle.fade(0xFF000000, DIM_ALPHA * a));
      Matrix3x2fStack pose = ctx.pose();
      pose.pushMatrix();
      float bloom = 0.72F + 0.28F * easeOutBack(a);
      pose.translate(cx, cy);
      pose.scale(bloom, bloom);
      pose.translate(-cx, -cy);
      int box = (int)Math.ceil(outer * 2.0F + 4.0F);
      float half = box / 2.0F;
      float bx = cx - half;
      float by = cy - half;
      Render2DUtil.arc(ctx, bx, by, box, box, inner / half, (outer - inner) / half, 360.0F, HudStyle.fade(HudStyle.PANEL_BG, a));
      Render2DUtil.arc(ctx, bx, by, box, box, (outer - HAIRLINE) / half, HAIRLINE / half, 360.0F, HudStyle.fade(OUTLINE, a));
      Render2DUtil.arc(ctx, bx, by, box, box, inner / half, HAIRLINE / half, 360.0F, HudStyle.fade(OUTLINE, a));

      for (int i = 0; i < count; i++) {
         float b = sliceAngle(i, count) - step / 2.0F;
         float cos = (float)Math.cos(b);
         float sin = (float)Math.sin(b);
         Render2DUtil.line(ctx, cx + cos * (inner + 5.0F), cy + sin * (inner + 5.0F), cx + cos * (outer - 5.0F), cy + sin * (outer - 5.0F), 1.0F, HudStyle.fade(HudStyle.DIVIDER, a));
      }

      if (highlightT > 0.01F) {
         float sweep = quantizeSweep((float)Math.toDegrees(step) - SLICE_GAP_DEG);
         float ha = a * highlightT;
         segment(ctx, cx, cy, bx, by, box, highlightAngle, sweep, inner / half, (outer - inner) / half, Accent.color(0.14F * ha));
         segment(ctx, cx, cy, bx, by, box, highlightAngle, sweep, (outer - EDGE) / half, EDGE / half, Accent.color(ha));
      }

      float since = (System.nanoTime() - openedAt) / 1.0E9F;
      float slot = (float)(Math.PI * 2.0 * radius / count);
      float iconBase = Math.min(ICON_SIZE, slot * 0.8F);

      for (int i = 0; i < count; i++) {
         float pop = open ? clamp01((since - i * STAGGER) / POP_TIME) : 1.0F;
         if (pop <= 0.0F) continue;

         float angle = sliceAngle(i, count);
         int index = offset + i;
         float r = radius + 2.0F * hover[index];
         float ix = cx + (float)Math.cos(angle) * r;
         float iy = cy + (float)Math.sin(angle) * r;
         float size = iconBase * (0.55F + 0.45F * easeOutBack(pop)) * (1.0F + 0.2F * hover[index]);
         float ia = a * Math.min(1.0F, pop * 1.6F) * (1.0F - 0.45F * hubT * (1.0F - hover[index]));
         Render2DUtil.roundedTexture(ctx, ENTRIES.get(index).icon(), ix - size / 2.0F, iy - size / 2.0F, size, size, 0.0F, true, HudStyle.fade(0xFFFFFFFF, ia));
      }

      drawHub(ctx, cx, cy, a);
      int pages = (ENTRIES.size() + PAGE_SIZE - 1) / PAGE_SIZE;
      if (pages > 1) {
         centered(ctx, HudStyle.body(), (page + 1) + " / " + pages + "  ·  " + Translations.text("прокрутка"), cx, cy + outer + 11.0F,
            HINT_FS, outer * 2.0F, HudStyle.fade(HudStyle.TEXT_DIM, a));
      }
      pose.popMatrix();
   }

   private static void drawHub(GuiGraphicsExtractor ctx, float cx, float cy, float a) {
      float r = HUB_RADIUS + 2.0F * hubT;
      Render2DUtil.roundedRect(ctx, cx - r, cy - r, r * 2.0F, r * 2.0F, r, HudStyle.fade(HudStyle.PANEL_BG, a));
      int box = (int)Math.ceil(r * 2.0F + 4.0F);
      float half = box / 2.0F;
      float bx = cx - half;
      float by = cy - half;
      Render2DUtil.arc(ctx, bx, by, box, box, (r - HAIRLINE) / half, HAIRLINE / half, 360.0F, HudStyle.fade(OUTLINE, a));
      float length = (float)Math.sqrt(cursorX * cursorX + cursorY * cursorY);
      float pointer = clamp01((length - 2.0F) / MAX_OFFSET);
      if (open && pointer > 0.0F) {
         float sweep = quantizeSweep(POINTER_SWEEP_DEG);
         segment(ctx, cx, cy, bx, by, box, (float)Math.atan2(cursorY, cursorX), sweep, (r - EDGE) / half, EDGE / half, Accent.color(a * pointer));
      }

      MsdfFont titleFont = HudStyle.title();
      MsdfFont hintFont = HudStyle.body();
      float maxW = r * 2.0F - 10.0F;
      Minecraft mc = Minecraft.getInstance();
      boolean playing = mc.player != null && EmoteEngine.INSTANCE.isPlaying(mc.player.getUUID());
      float idle = 1.0F - hubT;
      if (idle > 0.01F) {
         centered(ctx, titleFont, Translations.text("Эмоции"), cx, cy - 3.0F, NAME_FS, maxW, HudStyle.fade(HudStyle.TEXT, a * idle));
         centered(ctx, hintFont, Translations.text(playing ? "центр - стоп" : "веди мышью"), cx, cy + 6.0F, HINT_FS, maxW, HudStyle.fade(HudStyle.TEXT_DIM, a * idle));
      }

      EmoteDefinition shown = entry(hoveredIndex);
      if (shown != null && hubT > 0.01F) {
         centered(ctx, titleFont, Translations.text(shown.displayName()), cx, cy - 3.0F, NAME_FS, maxW, HudStyle.fade(HudStyle.TEXT, a * hubT));
         centered(ctx, hintFont, Translations.text("отпусти"), cx, cy + 6.0F, HINT_FS, maxW, HudStyle.fade(HudStyle.TEXT_DIM, a * hubT));
      }
   }

   // The arc shader starts at 12 o'clock and sweeps counter-clockwise, so rotate it to cover [center - sweep/2, center + sweep/2].
   private static void segment(GuiGraphicsExtractor ctx, float cx, float cy, float bx, float by, int box, float center, float sweepDeg, float rN, float tN, int argb) {
      Matrix3x2fStack pose = ctx.pose();
      pose.pushMatrix();
      pose.translate(cx, cy);
      pose.rotate(center + (float)Math.toRadians(sweepDeg) / 2.0F + (float)(Math.PI / 2.0));
      pose.translate(-cx, -cy);
      Render2DUtil.arc(ctx, bx, by, box, box, rN, tN, sweepDeg, argb);
      pose.popMatrix();
   }

   private static void centered(GuiGraphicsExtractor ctx, MsdfFont font, String text, float cx, float cy, float size, float maxW, int argb) {
      float width = font.width(text, size);
      if (width > maxW && width > 0.0F) {
         size *= maxW / width;
         width = maxW;
      }

      Render2DUtil.msdfText(ctx, font, text, cx - width / 2.0F, cy - 0.35F * size, size, argb);
   }

   private static void updateHovered() {
      int count = visibleCount();
      hoveredIndex = NONE;
      if (cursorX * cursorX + cursorY * cursorY <= DEADZONE * DEADZONE) return;

      float angle = (float)Math.atan2(cursorY, cursorX);
      float best = Float.MAX_VALUE;
      for (int i = 0; i < count; i++) {
         float diff = Math.abs(angleDiff(angle, sliceAngle(i, count)));
         if (diff < best) {
            best = diff;
            hoveredIndex = page * PAGE_SIZE + i;
         }
      }
   }

   private static float quantizeSweep(float deg) {
      float unit = 360.0F / 255.0F;
      return Math.round(deg / unit) * unit;
   }

   private static float sliceAngle(int index, int count) {
      return (float)(-Math.PI / 2.0 + index * (Math.PI * 2.0 / count));
   }

   private static float angleDiff(float a, float b) {
      float diff = (a - b) % (float)(Math.PI * 2.0);
      if (diff > Math.PI) {
         diff -= (float)(Math.PI * 2.0);
      } else if (diff < -Math.PI) {
         diff += (float)(Math.PI * 2.0);
      }

      return diff;
   }

   private static float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float t = x - 1.0F;
      return 1.0F + c3 * t * t * t + c1 * t * t;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
