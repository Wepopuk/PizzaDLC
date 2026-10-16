package pizza.dlc.api.util.render.streamproof;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.Callbacks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonInfo;
import pizza.dlc.mixin.client.KeyboardStreamProofAccessor;
import pizza.dlc.mixin.client.MouseStreamProofAccessor;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL32;

final class CleanFeedWindow implements AutoCloseable {
   private final Minecraft minecraft;
   private final boolean overlay;
   private long handle;
   private GpuSurface surface;
   private GLCapabilities capabilities;
   private int readFramebuffer;
   private final int[] width = new int[1];
   private final int[] height = new int[1];
   private boolean shown;
   private String title;
   private boolean pointerKnown;
   private double pointerX;
   private double pointerY;
   private double forwardedX;
   private double forwardedY;
   private int windowedX, windowedY, windowedWidth, windowedHeight;

   CleanFeedWindow(Minecraft minecraft) throws Exception {
      this(minecraft, false);
   }

   CleanFeedWindow(Minecraft minecraft, boolean overlay) throws Exception {
      this.minecraft = minecraft;
      this.overlay = overlay;
      Window main = minecraft.getWindow();
      this.title = GLFW.glfwGetWindowTitle(main.handle());
      boolean openGl = GLFW.glfwGetWindowAttrib(main.handle(), GLFW.GLFW_CLIENT_API) != GLFW.GLFW_NO_API;
      GLFW.glfwDefaultWindowHints();
      main.backend().setWindowHints();
      GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
      GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
      GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
      if (overlay) {
         GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
         GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);
      }
      try {
         this.handle = GLFW.glfwCreateWindow(main.getScreenWidth(), main.getScreenHeight(), this.title, 0L, openGl ? main.handle() : 0L);
      } finally {
         GLFW.glfwDefaultWindowHints();
      }
      if (this.handle == 0L) throw new IllegalStateException("Не удалось создать окно StreamProof");
      try {
         this.setupIcon();
         if (overlay) WindowsCaptureOverlay.protect(this.handle, main.handle());
         if (openGl) {
            long originalContext = GLFW.glfwGetCurrentContext();
            GLCapabilities originalCapabilities = GL.getCapabilities();
            try {
               GLFW.glfwMakeContextCurrent(this.handle);
               this.capabilities = GL.createCapabilities();
               GLFW.glfwSwapInterval(0);
               this.readFramebuffer = GL32.glGenFramebuffers();
            } finally {
               GLFW.glfwMakeContextCurrent(originalContext);
               GL.setCapabilities(originalCapabilities);
            }
         } else {
            this.surface = RenderSystem.getDevice().createSurface(this.handle);
         }
         if (!overlay) this.setupInput();
      } catch (Exception error) {
         this.close();
         throw error;
      }
   }

   long handle() { return this.handle; }

   void syncOverlay() {
      long main = this.minecraft.getWindow().handle();
      if (GLFW.glfwGetWindowAttrib(main, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
         || GLFW.glfwGetWindowAttrib(main, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE) {
         GLFW.glfwHideWindow(this.handle);
         this.shown = false;
         return;
      }
      try (MemoryStack stack = MemoryStack.stackPush()) {
         var x = stack.mallocInt(1); var y = stack.mallocInt(1);
         var w = stack.mallocInt(1); var h = stack.mallocInt(1);
         GLFW.glfwGetWindowPos(main, x, y);
         GLFW.glfwGetWindowPos(this.handle, w, h);
         if (x.get(0) != w.get(0) || y.get(0) != h.get(0)) GLFW.glfwSetWindowPos(this.handle, x.get(0), y.get(0));
         GLFW.glfwGetWindowSize(main, w, h);
         GLFW.glfwGetWindowSize(this.handle, x, y);
         if (w.get(0) > 0 && h.get(0) > 0 && (w.get(0) != x.get(0) || h.get(0) != y.get(0)))
            GLFW.glfwSetWindowSize(this.handle, w.get(0), h.get(0));
      }
   }

   private void setupIcon() throws java.io.IOException {
      IconSet set = SharedConstants.getCurrentVersion().stable() ? IconSet.RELEASE : IconSet.SNAPSHOT;
      var sources = set.getStandardIcons(this.minecraft.getVanillaPackResources());
      var pixels = new ArrayList<ByteBuffer>(sources.size());
      try (MemoryStack stack = MemoryStack.stackPush()) {
         GLFWImage.Buffer icons = GLFWImage.malloc(sources.size(), stack);
         for (int i = 0; i < sources.size(); i++) {
            try (var stream = sources.get(i).get(); NativeImage image = NativeImage.read(stream)) {
               ByteBuffer data = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
               pixels.add(data);
               data.asIntBuffer().put(image.getPixelsABGR());
               icons.position(i).width(image.getWidth()).height(image.getHeight()).pixels(data);
            }
         }
         GLFW.glfwSetWindowIcon(this.handle, icons.position(0));
      } finally {
         for (ByteBuffer data : pixels) MemoryUtil.memFree(data);
      }
   }

   boolean isFocused() {
      return this.handle != 0L && GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
   }

   boolean isIconified() {
      return GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
   }

   void syncTitle() {
      String mainTitle = GLFW.glfwGetWindowTitle(this.minecraft.getWindow().handle());
      if (!mainTitle.equals(this.title)) {
         GLFW.glfwSetWindowTitle(this.handle, mainTitle);
         this.title = mainTitle;
      }
   }

   void focus() {
      GLFW.glfwShowWindow(this.handle);
      GLFW.glfwFocusWindow(this.handle);
   }

   private void setupInput() {
      long main = this.minecraft.getWindow().handle();
      GLFW.glfwSetWindowSizeCallback(this.handle, (window, width, height) -> {
         this.resetPointerAfterResize();
         StreamProofRenderer.windowSizeChanged(window, width, height);
      });
      KeyboardStreamProofAccessor keyboard = (KeyboardStreamProofAccessor)this.minecraft.keyboardHandler;
      MouseStreamProofAccessor mouse = (MouseStreamProofAccessor)this.minecraft.mouseHandler;
      GLFW.glfwSetKeyCallback(this.handle, (window, code, scan, action, modifiers) -> {
         if (StreamProofRenderer.acceptsInput(this.handle)) StreamProofRenderer.forwardInput(this,
            () -> keyboard.pizzadlc$keyPress(main, action, new KeyEvent(code, scan, modifiers)));
      });
      GLFW.glfwSetCharCallback(this.handle, (window, codepoint) -> {
         if (StreamProofRenderer.acceptsInput(this.handle)) StreamProofRenderer.forwardInput(this,
            () -> keyboard.pizzadlc$charTyped(main, new CharacterEvent(codepoint)));
      });
      GLFW.glfwSetMouseButtonCallback(this.handle, (window, code, action, modifiers) -> {
         if (StreamProofRenderer.acceptsInput(this.handle)) StreamProofRenderer.forwardInput(this,
            () -> mouse.pizzadlc$onButton(main, new MouseButtonInfo(code, modifiers), action));
      });
      GLFW.glfwSetScrollCallback(this.handle, (window, horizontal, vertical) -> {
         if (StreamProofRenderer.acceptsInput(this.handle)) StreamProofRenderer.forwardInput(this,
            () -> mouse.pizzadlc$onScroll(main, horizontal, vertical));
      });
      GLFW.glfwSetCursorPosCallback(this.handle, (window, x, y) -> {
         if (StreamProofRenderer.acceptsInput(this.handle)) {
            double sourceX, sourceY;
            if (this.minecraft.mouseHandler.isMouseGrabbed()) {
               if (!this.pointerKnown) {
                  this.forwardedX = this.minecraft.mouseHandler.xpos();
                  this.forwardedY = this.minecraft.mouseHandler.ypos();
               } else {
                  this.forwardedX += x - this.pointerX;
                  this.forwardedY += y - this.pointerY;
               }
               sourceX = this.forwardedX;
               sourceY = this.forwardedY;
            } else {
               int[] w = new int[1], h = new int[1];
               GLFW.glfwGetWindowSize(this.handle, w, h);
               GLFW.glfwGetFramebufferSize(this.handle, this.width, this.height);
               if (w[0] <= 0 || h[0] <= 0 || this.width[0] <= 0 || this.height[0] <= 0) return;
               CleanFeedViewport viewport = this.viewport();
               sourceX = viewport.sourceX(x * this.width[0] / w[0], this.minecraft.getWindow().getScreenWidth());
               sourceY = viewport.sourceY(y * this.height[0] / h[0], this.minecraft.getWindow().getScreenHeight());
            }
            StreamProofRenderer.forwardInput(this, () -> mouse.pizzadlc$onMove(main, sourceX, sourceY));
         }
         this.pointerX = x;
         this.pointerY = y;
         this.pointerKnown = true;
      });
      GLFW.glfwSetCursorEnterCallback(this.handle, (window, entered) -> {
         this.pointerKnown = false;
         if (entered) this.minecraft.mouseHandler.cursorEntered();
      });
      GLFW.glfwSetWindowFocusCallback(this.handle, (window, focused) -> StreamProofRenderer.feedFocusChanged(focused));
   }

   private CleanFeedViewport viewport() {
      Window main = this.minecraft.getWindow();
      return this.capabilities != null ? CleanFeedViewport.fit(main.getWidth(), main.getHeight(), this.width[0], this.height[0])
         : new CleanFeedViewport(0, 0, Math.max(1, this.width[0]), Math.max(1, this.height[0]));
   }

   void resetPointerAfterResize() {
      this.pointerKnown = false;
      this.minecraft.mouseHandler.setIgnoreFirstMove();
   }

   void setMouseMode(int mode, double sourceX, double sourceY) {
      int[] w = new int[1], h = new int[1];
      GLFW.glfwGetWindowSize(this.handle, w, h);
      GLFW.glfwGetFramebufferSize(this.handle, this.width, this.height);
      CleanFeedViewport viewport = this.viewport();
      this.pointerKnown = false;
      this.forwardedX = sourceX;
      this.forwardedY = sourceY;
      if (this.width[0] > 0 && this.height[0] > 0) {
         GLFW.glfwSetCursorPos(this.handle,
            viewport.targetX(sourceX, this.minecraft.getWindow().getScreenWidth()) * w[0] / this.width[0],
            viewport.targetY(sourceY, this.minecraft.getWindow().getScreenHeight()) * h[0] / this.height[0]);
      }
      GLFW.glfwSetInputMode(this.handle, GLFW.GLFW_CURSOR, mode);
      this.pointerKnown = false;
   }

   void toggleFullscreen() {
      if (GLFW.glfwGetWindowMonitor(this.handle) != 0L) {
         GLFW.glfwSetWindowMonitor(this.handle, 0L, this.windowedX, this.windowedY, this.windowedWidth, this.windowedHeight, GLFW.GLFW_DONT_CARE);
      } else {
         int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
         GLFW.glfwGetWindowPos(this.handle, x, y);
         GLFW.glfwGetWindowSize(this.handle, w, h);
         this.windowedX = x[0]; this.windowedY = y[0]; this.windowedWidth = w[0]; this.windowedHeight = h[0];
         long monitor = GLFW.glfwGetPrimaryMonitor();
         var mode = GLFW.glfwGetVideoMode(monitor);
         if (mode != null) GLFW.glfwSetWindowMonitor(this.handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
      }
      this.pointerKnown = false;
      this.minecraft.mouseHandler.setIgnoreFirstMove();
   }

   boolean shouldClose() {
      return this.handle == 0L || GLFW.glfwWindowShouldClose(this.handle);
   }

   boolean canPresent() {
      if (this.overlay && (GLFW.glfwGetWindowAttrib(this.minecraft.getWindow().handle(), GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
         || GLFW.glfwGetWindowAttrib(this.minecraft.getWindow().handle(), GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE)) return false;
      GLFW.glfwGetFramebufferSize(this.handle, this.width, this.height);
      return this.width[0] > 0 && this.height[0] > 0
         && GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_FALSE;
   }

   void present(GpuTextureView texture) throws Exception {
      if (this.capabilities != null) {
         this.presentOpenGl(texture);
      } else {
         GpuSurface.Configuration configuration = new GpuSurface.Configuration(this.width[0], this.height[0],
            GpuSurface.PresentMode.getSupportedVsyncMode(this.surface.supportedPresentModes(), false));
         if (!this.surface.currentConfiguration().filter(configuration::equals).isPresent() || this.surface.isSuboptimal()) {
            this.surface.configure(configuration);
         }
         this.surface.acquireNextTexture();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         this.surface.blitFromTexture(encoder, texture);
         encoder.submit();
         this.surface.present();
      }
      if (!this.shown) {
         GLFW.glfwShowWindow(this.handle);
         this.shown = true;
      }
   }

   private void presentOpenGl(GpuTextureView texture) {
      long originalContext = GLFW.glfwGetCurrentContext();
      GLCapabilities originalCapabilities = GL.getCapabilities();
      long ready = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
      GL32.glFlush();
      long consumed = 0L;
      try {
         GLFW.glfwMakeContextCurrent(this.handle);
         GL.setCapabilities(this.capabilities);
         GL32.glWaitSync(ready, 0, GL32.GL_TIMEOUT_IGNORED);
         GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, this.readFramebuffer);
         GL32.glFramebufferTexture2D(GL32.GL_READ_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D,
            ((GlTexture)texture.texture()).glId(), 0);
         if (GL32.glCheckFramebufferStatus(GL32.GL_READ_FRAMEBUFFER) != GL32.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("StreamProof framebuffer is incomplete");
         }
         GL32.glReadBuffer(GL32.GL_COLOR_ATTACHMENT0);
         GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);
         GL32.glDisable(GL32.GL_SCISSOR_TEST);
         GL32.glClearColor(0, 0, 0, 1);
         GL32.glClear(GL32.GL_COLOR_BUFFER_BIT);
         int sourceWidth = texture.getWidth(0);
         int sourceHeight = texture.getHeight(0);
         CleanFeedViewport viewport = CleanFeedViewport.fit(sourceWidth, sourceHeight, this.width[0], this.height[0]);
         int bottom = this.height[0] - viewport.y() - viewport.height();
         GL32.glBlitFramebuffer(0, 0, sourceWidth, sourceHeight, viewport.x(), bottom,
            viewport.x() + viewport.width(), bottom + viewport.height(), GL32.GL_COLOR_BUFFER_BIT, GL32.GL_LINEAR);
         consumed = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
         GL32.glFlush();
         GLFW.glfwSwapBuffers(this.handle);
      } finally {
         GLFW.glfwMakeContextCurrent(originalContext);
         GL.setCapabilities(originalCapabilities);
         if (consumed != 0L) {
            GL32.glWaitSync(consumed, 0, GL32.GL_TIMEOUT_IGNORED);
            GL32.glDeleteSync(consumed);
         }
         GL32.glDeleteSync(ready);
      }
   }

   @Override
   public void close() {
      if (this.surface != null) {
         this.surface.close();
         this.surface = null;
      }
      if (this.handle != 0L) {
         if (this.capabilities != null) {
            long originalContext = GLFW.glfwGetCurrentContext();
            GLCapabilities originalCapabilities = GL.getCapabilities();
            try {
               GLFW.glfwMakeContextCurrent(this.handle);
               GL.setCapabilities(this.capabilities);
               GL32.glDeleteFramebuffers(this.readFramebuffer);
            } finally {
               GLFW.glfwMakeContextCurrent(originalContext);
               GL.setCapabilities(originalCapabilities);
            }
         }
         Callbacks.glfwFreeCallbacks(this.handle);
         GLFW.glfwDestroyWindow(this.handle);
         this.handle = 0L;
      }
   }
}
