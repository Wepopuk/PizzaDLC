package pizza.dlc.api.util.render.streamproof;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.impl.misc.StreamProof;
import pizza.dlc.mixin.client.MouseStreamProofAccessor;
import pizza.dlc.api.ui.menu.MainMenuScreen;
import pizza.dlc.api.ui.menu.ScreenFrame;

public final class StreamProofRenderer {
   private static final Logger LOGGER = LoggerFactory.getLogger(StreamProofRenderer.class);
   private static CleanFeedWindow window;
   private static OneWindowOutput oneWindow;
   private static long nextFrame;
   private static boolean mainHidden;
   private static boolean feedInput;
   private static boolean syncingSize;
   private static volatile boolean vanillaGameplay;
   private static final ThreadLocal<Boolean> FORWARDED_INPUT = ThreadLocal.withInitial(() -> false);

   private StreamProofRenderer() { }

   public static void poll(Minecraft minecraft) {
      if (oneWindow != null) oneWindow.sync();
      if (window == null) return;
      window.syncTitle();
      if (window.shouldClose()) {
         if (mainHidden) {
            shutdown();
            minecraft.stop();
         } else {
            StreamProof module = StreamProof.get();
            if (module != null) module.setEnabled(false);
            else close();
         }
      }
   }

   public static boolean keepMainWindowAlive(Window main) {
      StreamProof module = StreamProof.get();
      if (window == null || window.shouldClose() || module == null || !module.isEnabled() || module.oneWindow.isEnabled()) return false;
      GLFW.glfwSetWindowShouldClose(main.handle(), false);
      if (!mainHidden) {
         Minecraft minecraft = Minecraft.getInstance();
         if (main.isFullscreen()) {
            main.setWindowed(main.getScreenWidth(), main.getScreenHeight());
            main.updateFullscreenIfChanged();
            minecraft.options.fullscreen().set(false);
         }
         mainHidden = true;
         setVanillaGameplay(true);
         if (minecraft.gui.screen() != null && !(minecraft.gui.screen() instanceof MainMenuScreen)
            && minecraft.gui.screen().getClass().getName().startsWith("pizza.dlc.")) {
            minecraft.setScreenAndShow(null);
         }
         GLFW.glfwHideWindow(main.handle());
         window.focus();
      }
      return true;
   }

   public static boolean isMainHidden() { return mainHidden; }

   public static boolean isOneWindowActive() { return oneWindow != null; }

