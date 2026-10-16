package pizza.dlc.api.ui.clickgui;

import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.util.color.ColorRGBA;
import pizza.dlc.api.util.render.Render2DUtil;

public class ColorPicker {
   private static final int GAP = 7;
   private static final int BOTTOM_H = 18;
   private static final int SWATCH_W = 22;
   private boolean open;
   private String title = "";
   private boolean hasAlpha;
   private float hue;
   private float sat;
   private float val;
   private float alpha = 1.0F;
   private float x;
   private float y;
   private IntConsumer onChange;
   private Object owner;
   private boolean draggingPanel;
   private float dragDX;
   private float dragDY;
   private int drag = 0;
   private boolean editingHex;
   private String hexBuf = "";
   private boolean hexSelectAll;
   private float anim;
   private boolean closing;
   private long lastNanos;
   private static final float ANIM_SPEED = 9.0F;

   public boolean isOpen() {
      return this.open;
   }

   public Object owner() {
      return this.owner;
   }

   public void open(Object owner, int argb, boolean hasAlpha, String title, float px, float py, IntConsumer onChange) {
      this.owner = owner;
      this.hasAlpha = hasAlpha;
      this.title = title;
      this.onChange = onChange;
      ColorRGBA c = new ColorRGBA(argb);
      this.hue = c.getHue();
      this.sat = c.getSaturation();
      this.val = c.getBrightness();
      this.alpha = hasAlpha ? (argb >>> 24 & 0xFF) / 255.0F : 1.0F;
      this.x = px;
      this.y = py;
      this.clampToScreen();
      this.open = true;
      this.draggingPanel = false;
      this.drag = 0;
      this.editingHex = false;
      this.hexSelectAll = false;
      this.closing = false;
      this.anim = 0.0F;
      this.lastNanos = 0L;
   }

   public void requestClose() {
      if (!this.open || this.closing) return;

      this.closing = true;
      this.editingHex = false;
      this.hexSelectAll = false;
   }

   public void close() {
      this.open = false;
      this.closing = false;
      this.owner = null;
      this.onChange = null;
      this.draggingPanel = false;
      this.drag = 0;
      this.editingHex = false;
      this.hexSelectAll = false;
   }

   private void advanceAnim() {
      long now = System.nanoTime();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      this.lastNanos = now;
      float target = this.closing ? 0.0F : 1.0F;
      if (this.anim < target) {
         this.anim = Math.min(target, this.anim + dt * ANIM_SPEED);
      } else if (this.anim > target) {
         this.anim = Math.max(target, this.anim - dt * ANIM_SPEED);
      }

      if (this.closing && this.anim <= 0.001F) {
         this.close();
      }
   }

   private static float easeOutBack(float t) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   public int currentArgb() {
      int rgb = ColorRGBA.fromHSB(this.hue, this.sat, this.val).getRGB() & 0xFFFFFF;
      int a = this.hasAlpha ? Math.round(this.alpha * 255.0F) : 255;
      return a << 24 | rgb;
   }

   private int hueColor() {
      return ColorRGBA.fromHSB(this.hue, 1.0F, 1.0F).getRGB() | 0xFF000000;
   }

   private int panelW() {
      return 139 + (this.hasAlpha ? 15 : 0) + 8;
   }

   private int panelH() {
      return 148;
   }

   private int svX() {
      return (int)this.x + 8;
   }

   private int svY() {
      return (int)this.y + 15;
   }

   private int hueX() {
      return this.svX() + 116 + 6;
   }

   private int alphaX() {
      return this.hueX() + 9 + 6;
   }

   private int barY() {
      return this.svY();
   }

   private int bottomY() {
      return this.svY() + 100 + GAP;
   }

   private int[] hexFieldRect() {
      int fx = this.svX() + SWATCH_W + 6;
      return new int[]{fx, this.bottomY(), (int)this.x + this.panelW() - 8 - fx, BOTTOM_H};
   }

   public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
      if (!this.open) return;

