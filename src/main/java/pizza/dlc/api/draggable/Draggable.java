package pizza.dlc.api.draggable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pizza.dlc.api.draggable.grid.Grid;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.PizzaDLCClient;

public class Draggable implements QuickImports {
   @Expose
   @SerializedName("x")
   private float x;
   @Expose
   @SerializedName("y")
   private float y;
   @Expose
   @SerializedName("name")
   private final String name;
   public float initX;
   public float initY;
   private float startX;
   private float startY;
   private boolean dragging;
   private float width;
   private float height;
   private float renderX;
   private float renderY;
   private boolean renderInitialized = false;
   private boolean positioned = false;
   private float dragAlpha = 0.0F;
   private long lastFrameNs = 0L;
   private boolean wasMouseDown = false;
   private boolean wasRightDown = false;
   private List<Setting> contextSettings = new ArrayList<>();
   public static Draggable currentDragging = null;
   public static final Grid grid = new Grid();

   public Draggable(String name, float initX, float initY, float width, float height) {
      this.name = name;
      this.x = initX;
      this.y = initY;
      this.initX = initX;
      this.initY = initY;
      this.renderX = initX;
      this.renderY = initY;
      this.width = width;
      this.height = height;
      if (PizzaDLCClient.getInstance() != null && PizzaDLCClient.getInstance().getDraggableManager() != null) {
         PizzaDLCClient.getInstance().getDraggableManager().addDraggable(this);
      }
   }

   public void resetPosition() {
      this.x = this.initX;
      this.y = this.initY;
      this.renderX = this.initX;
      this.renderY = this.initY;
   }

   public void syncRenderPosition() {
      this.renderX = this.x;
      this.renderY = this.y;
      this.renderInitialized = true;
   }

   public void update(GuiGraphicsExtractor ctx) {
      long handle = StreamProofRenderer.inputHandle(mc.getWindow().handle());
      int scale = mc.getWindow().getGuiScale();
      int mouseX = (int)(mc.mouseHandler.xpos() / scale);
      int mouseY = (int)(mc.mouseHandler.ypos() / scale);
      boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
      if (chatOpen) {
         boolean isDown = GLFW.glfwGetMouseButton(handle, 0) == 1;
         if (isDown && !this.wasMouseDown) {
            this.onClick(mouseX, mouseY, 0);
         } else if (!isDown && this.wasMouseDown) {
            this.onRelease(0);
         }

         this.wasMouseDown = isDown;
         boolean rDown = GLFW.glfwGetMouseButton(handle, 1) == 1;
         if (rDown && !this.wasRightDown && !this.contextSettings.isEmpty() && this.isHovered(mouseX, mouseY)) {
            HudContextMenu.toggle(this);
         }

         this.wasRightDown = rDown;
      } else if (this.wasMouseDown) {
         this.onRelease(0);
         this.wasMouseDown = false;
         this.wasRightDown = false;
      }

      boolean shiftHeld = GLFW.glfwGetKey(handle, 340) == 1;
      if (this.dragging) {
         if (!shiftHeld) {
            grid.render(ctx);
         }

         this.x = this.clampPos(mouseX + this.startX, this.width, mc.getWindow().getGuiScaledWidth());
         this.y = this.clampPos(mouseY + this.startY, this.height, mc.getWindow().getGuiScaledHeight());
      }

      if (!shiftHeld) {
         grid.handleDragPositions();
      }

      long nowNs = System.nanoTime();
      if (this.lastFrameNs == 0L) {
         this.lastFrameNs = nowNs;
      }

      float dt = Mth.clamp((float)(nowNs - this.lastFrameNs) / 1.0E9F, 0.0F, 0.1F);
      this.lastFrameNs = nowNs;
      if (!this.renderInitialized) {
         this.renderX = this.x;
         this.renderY = this.y;
         this.renderInitialized = true;
      }

      float posT = 1.0F - (float)Math.exp(-14.0F * dt);
      this.renderX = Mth.lerp(posT, this.renderX, this.x);
      this.renderY = Mth.lerp(posT, this.renderY, this.y);
      if (Math.abs(this.renderX - this.x) < 0.05F) {
         this.renderX = this.x;
      }

      if (Math.abs(this.renderY - this.y) < 0.05F) {
         this.renderY = this.y;
      }

      float glowT = 1.0F - (float)Math.exp(-(this.dragging ? 12.0F : 8.0F) * dt);
      this.dragAlpha = Mth.lerp(glowT, this.dragAlpha, this.dragging ? 1.0F : 0.0F);
      if (this.dragAlpha < 0.005F && !this.dragging) {
         this.dragAlpha = 0.0F;
      }

      if (this.dragAlpha > 0.995F && this.dragging) {
         this.dragAlpha = 1.0F;
      }
   }