   public static void windowSizeChanged(long source, int width, int height) {
      if (window == null || syncingSize || width <= 0 || height <= 0 || window.shouldClose()) return;
      Minecraft minecraft = Minecraft.getInstance();
      Window main = minecraft.getWindow();
      long feed = window.handle();
      if (source != main.handle() && source != feed) return;
      if (main.isFullscreen() || GLFW.glfwGetWindowMonitor(feed) != 0L) return;
      try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
         var currentWidth = stack.mallocInt(1);
         var currentHeight = stack.mallocInt(1);
         GLFW.glfwGetWindowSize(source, currentWidth, currentHeight);
         if (currentWidth.get(0) != width || currentHeight.get(0) != height) return;
         long target = source == feed ? main.handle() : feed;
         GLFW.glfwGetWindowSize(target, currentWidth, currentHeight);
         if (currentWidth.get(0) == width && currentHeight.get(0) == height) return;
         syncingSize = true;
         try {
            window.resetPointerAfterResize();
            GLFW.glfwSetWindowSize(target, width, height);
            nextFrame = 0L;
         } finally {
            syncingSize = false;
         }
      }
   }

   public static boolean handleWindowCommand(String content) {
      if (!".furry".equalsIgnoreCase(content.strip())) return false;
      if (mainHidden) Minecraft.getInstance().schedule(StreamProofRenderer::restoreMainWindow);
      return true;
   }

   public static boolean restoreMainWindow() {
      Minecraft minecraft = Minecraft.getInstance();
      if (!mainHidden || window == null || window.shouldClose() || !minecraft.isRunning()) return false;
      Window main = minecraft.getWindow();
      mainHidden = false;
      GLFW.glfwSetWindowShouldClose(main.handle(), false);
      changeInputWindow(false);
      setVanillaGameplay(false);
      GLFW.glfwShowWindow(main.handle());
      GLFW.glfwRestoreWindow(main.handle());
      GLFW.glfwFocusWindow(main.handle());
      if (minecraft.level != null && minecraft.gui.screen() == null && minecraft.isWindowActive()) minecraft.mouseHandler.grabMouse();
      return true;
   }

   public static boolean isVanillaGameplayActive() { return vanillaGameplay; }

   private static void setVanillaGameplay(boolean active) {
      if (vanillaGameplay == active) return;
      vanillaGameplay = active;
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      if (client == null || client.getModuleManager() == null) return;
      if (active) {
         try {
            SwapSystem.finishPendingRestores();
         } catch (Exception error) {
            LOGGER.error("StreamProof could not finish an item restoration", error);
         }
         if (RotationController.INSTANCE != null) RotationController.INSTANCE.clear();
         RotationController.attackReady = false;
      }
      for (Module module : client.getModuleManager().getModules()) {
         if (module instanceof StreamProof) continue;
         try {
            module.setStreamProofSuspended(active);
         } catch (Exception error) {
            LOGGER.error("StreamProof could not {} {}", active ? "suspend" : "resume", module.getName(), error);
         }
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen menu) menu.setFocused(null);
      if (active && minecraft.gui.screen() != null && !(minecraft.gui.screen() instanceof MainMenuScreen)
         && minecraft.gui.screen().getClass().getName().startsWith("pizza.dlc.")) {
         minecraft.setScreenAndShow(null);
      }
      var screen = minecraft.gui.screen();
      if (screen != null && ScreenFrame.isFramed(screen)) screen.resize(screen.width, screen.height);
   }

   public static boolean isFeedFocused() { return window != null && window.isFocused(); }

   public static boolean hasVisibleFeed() { return window != null && !window.shouldClose() && !window.isIconified(); }

   public static boolean acceptsInput(long handle) { return window != null && window.handle() == handle && feedInput; }

   public static boolean ignoreMainInput(long handle) {
      return (feedInput || mainHidden) && !FORWARDED_INPUT.get() && handle == Minecraft.getInstance().getWindow().handle();
   }

   static void forwardInput(CleanFeedWindow source, Runnable callback) {
      Minecraft.getInstance().execute(() -> {
         if (window != source || !feedInput) return;
         boolean previous = FORWARDED_INPUT.get();
         FORWARDED_INPUT.set(true);
         try { callback.run(); }
         finally {
            if (previous) FORWARDED_INPUT.set(true);
            else FORWARDED_INPUT.remove();
         }
      });
   }

   public static long inputHandle(long original) {
      return window != null && feedInput ? window.handle() : original;
   }

   public static void feedFocusChanged(boolean focused) {
      if (window == null) return;
      if (focused) changeInputWindow(true);
      else if (feedInput) changeInputWindow(false);
   }

   public static void mainFocusChanged(boolean focused) {
      if (window == null) return;
      if (focused) {
         changeInputWindow(false);
         if (!mainHidden) setVanillaGameplay(false);
         Minecraft minecraft = Minecraft.getInstance();
         if (!mainHidden && minecraft.level != null && minecraft.gui.screen() == null) minecraft.mouseHandler.grabMouse();
      }
      else if (!feedInput) resetInput();
   }

   private static void changeInputWindow(boolean feed) {
      if (feed == feedInput) return;
      Minecraft minecraft = Minecraft.getInstance();
      resetInput();
      feedInput = feed;
      if (feed) setVanillaGameplay(true);
      long handle = inputHandle(minecraft.getWindow().handle());
      if (GLFW.glfwRawMouseMotionSupported()) {
         GLFW.glfwSetInputMode(handle, GLFW.GLFW_RAW_MOUSE_MOTION, minecraft.options.rawMouseInput().get() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
      }
      if (minecraft.isWindowActive() && minecraft.level != null && minecraft.gui.screen() == null) minecraft.mouseHandler.grabMouse();
   }

   private static void resetInput() {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.mouseHandler.releaseMouse();
      KeyMapping.releaseAll();
      MouseStreamProofAccessor mouse = (MouseStreamProofAccessor)minecraft.mouseHandler;
      mouse.pizzadlc$leftPressed(false);
      mouse.pizzadlc$middlePressed(false);
      mouse.pizzadlc$rightPressed(false);
      mouse.pizzadlc$activeButton(null);
      mouse.pizzadlc$deltaX(0);
      mouse.pizzadlc$deltaY(0);
      minecraft.mouseHandler.setIgnoreFirstMove();
   }

   public static boolean setFeedMouseMode(int mode, double x, double y) {
      if (window == null || !feedInput) return false;
      window.setMouseMode(mode, x, y);
      return true;
   }

   public static boolean toggleFeedFullscreen() {
      if (window == null || !feedInput) return false;
      window.toggleFullscreen();
      return true;
   }

   public static void render(Minecraft minecraft, DeltaTracker delta, boolean renderLevel) {
      if (!minecraft.isRunning()) return;
      StreamProof module = StreamProof.get();
      if (module == null || !module.isEnabled()) {
         close();
         return;
      }
      try {
         boolean single = module.oneWindow.isEnabled();
         if (single && window != null || !single && oneWindow != null) dispose(true);
         if (single && oneWindow == null) oneWindow = new OneWindowOutput(minecraft);
         if (!single && window == null) window = new CleanFeedWindow(minecraft);
         poll(minecraft);
         if (window == null && oneWindow == null || !minecraft.isRunning()) return;
         long now = System.nanoTime();
         if (now < nextFrame || !(single ? oneWindow.canPresent() : window.canPresent())) return;
         nextFrame = now + (long)(1_000_000_000.0 / module.captureFps.getValue());
         GameRenderer renderer = minecraft.gameRenderer;
         if (minecraft.gui.overlay() != null || !renderLevel) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(renderer.mainRenderTarget().getColorTexture(), new Vector4f(0, 0, 0, 1));
            outputClean(renderer);
            return;
         }
         Angle current = pizza.dlc.api.util.combat.CombatRuntime.cleanCameraRotation(module.auraCamera.isEnabled());
         Angle previous = current != null ? current.copy() : null;
         HitResult originalHit = minecraft.hitResult;
         Entity originalCrosshairEntity = minecraft.crosshairPickEntity;
         try (CleanRenderScope ignored = new CleanRenderScope(previous, current)) {
            renderer.update(delta);
            minecraft.hitResult = minecraft.player == null ? null : minecraft.player.raycastHitResult(delta.getGameTimeDeltaPartialTick(false), minecraft.getCameraEntity());
            minecraft.crosshairPickEntity = minecraft.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
            renderer.extract(delta, renderLevel);
            renderer.render(delta, renderLevel);
            outputClean(renderer);
         } finally {
            minecraft.hitResult = originalHit;
            minecraft.crosshairPickEntity = originalCrosshairEntity;
         }
      } catch (Exception error) {
         LOGGER.error("StreamProof clean feed failed", error);
         module.setEnabled(false);
         NotificationManager.add("StreamProof", "Вывод отключён: " + error.getMessage() + ". См. latest.log", NotificationType.INFO, 5000L);
      }
   }

   private static void outputClean(GameRenderer renderer) throws Exception {
      if (oneWindow != null) oneWindow.rememberClean(renderer.mainRenderTarget());
      else if (window != null) window.present(renderer.mainRenderTarget().getColorTextureView());
   }

   public static void finishFrame(Minecraft minecraft) {
      if (oneWindow == null) return;
      try {
         oneWindow.presentClient(minecraft.gameRenderer.mainRenderTarget());
      } catch (Exception error) {
         LOGGER.error("StreamProof one-window output failed", error);
         StreamProof module = StreamProof.get();
         if (module != null) module.setEnabled(false);
         else close();
         NotificationManager.add("StreamProof", "Одно окно отключено: " + error.getMessage(), NotificationType.INFO, 5000L);
      }
   }

   public static void close() {
      dispose(true);
   }

   public static void shutdown() {
      dispose(false);
   }

   private static void dispose(boolean restoreMain) {
      if (oneWindow != null) {
         OneWindowOutput closing = oneWindow;
         oneWindow = null;
         closing.close();
      }
      if (window != null) {
         resetInput();
         feedInput = false;
         CleanFeedWindow closing = window;
         window = null;
         closing.close();
         if (mainHidden && restoreMain) {
            GLFW.glfwShowWindow(Minecraft.getInstance().getWindow().handle());
            GLFW.glfwFocusWindow(Minecraft.getInstance().getWindow().handle());
         }
      }
      mainHidden = false;
      nextFrame = 0L;
      if (restoreMain && Minecraft.getInstance().isRunning()) setVanillaGameplay(false);
   }
}
