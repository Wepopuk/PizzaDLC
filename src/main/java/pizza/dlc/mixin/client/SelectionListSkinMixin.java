package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.UiSkin;
import pizza.dlc.api.ui.skin.OptionRowsList;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin(AbstractSelectionList.class)
public class SelectionListSkinMixin {
   @Inject(method = "updateSizeAndPosition(IIII)V", at = @At("HEAD"))
   private void pizzadlc$optionRows(int width, int height, int x, int y, CallbackInfo ci) {
      if ((Object)this instanceof OptionRowsList list) list.pizzadlc$syncRows();
   }

   @Inject(method = {"updateSizeAndPosition(IIII)V", "setScrollAmount(D)V"}, at = @At("RETURN"))
   private void pizzadlc$optionHitAreas(CallbackInfo ci) {
      if ((Object)this instanceof OptionRowsList list) list.pizzadlc$positionRows();
   }
   @Redirect(
      method = "extractSelection",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0)
   )
   private void pizzadlc$skinSelection(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
      if (CleanRenderScope.isActive()) graphics.fill(x0, y0, x1, y1, color);
      else UiSkin.selection(graphics, x0, y0, x1 - x0, y1 - y0, (color & 0xFFFFFF) == 0xFFFFFF);
   }

   @Redirect(
      method = "extractSelection",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 1)
   )
   private void pizzadlc$skipSelectionInner(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
      if (CleanRenderScope.isActive()) graphics.fill(x0, y0, x1, y1, color);
   }

   @Inject(method = "updateSize(ILnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$frameList(int width, HeaderAndFooterLayout layout, CallbackInfo ci) {
      Screen screen = ((HeaderAndFooterLayoutAccessor)layout).pizzadlc$getScreen();
      ScreenFrame.Frame f = ScreenFrame.of(screen);
      if (f != null) {
         int top = f.winY() + 2 + layout.getHeaderHeight();
         int height = Math.max(20, f.bottom() - 2 - layout.getFooterHeight() - top);
         AbstractSelectionList<?> self = (AbstractSelectionList<?>)(Object)this;
         if (width >= screen.width) {
            self.updateSizeAndPosition(f.contentW(), height, f.contentX(), top);
         } else {
            self.updateSizeAndPosition(width, height, top);
         }

         ci.cancel();
      }
   }
}
