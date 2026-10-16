package pizza.dlc.mixin.client;

import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;

@Mixin(HeaderAndFooterLayout.class)
public abstract class HeaderFooterLayoutFrameMixin {
   @Unique
   private static final int FRAME_PAD = 2;
   @Unique private final Map<AbstractWidget, Integer> pizzadlc$footerWidths = new WeakHashMap<>();
   @Shadow
   @Final
   private FrameLayout headerFrame;
   @Shadow
   @Final
   private FrameLayout footerFrame;
   @Shadow
   @Final
   private FrameLayout contentsFrame;
   @Shadow
   @Final
   private Screen screen;

   @Shadow
   public abstract int getHeaderHeight();

   @Shadow
   public abstract int getFooterHeight();

   @Inject(method = "arrangeElements", at = @At("HEAD"))
   private void pizzadlc$resetFooterOrigin(CallbackInfo ci) {
      this.footerFrame.setX(0);
      ArrayList<AbstractWidget> widgets = new ArrayList<>();
      this.footerFrame.visitWidgets(widgets::add);
      for (AbstractWidget widget : widgets) widget.setWidth(this.pizzadlc$footerWidths.computeIfAbsent(widget, AbstractWidget::getWidth));
      ScreenFrame.Frame f = ScreenFrame.of(this.screen);
      if (f != null && !widgets.isEmpty()) {
         int total = widgets.stream().mapToInt(AbstractWidget::getWidth).sum();
         int available = f.contentW() - 16 - 12 * (widgets.size() - 1);
         if (total > available) for (AbstractWidget widget : widgets) {
            widget.setWidth(Math.max(24, widget.getWidth() * available / total));
         }
      }
   }

   @Inject(method = "arrangeElements", at = @At("RETURN"))
   private void pizzadlc$frame(CallbackInfo ci) {
      ScreenFrame.Frame f = ScreenFrame.of(this.screen);
      if (f != null) {
         int hh = this.getHeaderHeight();
         int fh = this.getFooterHeight();
         int top = f.winY() + FRAME_PAD;
         int bottom = f.bottom() - FRAME_PAD;
         this.headerFrame.setMinWidth(f.contentW());
         this.headerFrame.setMinHeight(hh);
         this.headerFrame.setPosition(f.contentX(), top);
         this.headerFrame.arrangeElements();
         this.footerFrame.setMinWidth(f.contentW());
         this.footerFrame.setMinHeight(fh);
         this.footerFrame.arrangeElements();
         this.footerFrame.setPosition(f.contentX(), bottom - fh);
         this.contentsFrame.setMinWidth(f.contentW());
         this.contentsFrame.arrangeElements();
         int contentTop = top + hh;
         int free = bottom - fh - contentTop - this.contentsFrame.getHeight();
         this.contentsFrame.setPosition(f.contentX(), contentTop + Math.max(0, Math.min(free / 2, 12)));
      }
   }
}
