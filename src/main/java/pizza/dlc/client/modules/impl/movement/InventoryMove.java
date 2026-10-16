package pizza.dlc.client.modules.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import pizza.dlc.api.ui.TypingScreen;
import net.minecraft.world.entity.player.Input;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.mixin.client.KeyMappingAccessor;

@ModuleInfo(
   name = "InventoryMove",
   description = "Ходьба при открытом инвентаре/экране",
   category = ModuleCategory.MOVEMENT
)
public class InventoryMove extends Module {
   public final pizza.dlc.api.settings.impl.ModeSetting bypass = new pizza.dlc.api.settings.impl.ModeSetting("Обход", this, "Никакой", "Grim");
   private final java.util.Set<Object> blockers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
   private boolean forcedBlocked;
   private final BooleanSetting resetSprint = new BooleanSetting("Сбрасывать спринт", this);

   public static boolean shouldResetSprint() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         InventoryMove m = cc.getModuleManager().get(InventoryMove.class);
         return m != null && m.isEnabled() && m.resetSprint.isEnabled();
      }

      return false;
   }

   private static InventoryMove instance() {
      var cc=PizzaDLCClient.getInstance();
      return cc == null || cc.getModuleManager() == null ? null : cc.getModuleManager().get(InventoryMove.class);
   }
   public void block(Object owner) { if (owner != null) blockers.add(owner); }
   public void unblock(Object owner) { blockers.remove(owner); }
   public void setBlocked(boolean value) { forcedBlocked=value; }
   public boolean isBlocked() { return forcedBlocked || !blockers.isEmpty(); }
   public static boolean grimBypass() { var module=instance(); return module != null && module.isEnabled() && module.bypass.is("Grim"); }
   @Override public void onDisable() { blockers.clear(); forcedBlocked=false; super.onDisable(); }
   @meteordevelopment.orbit.EventHandler private void world(pizza.dlc.api.events.impl.game.EventWorldLoad event) { blockers.clear(); forcedBlocked=false; }

   private static boolean active() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         InventoryMove m = cc.getModuleManager().get(InventoryMove.class);
         return m != null && m.isEnabled();
      }

      return false;
   }

   public static Input override(Input original) {
      if (!active()) {
         return original;
      }

      if (instance().isBlocked()) return Input.EMPTY;
      Screen screen = Minecraft.getInstance().gui.screen();
      if (screen != null && !TypingScreen.isTyping(screen)) {
         Options o = Minecraft.getInstance().options;
         return new Input(down(o.keyUp), down(o.keyDown), down(o.keyLeft), down(o.keyRight), down(o.keyJump), original.shift(), down(o.keySprint));
      }

      return original;
   }

   private static boolean down(KeyMapping key) {
      var binding = ((KeyMappingAccessor)key).pizzadlc$key();
      int code = binding.getValue();
      if (code < 0) return false;
      var window = Minecraft.getInstance().getWindow();
      return binding.getType() == InputConstants.Type.MOUSE
         ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(pizza.dlc.api.util.render.streamproof.StreamProofRenderer.inputHandle(window.handle()), code) == org.lwjgl.glfw.GLFW.GLFW_PRESS
         : InputConstants.isKeyDown(window, code);
   }
}