   public void onClick(int mouseX, int mouseY, int button) {
      if (button == 0 && HudContextMenu.isOpen() && HudContextMenu.consumes(mouseX, mouseY)) return;

      if (button == 0 && this.isHovered(mouseX, mouseY) && currentDragging == null) {
         this.dragging = true;
         currentDragging = this;
         this.startX = this.x - mouseX;
         this.startY = this.y - mouseY;
         this.renderX = this.x;
         this.renderY = this.y;
      }
   }

   public void onRelease(int button) {
      if (button != 0) return;

      this.dragging = false;
      if (currentDragging == this) {
         currentDragging = null;
      }
   }

   private float clampPos(float v, float size, float screen) {
      return Math.max(0.0F, Math.min(v, screen - size));
   }

   private boolean isHovered(double mouseX, double mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public String getName() {
      return this.name;
   }

   public float getInitX() {
      return this.initX;
   }

   public float getInitY() {
      return this.initY;
   }

   public float getStartX() {
      return this.startX;
   }

   public float getStartY() {
      return this.startY;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public float getRenderX() {
      return this.renderX;
   }

   public float getRenderY() {
      return this.renderY;
   }

   public boolean isRenderInitialized() {
      return this.renderInitialized;
   }

   public boolean isPositioned() {
      return this.positioned;
   }

   public float getDragAlpha() {
      return this.dragAlpha;
   }

   public long getLastFrameNs() {
      return this.lastFrameNs;
   }

   public boolean isWasMouseDown() {
      return this.wasMouseDown;
   }

   public boolean isWasRightDown() {
      return this.wasRightDown;
   }

   public List<Setting> getContextSettings() {
      return this.contextSettings;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void setInitX(float initX) {
      this.initX = initX;
   }

   public void setInitY(float initY) {
      this.initY = initY;
   }

   public void setStartX(float startX) {
      this.startX = startX;
   }

   public void setStartY(float startY) {
      this.startY = startY;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setRenderX(float renderX) {
      this.renderX = renderX;
   }

   public void setRenderY(float renderY) {
      this.renderY = renderY;
   }

   public void setRenderInitialized(boolean renderInitialized) {
      this.renderInitialized = renderInitialized;
   }

   public void setPositioned(boolean positioned) {
      this.positioned = positioned;
   }

   public void setDragAlpha(float dragAlpha) {
      this.dragAlpha = dragAlpha;
   }

   public void setLastFrameNs(long lastFrameNs) {
      this.lastFrameNs = lastFrameNs;
   }

   public void setWasMouseDown(boolean wasMouseDown) {
      this.wasMouseDown = wasMouseDown;
   }

   public void setWasRightDown(boolean wasRightDown) {
      this.wasRightDown = wasRightDown;
   }

   public void setContextSettings(List<Setting> contextSettings) {
      this.contextSettings = contextSettings;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      }

      if (!(o instanceof Draggable other)) {
         return false;
      }

      if (!other.canEqual(this)) {
         return false;
      }

      if (Float.compare(this.getX(), other.getX()) != 0) {
         return false;
      }

      if (Float.compare(this.getY(), other.getY()) != 0) {
         return false;
      }

      if (Float.compare(this.getInitX(), other.getInitX()) != 0) {
         return false;
      }

      if (Float.compare(this.getInitY(), other.getInitY()) != 0) {
         return false;
      }

      if (Float.compare(this.getStartX(), other.getStartX()) != 0) {
         return false;
      }

      if (Float.compare(this.getStartY(), other.getStartY()) != 0) {
         return false;
      }

      if (this.isDragging() != other.isDragging()) {
         return false;
      }

      if (Float.compare(this.getWidth(), other.getWidth()) != 0) {
         return false;
      }

      if (Float.compare(this.getHeight(), other.getHeight()) != 0) {
         return false;
      }

      if (Float.compare(this.getRenderX(), other.getRenderX()) != 0) {
         return false;
      }

      if (Float.compare(this.getRenderY(), other.getRenderY()) != 0) {
         return false;
      }

      if (this.isRenderInitialized() != other.isRenderInitialized()) {
         return false;
      }

      if (this.isPositioned() != other.isPositioned()) {
         return false;
      }

      if (Float.compare(this.getDragAlpha(), other.getDragAlpha()) != 0) {
         return false;
      }

      if (this.getLastFrameNs() != other.getLastFrameNs()) {
         return false;
      }

      if (this.isWasMouseDown() != other.isWasMouseDown()) {
         return false;
      }

      if (this.isWasRightDown() != other.isWasRightDown()) {
         return false;
      }

      Object this$name = this.getName();
      Object other$name = other.getName();
      if (this$name == null ? other$name == null : this$name.equals(other$name)) {
         Object this$contextSettings = this.getContextSettings();
         Object other$contextSettings = other.getContextSettings();
         return this$contextSettings == null ? other$contextSettings == null : this$contextSettings.equals(other$contextSettings);
      }

      return false;
   }

   protected boolean canEqual(Object other) {
      return other instanceof Draggable;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getX());
      result = result * 59 + Float.floatToIntBits(this.getY());
      result = result * 59 + Float.floatToIntBits(this.getInitX());
      result = result * 59 + Float.floatToIntBits(this.getInitY());
      result = result * 59 + Float.floatToIntBits(this.getStartX());
      result = result * 59 + Float.floatToIntBits(this.getStartY());
      result = result * 59 + (this.isDragging() ? 79 : 97);
      result = result * 59 + Float.floatToIntBits(this.getWidth());
      result = result * 59 + Float.floatToIntBits(this.getHeight());
      result = result * 59 + Float.floatToIntBits(this.getRenderX());
      result = result * 59 + Float.floatToIntBits(this.getRenderY());
      result = result * 59 + (this.isRenderInitialized() ? 79 : 97);
      result = result * 59 + (this.isPositioned() ? 79 : 97);
      result = result * 59 + Float.floatToIntBits(this.getDragAlpha());
      long $lastFrameNs = this.getLastFrameNs();
      result = result * 59 + (int)($lastFrameNs >>> 32 ^ $lastFrameNs);
      result = result * 59 + (this.isWasMouseDown() ? 79 : 97);
      result = result * 59 + (this.isWasRightDown() ? 79 : 97);
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $contextSettings = this.getContextSettings();
      return result * 59 + ($contextSettings == null ? 43 : $contextSettings.hashCode());
   }

   @Override
   public String toString() {
      return "Draggable(x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", name="
         + this.getName()
         + ", initX="
         + this.getInitX()
         + ", initY="
         + this.getInitY()
         + ", startX="
         + this.getStartX()
         + ", startY="
         + this.getStartY()
         + ", dragging="
         + this.isDragging()
         + ", width="
         + this.getWidth()
         + ", height="
         + this.getHeight()
         + ", renderX="
         + this.getRenderX()
         + ", renderY="
         + this.getRenderY()
         + ", renderInitialized="
         + this.isRenderInitialized()
         + ", positioned="
         + this.isPositioned()
         + ", dragAlpha="
         + this.getDragAlpha()
         + ", lastFrameNs="
         + this.getLastFrameNs()
         + ", wasMouseDown="
         + this.isWasMouseDown()
         + ", wasRightDown="
         + this.isWasRightDown()
         + ", contextSettings="
         + this.getContextSettings()
         + ")";
   }
}
