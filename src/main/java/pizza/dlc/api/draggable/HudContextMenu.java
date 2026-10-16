package pizza.dlc.api.draggable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;

public final class HudContextMenu implements QuickImports {
   private static final int ROW_H = 13;
   private static final float RADIUS = 4.0F;
   private static final float ARROW_W = 9.0F;
   private static final Map<BooleanSetting, Float> boolAnim = new HashMap<>();
   private static Draggable owner;
   private static boolean opening;
   private static float anim;
   private static long lastNs;
   private static float frameDt;
   private static int anchorX;
   private static int anchorY;
   private static boolean wasLeftDown;
   private static ModeSetting openDropdown;
   private static ModeSetting dropOwner;
   private static float dropAnim;
   private static SliderSetting draggingSlider;
   private static final Map<String, Float> optSlide = new HashMap<>();

   private HudContextMenu() {
   }

   public static boolean isOpen() {
      return owner != null && opening;
   }

   public static void toggle(Draggable d) {
      if (owner == d && opening) {
         close();
      } else {
         open(d);
      }
   }

   public static void open(Draggable d) {
      boolean fresh = owner != d;
      owner = d;
      opening = true;
      openDropdown = null;
      draggingSlider = null;
      if (fresh) {
         anim = 0.0F;
         dropOwner = null;
         dropAnim = 0.0F;
      }

      int screenW = mc.getWindow().getGuiScaledWidth();
      int screenH = mc.getWindow().getGuiScaledHeight();
      int estH = Math.max(1, d.getContextSettings().size()) * ROW_H + 8;
      anchorX = (int)Mth.clamp(d.getX(), 0.0F, Math.max(0, screenW - 120));
      int below = (int)(d.getY() + d.getHeight() + 2.0F);
      anchorY = below + estH <= screenH ? below : (int)(d.getY() - estH - 2.0F);
      if (anchorY < 0) {
         anchorY = 0;
      }
   }

   public static void close() {
      opening = false;
      openDropdown = null;
   }

   public static boolean consumes(int mx, int my) {
      if (owner != null && opening) {
         List<Row> rows = buildRows();
         if (mx >= anchorX && mx <= anchorX + 120 && my >= anchorY && my <= panelBottom(rows)) {
            return true;
         }

         float[] dr = dropRect(rows);
         return dr != null && mx >= dr[0] && mx <= dr[0] + dr[2] && my >= dr[1] && my <= dr[1] + dr[3];
      }

      return false;
   }

   public static void render(GuiGraphicsExtractor ctx) {
      if (owner == null) return;

      if (!(mc.gui.screen() instanceof ChatScreen)) {
         owner = null;
         anim = 0.0F;
         opening = false;
      } else {
         if (owner.getContextSettings().isEmpty()) {
            close();
         }

         int scale = mc.getWindow().getGuiScale();
         int mx = (int)(mc.mouseHandler.xpos() / scale);
         int my = (int)(mc.mouseHandler.ypos() / scale);
         if (opening) {
            long handle = StreamProofRenderer.inputHandle(mc.getWindow().handle());
            boolean lDown = GLFW.glfwGetMouseButton(handle, 0) == 1;
            if (lDown && !wasLeftDown) {
               handleClick(mx, my);
            }

            if (!lDown) {
               draggingSlider = null;
            }

            wasLeftDown = lDown;
            if (draggingSlider != null && lDown) {
               dragSlider(mx);
            }
         }

         long now = System.nanoTime();
         if (lastNs == 0L) {
            lastNs = now;
         }

         frameDt = Mth.clamp((float)(now - lastNs) / 1.0E9F, 0.0F, 0.1F);
         lastNs = now;
         anim = Mth.lerp(1.0F - (float)Math.exp(-16.0F * frameDt), anim, opening ? 1.0F : 0.0F);
         if (!opening && anim < 0.02F) {
            owner = null;
            anim = 0.0F;
            dropOwner = null;
            dropAnim = 0.0F;
         } else {
            float e = Mth.clamp(anim, 0.0F, 1.0F);
            float oy = (1.0F - e) * 6.0F;
            if (openDropdown != null) {
               dropOwner = openDropdown;
            }

            float dropTarget = openDropdown != null && dropOwner == openDropdown ? 1.0F : 0.0F;
            dropAnim = expLerp(dropAnim, dropTarget, 16.0F, frameDt);
            if (dropTarget == 0.0F && dropAnim < 0.01F) {
               dropOwner = null;
            }

            List<Row> rows = buildRows();
            HudStyle.panel(ctx, anchorX, anchorY + oy, 120.0F, panelBottom(rows) - anchorY, HudStyle.RADIUS, e);

            for (Row r : rows) {
               drawHeader(ctx, r, oy, e);
            }

            renderDropdown(ctx, rows, oy, e);
         }
      }
   }

