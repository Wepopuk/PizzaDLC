package pizza.dlc.api.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import pizza.dlc.mixin.client.LanguageSelectScreenAccessor;
import pizza.dlc.mixin.client.OptionsSubScreenAccessor;

public final class SettingsNav {
   private static Screen lastRoot;
   private static boolean navigating;

   private SettingsNav() {
   }

   public static Category current(Screen s) {
      if (s instanceof VideoSettingsScreen) {
         return Category.VIDEO;
      }

      if (s instanceof SoundOptionsScreen) {
         return Category.SOUND;
      }

      if (s instanceof ControlsScreen || s instanceof MouseSettingsScreen) {
         return Category.CONTROLS;
      }

      if (s instanceof KeyBindsScreen) {
         return Category.KEYS;
      }

      if (s instanceof ChatOptionsScreen) {
         return Category.CHAT;
      }

      if (s instanceof SkinCustomizationScreen) {
         return Category.SKIN;
      }

      if (s instanceof LanguageSelectScreen) {
         return Category.LANGUAGE;
      }

      if (s instanceof PackSelectionScreen) {
         return Category.PACKS;
      }

      return s instanceof AccessibilityOptionsScreen ? Category.ACCESS : Category.MAIN;
   }

   public static void remember(Screen settingsScreen) {
      Screen root = parentChain(settingsScreen);
      if (root != null) {
         lastRoot = root;
      }
   }

   public static void open(Screen from, Category cat) {
      Minecraft mc = Minecraft.getInstance();
      if (current(from) != cat || from instanceof MouseSettingsScreen
         || cat == Category.MAIN && !(from instanceof OptionsScreen) && !(from instanceof ClientSettingsScreen)) {
         Screen root = rootOf(from);
         Screen target = create(cat, root, mc);
         leave(from, mc);
         mc.gui.setScreen(target);
      }
   }

   private static void leave(Screen from, Minecraft mc) {
      navigating = true;

      try {
         if (from instanceof LanguageSelectScreen lang) {
            ((LanguageSelectScreenAccessor)lang).pizzadlc$onDone();
         } else if (from instanceof OptionsSubScreen sub) {
            OptionsList list = ((OptionsSubScreenAccessor)sub).pizzadlc$getList();
            if (list != null) {
               list.applyUnsavedChanges();
            }
         }

         if (from instanceof VideoSettingsScreen) {
            mc.getWindow().changeFullscreenVideoMode();
         }

         if (from instanceof PackSelectionScreen pack) {
            pack.onClose();
         }
      } finally {
         navigating = false;
      }
   }

   private static Screen rootOf(Screen s) {
      Screen root = s instanceof PackSelectionScreen ? lastRoot : parentChain(s);
      if (root == null) {
         root = lastRoot;
      }

      if (root == null && Minecraft.getInstance().level == null) {
         root = new TitleScreen();
      }

      return root;
   }

   private static Screen parentChain(Screen s) {
      for (int i = 0; i < 10 && s != null && ScreenFrame.isSettings(s); i++) {
         if (s instanceof OptionsScreen o) {
            s = o.getLastScreen();
         } else {
            if (!(s instanceof OptionsSubScreen sub)) {
               return null;
            }

            s = ((OptionsSubScreenAccessor)sub).pizzadlc$getLastScreen();
         }
      }

      return s != null && !ScreenFrame.isSettings(s) ? s : null;
   }

   private static Screen create(Category cat, Screen root, Minecraft mc) {
      Options o = mc.options;

      return (Screen)(switch (cat) {
         case MAIN -> new OptionsScreen(root, o, mc.level != null);
         case VIDEO -> new VideoSettingsScreen(root, mc, o);
         case SOUND -> new SoundOptionsScreen(root, o);
         case CONTROLS -> new ControlsScreen(root, o);
         case KEYS -> new KeyBindsScreen(root, o);
         case CHAT -> new ChatOptionsScreen(root, o);
         case SKIN -> new SkinCustomizationScreen(root, o);
         case LANGUAGE -> new LanguageSelectScreen(root, o, mc.getLanguageManager());
         case PACKS -> {
            lastRoot = root;
            yield new PackSelectionScreen(mc.getResourcePackRepository(), repo -> {
               mc.options.updateResourcePacks(repo);
               if (!navigating) {
                  mc.gui.setScreen(root);
               }
            }, mc.getResourcePackDirectory(), Component.translatable("resourcePack.title"));
         }
         case ACCESS -> new AccessibilityOptionsScreen(root, o);
      });
   }

   public static enum Category {
      MAIN("Основное", MenuKit.Icon.texture("other")),
      VIDEO("Графика", MenuKit.Icon.texture("visuals")),
      SOUND("Звук", MenuKit.Icon.SOUND),
      CONTROLS("Управление", MenuKit.Icon.MOUSE),
      KEYS("Клавиши", MenuKit.Icon.KEY),
      CHAT("Чат", MenuKit.Icon.CHAT),
      SKIN("Скин", MenuKit.Icon.texture("player")),
      LANGUAGE("Язык", MenuKit.Icon.text("Aa")),
      PACKS("Ресурспаки", MenuKit.Icon.PACKS),
      ACCESS("Доступность", MenuKit.Icon.texture("search"));

      public final String label;
      public final MenuKit.Icon icon;

      private Category(String label, MenuKit.Icon icon) {
         this.label = label;
         this.icon = icon;
      }
   }
}
