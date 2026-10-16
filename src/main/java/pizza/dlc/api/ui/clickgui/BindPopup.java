package pizza.dlc.api.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.api.util.render.Render2DUtil;

public class BindPopup {
   private static final int W = 132;
   private static final int ROW_H = 15;
   private static final float TEXT_H = 4.9F;
   private static final float ANIM_SPEED = 9.0F;
   private boolean open;
   private BooleanSetting target;
   private float x;
   private float y;
   private boolean listening;
   private float anim;
   private boolean closing;
   private long lastNanos;

   public boolean isOpen() {
      return this.open;
   }

   public boolean isListening() {
      return this.open && this.listening;
   }

   public BooleanSetting target() {
      return this.target;
   }

   public void open(BooleanSetting setting, float px, float py, int screenW, int screenH) {
      this.target = setting;
      this.open = true;
      this.listening = false;
      this.closing = false;
      this.anim = 0.0F;
      this.lastNanos = 0L;
      this.x = Math.max(2.0F, Math.min(px, (float)(screenW - W - 2)));
      this.y = Math.max(2.0F, Math.min(py, (float)(screenH - 58 - 2)));
   }

   public void requestClose() {
      if (!this.open) return;

      this.closing = true;
      this.listening = false;
   }

   public void close() {
      this.open = false;
      this.closing = false;
      this.listening = false;
      this.target = null;
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

   public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
      if (!this.open || this.target == null) return;

      this.advanceAnim();
      if (this.open && this.target != null) {
         int accent = ThemeManager.accent();
         float s = easeOutBack(this.anim);
         float cx = this.x + 66.0F;
         float cy = this.y + 29.0F;
         ctx.pose().pushMatrix();
         ctx.pose().translate(cx, cy);
         ctx.pose().scale(s);
         ctx.pose().translate(-cx, -cy);
         Render2DUtil.panelShadow(ctx, this.x, this.y, 132.0F, 58.0F, 4.0F, 1.0F);
         Render2DUtil.roundedRect(ctx, this.x, this.y, 132.0F, 58.0F, 4.0F, 0xFF0D0D0D);
         Render2DUtil.roundedRect(ctx, this.x, this.y, 132.0F, 58.0F, 4.0F, 0x14FFFFFF);
         String title = Translations.text(this.target.getName());

         while (Render2DUtil.msdfWidth(title, 7.0F) > 120.0F && title.length() > 1) {
            title = title.substring(0, title.length() - 1);
         }

         Render2DUtil.msdfText(ctx, title, this.x + 6.0F, this.y + 6.0F, 7.0F, 0xFF9AA4B2);
         float rowY = this.y + 6.0F + TEXT_H + 4.0F;
         int half = 58;
         this.drawPill(ctx, this.x + 6.0F, rowY, half, "Toggle", !this.target.isHoldBind(), accent);
         this.drawPill(ctx, this.x + 6.0F + half + 4.0F, rowY, half, "Hold", this.target.isHoldBind(), accent);
         float fieldY = rowY + 15.0F + 4.0F;
         int fieldW = 120;
         Render2DUtil.roundedRect(ctx, this.x + 6.0F, fieldY, (float)fieldW, 15.0F, 3.0F, this.listening ? ThemeManager.accentAlpha(80) : 0x14FFFFFF);
         Render2DUtil.roundedOutline(ctx, Math.round(this.x + 6.0F), Math.round(fieldY), fieldW, ROW_H, 3.0F, 1.0F, this.listening ? accent : 0x24FFFFFF);
         String txt = this.listening ? Translations.text("Нажмите клавишу…") : (this.target.getKey() == -1 ? Translations.text("Не назначен") : KeyNameUtil.displayName(this.target.getKey()));
         float tw = Render2DUtil.msdfVisualWidth(txt, 7.0F);
         Render2DUtil.msdfText(
            ctx,
            txt,
            this.x + 6.0F + (fieldW - tw) / 2.0F,
            fieldY + 5.05F,
            7.0F,
            this.listening ? 0xFFE8ECF2 : (this.target.getKey() == -1 ? 0xFF9AA4B2 : 0xFFE8ECF2)
         );
         ctx.pose().popMatrix();
      }
   }

   private void drawPill(GuiGraphicsExtractor ctx, float px, float py, int w, String label, boolean active, int accent) {
      label = Translations.text(label);
      Render2DUtil.roundedRect(ctx, px, py, (float)w, 15.0F, 3.0F, active ? accent : 0x44FFFFFF);
      float tw = Render2DUtil.msdfVisualWidth(label, 7.0F);
      Render2DUtil.msdfText(ctx, label, px + (w - tw) / 2.0F, py + 5.05F, 7.0F, active ? -1 : 0xFF9AA4B2);
   }

   public boolean mouseClicked(double mx, double my, int button) {
      if (!this.open || this.closing || this.target == null) {
         return false;
      }

      if (this.listening) {
         this.target.setKey(-100 - button);
         this.listening = false;
         return true;
      }

      if (!(mx < this.x) && !(mx > this.x + 132.0F) && !(my < this.y) && !(my > this.y + 58.0F)) {
         float rowY = this.y + 6.0F + TEXT_H + 4.0F;
         int half = 58;
         if (my >= rowY && my <= rowY + 15.0F) {
            if (mx >= this.x + 6.0F && mx <= this.x + 6.0F + half) {
               this.target.setHoldBind(false);
               return true;
            }

            if (mx >= this.x + 6.0F + half + 4.0F && mx <= this.x + 6.0F + half + 4.0F + half) {
               this.target.setHoldBind(true);
               return true;
            }
         }

         float fieldY = rowY + 15.0F + 4.0F;
         if (my >= fieldY && my <= fieldY + 15.0F && mx >= this.x + 6.0F && mx <= this.x + 132.0F - 6.0F) {
            this.listening = true;
            return true;
         }

         return true;
      }

      this.requestClose();
      return false;
   }

   public boolean keyPressed(int key) {
      if (!this.open || this.closing || this.target == null) {
         return false;
      }

      if (this.listening) {
         if (key == 256) {
            this.listening = false;
         } else if (key == 259) {
            this.target.setKey(-1);
            this.listening = false;
         } else {
            this.target.setKey(key);
            this.listening = false;
         }

         return true;
      }

      if (key == 256) {
         this.requestClose();
         return true;
      }

      return false;
   }

   public boolean mouseScrolled(double vertical) {
      if (this.open && !this.closing && this.target != null && this.listening) {
         this.target.setKey(vertical > 0.0 ? -200 : -201);
         this.listening = false;
         return true;
      }

      return false;
   }
}