   private static void handleClick(int mx, int my) {
      List<Row> rows = buildRows();
      if (dropOwner != null && dropAnim > 0.5F) {
         Row head = findHead(rows, dropOwner);
         if (head != null) {
            float py = head.y + head.h;
            int n = dropOwner.getModes().size();
            float revealed = (n * 10 + 6) * dropAnim;
            if (mx >= anchorX && mx <= anchorX + 120 && my >= py && my <= py + revealed) {
               for (int i = 0; i < n; i++) {
                  float iy = py + 3.0F + i * 10;
                  if (my >= iy && my < iy + 10.0F) {
                     dropOwner.setCurrentMode(dropOwner.getModes().get(i));
                     openDropdown = null;
                     return;
                  }
               }

               return;
            }
         }
      }

      if (!consumes(mx, my)) {
         close();
      } else {
         for (Row r : rows) {
            if (my >= r.y && my < r.y + r.h && mx >= anchorX && mx <= anchorX + 120) {
               Setting s = r.s;
               if (s instanceof BooleanSetting b) {
                  b.setEnabled(!b.isEnabled());
                  return;
               }

               if (s instanceof ModeSetting m) {
                  openDropdown = openDropdown == m ? null : m;
                  return;
               }

               if (s instanceof SliderSetting) {
                  draggingSlider = (SliderSetting)s;
                  dragSlider(mx);
                  return;
               }

               return;
            }
         }
      }
   }

   private static void dragSlider(int mx) {
      if (draggingSlider == null) return;

      int trackX = anchorX + 4;
      int trackW = 112;
      float t = Mth.clamp((float)(mx - trackX) / trackW, 0.0F, 1.0F);
      float raw = draggingSlider.getMin() + t * (draggingSlider.getMax() - draggingSlider.getMin());
      float step = draggingSlider.getStep();
      if (step > 0.0F) {
         raw = Math.round(raw / step) * step;
      }

      draggingSlider.setCurrentValue(raw);
   }

   private static List<Row> buildRows() {
      List<Row> rows = new ArrayList<>();
      if (owner == null) {
         return rows;
      }

      int y = anchorY + 4;

      for (Setting s : owner.getContextSettings()) {
         if (s.getHideCondition() != null && s.getHideCondition().get()) continue;

         rows.add(new Row(y, ROW_H, s));
         y += ROW_H;
      }

      return rows;
   }

   private static int panelBottom(List<Row> rows) {
      int y = anchorY + 4;
      if (!rows.isEmpty()) {
         y = rows.get(rows.size() - 1).y + rows.get(rows.size() - 1).h;
      }

      return y + 4;
   }

   private static Row findHead(List<Row> rows, Setting s) {
      for (Row r : rows) {
         if (r.s != s) continue;

         return r;
      }

      return null;
   }

   private static float[] dropRect(List<Row> rows) {
      if (dropOwner != null && !(dropAnim < 0.01F)) {
         Row head = findHead(rows, dropOwner);
         if (head == null) {
            return null;
         }

         float py = head.y + head.h;
         float fullH = dropOwner.getModes().size() * 10 + 6;
         return new float[]{anchorX, py, 120.0F, fullH * dropAnim};
      }

      return null;
   }

   private static void renderDropdown(GuiGraphicsExtractor ctx, List<Row> rows, float oy, float e) {
      float[] dr = dropRect(rows);
      if (dr != null) {
         ModeSetting m = dropOwner;
         int n = m.getModes().size();
         float px = dr[0];
         float py = dr[1] + oy;
         float fullH = n * 10 + 6;
         float revealed = dr[3];
         int top = (int)py;
         int bot = (int)(py + revealed);
         if (bot > top) {
            ctx.enableScissor((int)px, top, (int)(px + 120.0F), bot);
            Render2DUtil.roundedRect(ctx, (int)px + 3, (int)py, 114, (int)fullH, 0.0F, 0.0F, 4.0F, 4.0F, fade(HudStyle.CARD, e));
            int accent = HudStyle.TEXT;
            MsdfFont font = HudStyle.body();

            for (int i = 0; i < n; i++) {
               String opt = m.getModes().get(i);
               float iy = py + 3.0F + i * 10;
               boolean sel = m.is(opt);
               String key = System.identityHashCode(m) + ":" + opt;
               float sl = expLerp(optSlide.getOrDefault(key, sel ? 1.0F : 0.0F), sel ? 1.0F : 0.0F, 16.0F, frameDt);
               optSlide.put(key, sl);
               float textX = px + 3.0F + sl * 8.0F;
               if (sl > 0.02F) {
                  drawArrow(ctx, textX - 4.0F, iy + 5.0F, (float) (-Math.PI / 2), fade(Math.round(255.0F * sl) << 24 | accent & 0xFFFFFF, e), 0.65F);
               }

               Render2DUtil.msdfText(ctx, font, opt, textX + 3.0F, iy + 1.5F, 8.0F, fade(Render2DUtil.lerpColor(HudStyle.TEXT_OFF, accent, sl), e));
            }

            ctx.disableScissor();
         }
      }
   }

