package pizza.dlc.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pizza.dlc.api.ui.menu.MainMenuScreen;
import pizza.dlc.api.ui.menu.ClientSettingsScreen;
import pizza.dlc.api.ui.language.LanguageSelectScreen;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.api.translation.MinecraftLanguageSync;

@Mixin(Gui.class)
public class GuiScreenSwapMixin {
   @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
   private Screen pizzadlc$mainMenu(Screen screen) {
      if (screen instanceof OptionsScreen options) return new ClientSettingsScreen(options);
      if (screen == null || screen.getClass() != TitleScreen.class) return screen;
      Screen menu = new MainMenuScreen();
      MinecraftLanguageSync.applySavedLanguage();
      return TranslationManager.getInstance().needsSelection() ? new LanguageSelectScreen(menu, true) : menu;
   }
}
