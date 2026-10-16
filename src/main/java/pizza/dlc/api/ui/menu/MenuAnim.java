package pizza.dlc.api.ui.menu;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import pizza.dlc.api.util.render.Render2DUtil;

public final class MenuAnim {
   private static final long OPEN_MS = 280L;
   private static final long SWITCH_MS = 240L;
   private static final long SWITCH_WINDOW_MS = 400L;
   private static final float OPEN_SCALE = 0.95F;
   private static final float SWITCH_SLIDE = 10.0F;
   private static final Map<Screen, State> STATES = new WeakHashMap<>();
   private static long lastSettingsFrame;
   private static float lastPillIdx = -1.0F;

   private MenuAnim() {
   }

   public static void begin(Screen s) {
      if (STATES.containsKey(s)) return;

      long now = System.currentTimeMillis();
      boolean switching = ScreenFrame.isSettings(s) && now - lastSettingsFrame < SWITCH_WINDOW_MS;
      STATES.put(s, new State(now, switching, switching ? lastPillIdx : -1.0F));
   }

   public static void markSettingsFrame(float pillIdx) {
      lastSettingsFrame = System.currentTimeMillis();
      lastPillIdx = pillIdx;
   }

   public static float pillFrom(Screen s) {
      State st = STATES.get(s);
      return st != null ? st.pillFrom : -1.0F;
   }

   private static float progress(State st) {
      long dur = st.switching ? SWITCH_MS : OPEN_MS;
      return Math.min(1.0F, (float)(System.currentTimeMillis() - st.born) / dur);
   }

   public static float easeOutCubic(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   public static float easeOutBack(float t) {
      float c1 = 1.4F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   public static float contentOffsetY(Screen s) {
      State st = STATES.get(s);
      if (st != null && st.switching) {
         float t = progress(st);
         return t >= 1.0F ? 0.0F : (1.0F - easeOutCubic(t)) * SWITCH_SLIDE;
      }

      return 0.0F;
   }

   public static float windowScale(Screen s) {
      State st = STATES.get(s);
      if (st != null && !st.switching) {
         float t = progress(st);
         return t >= 1.0F ? 1.0F : OPEN_SCALE + (1.0F - OPEN_SCALE) * easeOutBack(t);
      }

      return 1.0F;
   }

   public static boolean pushContent(GuiGraphicsExtractor g, Screen s, ScreenFrame.Frame f) {
      State st = STATES.get(s);
      if (st != null && f != null && progress(st) < 1.0F) {
         g.pose().pushMatrix();
         if (st.switching) {
            g.pose().translate(0.0F, contentOffsetY(s));
         } else {
            scaleAround(g, f, windowScale(s));
         }

         return true;
      }

      return false;
   }

   public static void scaleAround(GuiGraphicsExtractor g, ScreenFrame.Frame f, float scale) {
      float cx = f.winX() + f.winW() / 2.0F;
      float cy = f.winY() + f.winH() / 2.0F;
      g.pose().translate(cx, cy);
      g.pose().scale(scale, scale);
      g.pose().translate(-cx, -cy);
   }

   public static void overlay(GuiGraphicsExtractor g, Screen s, ScreenFrame.Frame f) {
      State st = STATES.get(s);
      if (st != null && f != null) {
         float t = progress(st);
         if (t < 1.0F) {
            float alpha = 1.0F - easeOutCubic(t);
            g.nextStratum();
            if (st.switching) {
               int x = f.contentX() - 2;
               int y = f.winY() + 4;
               int right = f.winX() + f.winW() - 6;
               int bottom = f.bottom() - 4;
               Render2DUtil.roundedRect(g, x, y, right - x, bottom - y, 8.0F, MenuKit.fade(MenuKit.WINDOW, alpha * 0.92F));
            } else {
               g.pose().pushMatrix();
               scaleAround(g, f, windowScale(s));
               Render2DUtil.roundedRect(g, f.winX(), f.winY(), f.winW(), f.winH(), MenuKit.WIN_RADIUS, MenuKit.fade(MenuKit.WINDOW, alpha));
               g.pose().popMatrix();
            }
         }
      }
   }

   private record State(long born, boolean switching, float pillFrom) {
   }
}
