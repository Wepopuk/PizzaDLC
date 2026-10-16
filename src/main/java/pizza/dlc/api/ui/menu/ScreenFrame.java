package pizza.dlc.api.ui.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

public final class ScreenFrame {
   private static final ThreadLocal<Boolean> VANILLA_LAYOUT = new ThreadLocal<>();

   public static void withLayout(boolean vanilla, Runnable action) {
      Boolean previous = VANILLA_LAYOUT.get();
      VANILLA_LAYOUT.set(vanilla);
      try { action.run(); }
      finally {
         if (previous == null) VANILLA_LAYOUT.remove();
         else VANILLA_LAYOUT.set(previous);
      }
   }

   public static boolean isVanillaLayout() {
      Boolean override = VANILLA_LAYOUT.get();
      return override != null ? override : CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive();
   }
   public static final int SIDEBAR_W = 132;
   private static final int MAX_W = 620;
   private static final int MAX_H = 340;
   private static final int MIN_H = 180;
   private static final int ROW_CONTENT = 326;
   private static final int PACK_CONTENT = 440;
   private static final int KEYBINDS_CONTENT = 356;

   private ScreenFrame() {
   }

   public static Frame of(Screen s) {
      if (isVanillaLayout() || s == null || !isFramed(s)) {
         return null;
      }

      int needed = s instanceof PackSelectionScreen ? PACK_CONTENT : (s instanceof KeyBindsScreen ? KEYBINDS_CONTENT
         : s instanceof LanguageSelectScreen ? ROW_CONTENT : isSettings(s) ? 160 : ROW_CONTENT);
      return fit(s.width, s.height, isSettings(s), needed);
   }

   /** Shared geometry, independent of game state, for responsive menus and layout checks. */
   public static Frame fit(int width, int height, boolean settings, int needed) {
      int winW = Math.min(width - 24, MAX_W);
      int winH = Math.min(height - 16, MAX_H);
      int sidebarWidth = Math.max(96, Math.min(SIDEBAR_W, winW / 4));
      boolean sidebar = settings && winW - sidebarWidth - 24 >= needed;
      int contentW = sidebar ? winW - sidebarWidth - 24 : winW - 16;
      if (contentW >= needed && winH >= (settings ? 140 : MIN_H)) {
         int winX = (width - winW) / 2;
         int winY = (height - winH) / 2;
         int sideX = winX + 8;
         int contentX = sidebar ? sideX + sidebarWidth + 8 : winX + 8;
         return new Frame(winX, winY, winW, winH, sidebar, sideX, contentX, contentW);
      }

      return null;
   }

   public static boolean isFramed(Screen s) {
      return isSettings(s) || s instanceof PackSelectionScreen || s instanceof SelectWorldScreen || s instanceof JoinMultiplayerScreen;
   }

   public static boolean isSettings(Screen s) {
      return s instanceof OptionsScreen || s instanceof OptionsSubScreen || isResourcePacks(s);
   }

   public static boolean isResourcePacks(Screen s) {
      return s instanceof PackSelectionScreen && s.getTitle().getContents() instanceof TranslatableContents t && "resourcePack.title".equals(t.getKey());
   }

   public record Frame(int winX, int winY, int winW, int winH, boolean sidebar, int sideX, int contentX, int contentW) {
      public int bottom() {
         return this.winY + this.winH;
      }

      public int contentCenter() {
         return this.contentX + this.contentW / 2;
      }

      public int sidebarW() { return this.sidebar ? this.contentX - this.sideX - 8 : 0; }
   }
}