      this.advanceAnim();
      if (this.open) {
         this.clampToScreen();
         int px = (int)this.x;
         int py = (int)this.y;
         int pw = this.panelW();
         int ph = this.panelH();
         float s = easeOutBack(this.anim);
         float ccx = px + pw / 2.0F;
         float ccy = py + ph / 2.0F;
         ctx.pose().pushMatrix();
         ctx.pose().translate(ccx, ccy);
         ctx.pose().scale(s);
         ctx.pose().translate(-ccx, -ccy);
         Render2DUtil.glow(ctx, px, py, pw, ph, 6, 0x60000000, 8);
         Render2DUtil.roundedRect(ctx, px, py, pw, ph, 6.0F, 0xF0121319);
         Render2DUtil.roundedOutline(ctx, px, py, pw, ph, 6.0F, 0.5F, 0x22FFFFFF);
         Render2DUtil.msdfText(ctx, this.title, px + 8, py + 5.0F, 7.5F, 0xFFE9EAF2);
         int sx = this.svX();
         int sy = this.svY();
         Render2DUtil.svSquare(ctx, sx, sy, 116, 100, this.hueColor(), 3.5F);
         Render2DUtil.roundedOutline(ctx, sx, sy, 116, 100, 3.5F, 0.5F, 0x22FFFFFF);
         this.drawRingHandle(ctx, sx + this.sat * 116.0F, sy + (1.0F - this.val) * 100.0F);
         int hx = this.hueX();
         int by = this.barY();
         Render2DUtil.hueBar(ctx, hx, by, 9, 100, 4.5F);
         Render2DUtil.roundedOutline(ctx, hx, by, 9, 100, 4.5F, 0.5F, 0x22FFFFFF);
         this.drawBarHandle(ctx, hx, by + this.hue * 100.0F);
         if (this.hasAlpha) {
            int ax = this.alphaX();
            Render2DUtil.alphaBar(ctx, ax, by, 9, 100, this.currentArgb() & 0xFFFFFF, 4.5F);
            Render2DUtil.roundedOutline(ctx, ax, by, 9, 100, 4.5F, 0.5F, 0x22FFFFFF);
            this.drawBarHandle(ctx, ax, by + (1.0F - this.alpha) * 100.0F);
         }

         int bY = this.bottomY();
         int bH = BOTTOM_H;
         Render2DUtil.roundedRect(ctx, sx, bY, SWATCH_W, bH, 3.5F, this.currentArgb());
         Render2DUtil.roundedOutline(ctx, sx, bY, SWATCH_W, bH, 3.5F, 0.5F, 0x22FFFFFF);
         int[] hf = this.hexFieldRect();
         Render2DUtil.roundedRect(ctx, hf[0], hf[1], hf[2], hf[3], 3.5F, this.editingHex ? 0xF02A2D34 : 0xE015171C);
         if (this.editingHex) {
            Render2DUtil.roundedOutline(ctx, hf[0], hf[1], hf[2], hf[3], 3.5F, 0.5F, 0x66FFFFFF);
         }

         String disp = this.editingHex ? "#" + this.hexBuf : this.hexString();
         float tx = hf[0] + 6;
         float ty = bY + (bH - 6.0F) / 2.0F;
         float tw = Render2DUtil.msdfWidth(disp, 8.0F);
         if (this.editingHex && this.hexSelectAll && tw > 0.0F) {
            Render2DUtil.roundedRect(ctx, (int)tx - 1, hf[1] + 2, (int)Math.ceil(tw) + 2, bH - 4, 2.0F, 0x803C6FE0);
         }

         Render2DUtil.msdfText(ctx, disp, tx, ty, 8.0F, 0xFFE9EAF2);
         if (this.editingHex && !this.hexSelectAll) {
            Render2DUtil.roundedRect(ctx, Math.round(tx + tw) + 1, hf[1] + 3, 1, bH - 6, 0.5F, 0xFFE9EAF2);
         }

         ctx.pose().popMatrix();
      }
   }

   private void drawRingHandle(GuiGraphicsExtractor ctx, float cx, float cy) {
      float r = 4.5F;
      Render2DUtil.roundedOutline(ctx, Math.round(cx - r) - 1, Math.round(cy - r) - 1, 11, 11, 5.5F, 1.2F, 0xB0000000);
      Render2DUtil.roundedOutline(ctx, Math.round(cx - r), Math.round(cy - r), 9, 9, 4.5F, 1.4F, -1);
   }

   private void drawBarHandle(GuiGraphicsExtractor ctx, float barX, float cy) {
      int w = 14;
      int h = 4;
      int hx = Math.round(barX + 4.5F - w / 2.0F);
      int hy = Math.round(cy - h / 2.0F);
      Render2DUtil.roundedRect(ctx, hx, hy, w, h, h / 2.0F, -1);
      Render2DUtil.roundedOutline(ctx, hx, hy, w, h, h / 2.0F, 0.5F, 0xB0000000);
   }

   private String hexString() {
      int argb = this.currentArgb();
      int rgb = argb & 0xFFFFFF;
      return this.hasAlpha ? String.format(Locale.US, "#%06X%02X", rgb, argb >>> 24 & 0xFF) : String.format(Locale.US, "#%06X", rgb);
   }

   private String hexDigits() {
      return this.hexString().substring(1);
   }

   public boolean mouseClicked(double mx, double my) {
      if (!this.open) {
         return false;
      }

      int px = (int)this.x;
      int py = (int)this.y;
      int pw = this.panelW();
      int ph = this.panelH();
      boolean inside = mx >= px && mx <= px + pw && my >= py && my <= py + ph;
      int[] hf = this.hexFieldRect();
      boolean inHex = in(mx, my, hf[0], hf[1], hf[2], hf[3]);
      if (this.editingHex && !inHex) {
         this.commitHex();
      }

      if (!inside) {
         this.requestClose();
         return false;
      }

      this.closing = false;
      if (my < py + 15) {
         this.draggingPanel = true;
         this.dragDX = (float)(mx - this.x);
         this.dragDY = (float)(my - this.y);
         return true;
      }

      if (in(mx, my, this.svX(), this.svY(), 116, 100)) {
         this.drag = 1;
         this.updateSV(mx, my);
         return true;
      }

      if (in(mx, my, this.hueX(), this.barY(), 9, 100)) {
         this.drag = 2;
         this.updateHue(my);
         return true;
      }

      if (this.hasAlpha && in(mx, my, this.alphaX(), this.barY(), 9, 100)) {
         this.drag = 3;
         this.updateAlpha(my);
         return true;
      }

      if (inHex) {
         this.editingHex = true;
         this.hexBuf = this.hexDigits();
         this.hexSelectAll = true;
         return true;
      }

      return true;
   }

   public boolean charTyped(String s) {
      if (this.editingHex && s != null && !s.isEmpty()) {
         char c = s.charAt(0);
         boolean hex = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
         if (!hex) {
            return true;
         }

         if (this.hexSelectAll) {
            this.hexBuf = "";
            this.hexSelectAll = false;
         }

         int max = this.hasAlpha ? 8 : 6;
         if (this.hexBuf.length() < max) {
            this.hexBuf = this.hexBuf + Character.toUpperCase(c);
         }

         return true;
      }

      return false;
   }

   public boolean keyPressed(int key) {
      if (!this.editingHex) {
         return false;
      }

      if (key == 259) {
         if (this.hexSelectAll) {
            this.hexBuf = "";
            this.hexSelectAll = false;
         } else if (!this.hexBuf.isEmpty()) {
            this.hexBuf = this.hexBuf.substring(0, this.hexBuf.length() - 1);
         }
      } else if (key == 257 || key == 335) {
         this.commitHex();
      } else if (key == 256) {
         this.editingHex = false;
         this.hexSelectAll = false;
      }

      return true;
   }

   private void commitHex() {
      String h = this.hexBuf.trim();

      try {
         if (h.length() == 6) {
            int rgb = (int)(Long.parseLong(h, 16) & 0xFFFFFFL);
            this.applyArgb((this.hasAlpha ? Math.round(this.alpha * 255.0F) << 24 : 0xFF000000) | rgb);
         } else if (h.length() == 8) {
            long v = Long.parseLong(h, 16);
            int rgb = (int)(v >> 8 & 0xFFFFFFL);
            int a = (int)(v & 255L);
            this.applyArgb(a << 24 | rgb);
         }
      } catch (NumberFormatException ignored) {
      }

      this.editingHex = false;
      this.hexSelectAll = false;
   }

   private void applyArgb(int argb) {
      ColorRGBA c = new ColorRGBA(argb);
      this.hue = c.getHue();
      this.sat = c.getSaturation();
      this.val = c.getBrightness();
      if (this.hasAlpha) {
         this.alpha = (argb >>> 24 & 0xFF) / 255.0F;
      }

      this.fire();
   }

   public boolean mouseDragged(double mx, double my) {
      if (!this.open) {
         return false;
      }

      if (this.draggingPanel) {
         this.x = (float)(mx - this.dragDX);
         this.y = (float)(my - this.dragDY);
         return true;
      }

      if (this.drag == 1) {
         this.updateSV(mx, my);
         return true;
      }

      if (this.drag == 2) {
         this.updateHue(my);
         return true;
      }

      if (this.drag == 3) {
         this.updateAlpha(my);
         return true;
      }

      return false;
   }

   public boolean mouseReleased() {
      boolean was = this.draggingPanel || this.drag != 0;
      this.draggingPanel = false;
      this.drag = 0;
      return was;
   }

   private void updateSV(double mx, double my) {
      this.sat = clamp01((float)(mx - this.svX()) / 116.0F);
      this.val = clamp01(1.0F - (float)(my - this.svY()) / 100.0F);
      this.fire();
   }

   private void updateHue(double my) {
      this.hue = clamp01((float)(my - this.barY()) / 100.0F);
      this.fire();
   }

   private void updateAlpha(double my) {
      this.alpha = clamp01(1.0F - (float)(my - this.barY()) / 100.0F);
      this.fire();
   }

   private void fire() {
      if (this.onChange == null) return;

      this.onChange.accept(this.currentArgb());
   }

   private void clampToScreen() {
      Minecraft mc = Minecraft.getInstance();
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      this.x = Math.max(2.0F, Math.min(this.x, (float)(sw - this.panelW() - 2)));
      this.y = Math.max(2.0F, Math.min(this.y, (float)(sh - this.panelH() - 2)));
   }

   private static boolean in(double mx, double my, int rx, int ry, int rw, int rh) {
      return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }
}
