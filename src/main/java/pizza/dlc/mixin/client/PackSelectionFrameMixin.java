package pizza.dlc.mixin.client;

import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;

@Mixin(PackSelectionScreen.class)
public class PackSelectionFrameMixin {
   @Unique
   private static final int LIST_GAP = 15;
   @Shadow
   @Final
   private HeaderAndFooterLayout layout;
   @Shadow
   private TransferableSelectionList availablePackList;
   @Shadow
   private TransferableSelectionList selectedPackList;

   @Inject(method = "repositionElements", at = @At("RETURN"))
   private void pizzadlc$frameLists(CallbackInfo ci) {
      ScreenFrame.Frame f = ScreenFrame.of((Screen)(Object)this);
      if (f != null && this.availablePackList != null && this.selectedPackList != null) {
         int top = f.winY() + 2 + this.layout.getHeaderHeight();
         int height = Math.max(20, f.bottom() - 2 - this.layout.getFooterHeight() - top);
         int center = f.contentCenter();
         int w = this.availablePackList.getWidth();
         this.availablePackList.updateSizeAndPosition(w, height, center - LIST_GAP - w, top);
         this.selectedPackList.updateSizeAndPosition(this.selectedPackList.getWidth(), height, center + LIST_GAP, top);
      }
   }
}
