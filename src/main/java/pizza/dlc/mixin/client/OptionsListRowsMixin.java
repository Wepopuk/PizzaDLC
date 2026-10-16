package pizza.dlc.mixin.client;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.OptionRowEntry;
import pizza.dlc.api.ui.skin.OptionRowsList;

@Mixin(OptionsList.class)
public abstract class OptionsListRowsMixin implements OptionRowsList {
   @Shadow @Final private OptionsSubScreen screen;

   @Override public void pizzadlc$syncRows() {
      boolean custom = ScreenFrame.of(this.screen) != null;
      for (var entry : ((OptionsList)(Object)this).children()) {
         if (entry instanceof OptionRowEntry row) row.pizzadlc$layoutRows(custom);
      }
   }

   @Override public void pizzadlc$positionRows() {
      boolean custom = ScreenFrame.of(this.screen) != null;
      for (var entry : ((OptionsList)(Object)this).children()) {
         if (entry instanceof OptionRowEntry row) row.pizzadlc$positionRows(custom);
      }
   }

   @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$fullRowWidth(CallbackInfoReturnable<Integer> cir) {
      ScreenFrame.Frame frame = ScreenFrame.of(this.screen);
      if (frame != null) cir.setReturnValue(frame.contentW() - 16);
   }
}
