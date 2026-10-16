package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.translation.ClientLanguage;
import pizza.dlc.api.translation.TranslationManager;

/** Keep the five supported client languages in sync when changed in vanilla settings too. */
@Mixin(net.minecraft.client.gui.screens.options.LanguageSelectScreen.class)
public abstract class LanguagePreferenceMixin {
   @Inject(method = "onDone", at = @At("TAIL"))
   private void pizzadlc$rememberLanguage(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      String selected = mc.getLanguageManager().getSelected();
      TranslationManager manager = TranslationManager.getInstance();
      for (ClientLanguage language : ClientLanguage.values()) {
         if (language.minecraftCode().equals(selected) && (manager.needsSelection() || manager.language() != language)) {
            manager.setLanguage(language.code());
            mc.options.save();
            break;
         }
      }
   }
}
