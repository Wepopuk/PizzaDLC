package pizza.dlc.api.ui.menu;

import pizza.dlc.api.translation.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class SettingsSidebar extends AbstractWidget {
   private static final int MAX_STEP = 28;
   private final Screen screen;
   private final float[] hover = new float[SettingsNav.Category.values().length];
   private static final float PILL_STIFFNESS = 260.0F;
   private static final float PILL_DAMPING = 22.0F;
   private float pill = -1.0F;
   private float pillVel;
   private long lastNs;
   private float scroll;
   private boolean scrollInitialized;

   public SettingsSidebar(Screen screen) {
      super(0, 0, 0, 0, Component.literal(Translations.text("Настройки")));
      this.screen = screen;
      SettingsNav.remember(screen);
   }

   private ScreenFrame.Frame sync() {
      ScreenFrame.Frame f = ScreenFrame.of(this.screen);
      if (f != null && f.sidebar()) {
         this.setX(f.sideX());
         this.setY(f.winY());
         this.setSize(f.sidebarW(), f.winH());
         return f;
      }

      this.setSize(0, 0);
      return null;
   }

   private int rowsY(ScreenFrame.Frame f) {
      return f.winY() + MenuKit.HEADER_H;
   }

   private int step(ScreenFrame.Frame f) {
      int n = SettingsNav.Category.values().length;
      int avail = f.bottom() - 10 - this.rowsY(f);
      return Math.max(20, Math.min(MAX_STEP, avail / n));
   }

   @Override
   protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      ScreenFrame.Frame f = this.sync();
      if (f != null) {
         long now = System.nanoTime();
         float dt = this.lastNs == 0L ? 0.016F : Math.min(0.1F, (float)(now - this.lastNs) / 1.0E9F);
         this.lastNs = now;
         float offset = MenuAnim.contentOffsetY(this.screen);
         g.pose().pushMatrix();
         g.pose().translate(0.0F, -offset);
         MenuKit.brand(g, f.winX(), f.winY(), Translations.text("Настройки"), 1.0F);
         SettingsNav.Category current = SettingsNav.current(this.screen);
         int step = this.step(f);
         int rowH = Math.min(24, step - 3);
         int y0 = this.rowsY(f);
         int bottom = f.bottom() - 8;
         float maxScroll = Math.max(0, SettingsNav.Category.values().length * step - (bottom - y0));
         if (!this.scrollInitialized) {
            this.scroll = Math.min(maxScroll, Math.max(0, current.ordinal() * step + rowH - (bottom - y0)));
            this.scrollInitialized = true;
         }
         this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
         if (this.pill < 0.0F) {
            float from = MenuAnim.pillFrom(this.screen);
            this.pill = from >= 0.0F ? from : current.ordinal();
         }

         int steps = Math.max(1, (int)Math.ceil(dt / 0.004F));
         float h = dt / steps;
         for (int i = 0; i < steps; i++) {
            float force = (current.ordinal() - this.pill) * PILL_STIFFNESS - this.pillVel * PILL_DAMPING;
            this.pillVel += force * h;
            this.pill += this.pillVel * h;
         }
         if (Math.abs(this.pill - current.ordinal()) < 0.002F && Math.abs(this.pillVel) < 0.01F) {
            this.pill = current.ordinal();
            this.pillVel = 0.0F;
         }

         g.enableScissor(this.getX(), y0, this.getRight(), bottom);
         MenuKit.sidebarHighlight(g, this.getX(), y0 + this.pill * step - this.scroll, this.getWidth(), rowH, 1.0F);

         for (SettingsNav.Category c : SettingsNav.Category.values()) {
            int i = c.ordinal();
            int ry = Math.round(y0 + i * step - this.scroll);
            boolean hov = c != current && MenuKit.inside(mouseX, mouseY, this.getX(), ry, this.getWidth(), rowH);
            this.hover[i] = MenuKit.expLerp(this.hover[i], hov ? 1.0F : 0.0F, 13.0F, dt);
            float near = Math.max(0.0F, 1.0F - Math.abs(this.pill - i));
            MenuKit.sidebarRow(g, this.getX(), ry, this.getWidth(), rowH, c.icon, Translations.text(c.label), near, this.hover[i], 1.0F, false);
         }

         g.disableScissor();

         g.pose().popMatrix();
         MenuAnim.markSettingsFrame(this.pill);
      }
   }

   @Override
   public void onClick(MouseButtonEvent event, boolean doubleClick) {
      ScreenFrame.Frame f = this.sync();
      if (f != null) {
         int step = this.step(f);
         int rowH = Math.min(24, step - 3);
         if (event.y() < this.rowsY(f) || event.y() >= f.bottom() - 8) return;
         int rel = (int)(event.y() - this.rowsY(f) + this.scroll);
         if (rel >= 0) {
            int idx = rel / step;
            SettingsNav.Category[] all = SettingsNav.Category.values();
            if (idx < all.length && rel - idx * step < rowH) {
               AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
               SettingsNav.open(this.screen, all[idx]);
            }
         }
      }
   }

   @Override
   public void playDownSound(SoundManager soundManager) {
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      ScreenFrame.Frame f = this.sync();
      if (f == null || !MenuKit.inside(x, y, this.getX(), this.rowsY(f), this.getWidth(), f.bottom() - 8 - this.rowsY(f))) return false;
      float max = Math.max(0, SettingsNav.Category.values().length * this.step(f) - (f.bottom() - 8 - this.rowsY(f)));
      this.scroll = Math.max(0, Math.min(max, this.scroll - (float)vertical * this.step(f)));
      return true;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
