package pizza.dlc.mixin.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.OptionControlSkin;
import pizza.dlc.api.ui.skin.OptionControlLayout;
import pizza.dlc.api.ui.skin.OptionRowEntry;

@Mixin(OptionsList.Entry.class)
public abstract class OptionsEntryRowsMixin implements OptionRowEntry {
   @Shadow @Final private List<OptionsList.OptionInstanceWidget> children;
   @Shadow @Final private Screen screen;
   @Unique private int pizzadlc$nativeHeight;
   @Unique private int[] pizzadlc$nativeWidths;

   @Override public void pizzadlc$layoutRows(boolean custom) {
      OptionsList.Entry entry = (OptionsList.Entry)(Object)this;
      if (this.pizzadlc$nativeWidths == null) {
         this.pizzadlc$nativeHeight = entry.getHeight();
         this.pizzadlc$nativeWidths = this.children.stream().mapToInt(item -> item.widget().getWidth()).toArray();
      }
      entry.setHeight(custom ? this.children.size() * OptionControlLayout.ROW_HEIGHT : this.pizzadlc$nativeHeight);
      if (!custom) for (int i = 0; i < this.children.size(); i++) {
         this.children.get(i).widget().setWidth(this.pizzadlc$nativeWidths[i]);
      }
   }

   @Override public void pizzadlc$positionRows(boolean custom) {
      OptionsList.Entry entry = (OptionsList.Entry)(Object)this;
      for (int i = 0; i < this.children.size(); i++) {
         var widget = this.children.get(i).widget();
         if (custom) {
            var layout = OptionControlLayout.row(entry.getContentX(), entry.getContentY() + i * OptionControlLayout.ROW_HEIGHT,
               entry.getContentWidth(), OptionControlLayout.ROW_HEIGHT - 4);
            layout.place(widget);
         } else widget.setPosition(this.screen.width / 2 - 155 + i * 160, entry.getContentY());
      }
   }

   @Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$settingRow(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
      if (ScreenFrame.of(this.screen) == null) return;
      OptionsList.Entry entry = (OptionsList.Entry)(Object)this;
      for (int i = 0; i < this.children.size(); i++) {
         var widget = this.children.get(i).widget();
         OptionControlSkin.row(g, widget, entry.getContentX(), entry.getContentY() + i * OptionControlLayout.ROW_HEIGHT,
            entry.getContentWidth(), OptionControlLayout.ROW_HEIGHT - 4, mouseX, mouseY);
         widget.extractRenderState(g, mouseX, mouseY, partialTick);
      }
      ci.cancel();
   }
}