   private static void drawHeader(GuiGraphicsExtractor ctx, Row r, float oy, float e) {
      MsdfFont font = HudStyle.body();
      float ty = r.y + oy + 3.5F;
      int text = fade(HudStyle.TEXT, e);
      if (r.s instanceof BooleanSetting b) {
         float on = expLerp(boolAnim.getOrDefault(b, b.isEnabled() ? 1.0F : 0.0F), b.isEnabled() ? 1.0F : 0.0F, 16.0F, frameDt);
         boolAnim.put(b, on);
         int tw = 18;
         int th = 10;
         float tx = anchorX + 120 - 6 - tw;
         float by = r.y + oy + (r.h - th) / 2.0F;
         String name = HudStyle.truncate(font, r.s.getName(), 8.0F, tx - 4.0F - (anchorX + 6));
         Render2DUtil.msdfText(ctx, font, name, anchorX + 6, ty, 8.0F, text);
         Render2DUtil.roundedRect(ctx, tx, by, (float)tw, (float)th, th / 2.0F, fade(Render2DUtil.lerpColor(HudStyle.PILL, HudStyle.TEXT, on), e));
         int knob = th - 4;
         float kx = tx + 2.0F + on * (tw - knob - 4);
         Render2DUtil.roundedRect(ctx, kx, by + 2.0F, (float)knob, (float)knob, knob / 2.0F, fade(Render2DUtil.lerpColor(0xFF8A8484, HudStyle.WINDOW, on), e));
      } else if (r.s instanceof ModeSetting m) {
         float chevX = anchorX + 120 - 6 - 3;
         drawArrow(ctx, chevX, r.y + oy + r.h / 2.0F, dropOwner == m && openDropdown == m ? (float) Math.PI : 0.0F, fade(HudStyle.TEXT_OFF, e), 0.7F);
         String v = m.get();
         float w = font.width(v, 8.0F);
         float vx = chevX - 7.0F - w;
         String name = HudStyle.truncate(font, r.s.getName(), 8.0F, vx - 4.0F - (anchorX + 6));
         Render2DUtil.msdfText(ctx, font, name, anchorX + 6, ty, 8.0F, text);
         Render2DUtil.msdfText(ctx, font, v, vx, ty, 8.0F, fade(HudStyle.TEXT_DIM, e));
      } else if (r.s instanceof SliderSetting sl) {
         String v = fmt(sl.getValue());
         float w = HudStyle.mono().width(v, 7.5F);
         Render2DUtil.msdfText(ctx, font, r.s.getName(), anchorX + 6, r.y + oy + 1.5F, 8.0F, text);
         Render2DUtil.msdfText(ctx, HudStyle.mono(), v, anchorX + 120 - 6 - w, r.y + oy + 2.0F, 7.5F, fade(HudStyle.TEXT_DIM, e));
         float trackX = anchorX + 4;
         float trackW = 112.0F;
         float trackY = r.y + oy + r.h - 3.0F;
         float frac = (sl.getValue() - sl.getMin()) / Math.max(1.0E-4F, sl.getMax() - sl.getMin());
         Render2DUtil.roundedRect(ctx, trackX, trackY, trackW, 2.0F, 1.0F, fade(HudStyle.TRACK, e));
         Render2DUtil.roundedRect(ctx, trackX, trackY, Math.max(2.0F, trackW * frac), 2.0F, 1.0F, text);
      } else {
         Render2DUtil.msdfText(ctx, font, r.s.getName(), anchorX + 6, ty, 8.0F, text);
      }
   }

   private static void drawArrow(GuiGraphicsExtractor ctx, float cx, float cy, float rot, int color, float scale) {
      int w = Math.max(1, Math.round(ARROW_W * scale));
      int h = Math.max(1, Math.round(6.0F * scale));
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().rotate(rot);
      Render2DUtil.chevron(ctx, -w / 2, -h / 2, w, h, false, 1.5F, color);
      ctx.pose().popMatrix();
   }

   private static float expLerp(float a, float b, float speed, float dt) {
      return Mth.lerp(1.0F - (float)Math.exp(-speed * dt), a, b);
   }

   private static int fade(int argb, float e) {
      int a = (int)((argb >>> 24 & 0xFF) * Mth.clamp(e, 0.0F, 1.0F));
      return a << 24 | argb & 0xFFFFFF;
   }

   private static String fmt(float v) {
      return v == Math.rint(v) ? String.valueOf((int)v) : String.format(Locale.US, "%.2f", v);
   }

   private static final class Row {
      final int y;
      final int h;
      final Setting s;

      Row(int y, int h, Setting s) {
         this.y = y;
         this.h = h;
         this.s = s;
      }
   }
}
