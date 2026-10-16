package pizza.dlc.client.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Zoom",
   description = "Hold-to-zoom",
   category = ModuleCategory.OTHER
)
public class Zoom extends Module {
   public static final float FOV_MULTIPLIER = 0.25F;
   public final BindSetting bind = new BindSetting("Бинд", this, 67);
   public final SliderSetting animationSpeed = new SliderSetting("Скорость анимации", this, 0.22F, 0.05F, 1.0F, 0.01F);
   public final SliderSetting scrollStep = new SliderSetting("Шаг колеса", this, 0.05F, 0.01F, 0.25F, 0.01F);
   private boolean zooming;
   private float targetMultiplier = FOV_MULTIPLIER;
   private float animatedMultiplier = 1.0F;
   private long lastAnimationTimeNanos = System.nanoTime();

   public Zoom() {
      this.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.zooming = false;
      this.animatedMultiplier = 1.0F;
      this.lastAnimationTimeNanos = System.nanoTime();
      super.onDisable();
   }

   @Override
   public void setEnabled(boolean enabled) {
      if (!enabled) return;

      super.setEnabled(true);
   }

   @Override
   public void toggle() {
      this.setEnabled(true);
   }

   @Override
   public void setKey(int key) {
      super.setKey(-1);
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (mc.gui.screen() == null && mc.getWindow() != null) {
         int key = this.bind.getKey();
         boolean held = key > 0 && GLFW.glfwGetKey(StreamProofRenderer.inputHandle(mc.getWindow().handle()), key) == GLFW.GLFW_PRESS;
         if (held && !this.zooming) {
            this.targetMultiplier = FOV_MULTIPLIER;
         }
         this.zooming = held;
      } else {
         this.zooming = false;
      }
   }

   public boolean isZooming() {
      return this.isEnabled() && this.zooming && mc.gui.screen() == null;
   }

   public boolean handleScroll(double vertical) {
      if (this.isZooming() && !(Math.abs(vertical) < 0.001)) {
         float direction = vertical > 0.0 ? -1.0F : 1.0F;
         this.targetMultiplier = Mth.clamp(this.targetMultiplier + direction * this.scrollStep.getValue(), 0.05F, 1.0F);
         return true;
      }

      return false;
   }

   public boolean shouldApply() {
      if (!this.isEnabled()) return false;
      this.updateAnimation();
      return this.animatedMultiplier < 0.999F || this.isZooming();
   }

   public float getAnimatedMultiplier() {
      this.updateAnimation();
      return Mth.clamp(this.animatedMultiplier, 0.05F, 1.0F);
   }

   private void updateAnimation() {
      long now = System.nanoTime();
      float deltaSeconds = Math.min((float)(now - this.lastAnimationTimeNanos) / 1.0E9F, 0.1F);
      this.lastAnimationTimeNanos = now;
      float target = this.isZooming() ? this.targetMultiplier : 1.0F;
      float smoothing = 1.0F - (float)Math.exp(-this.animationSpeed.getValue() * 14.0F * deltaSeconds);
      this.animatedMultiplier = this.animatedMultiplier + (target - this.animatedMultiplier) * smoothing;
      if (Math.abs(this.animatedMultiplier - target) < 5.0E-4F) {
         this.animatedMultiplier = target;
      }
   }

   public static Zoom get() {
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      return client != null && client.getModuleManager() != null ? client.getModuleManager().get(Zoom.class) : null;
   }
}
