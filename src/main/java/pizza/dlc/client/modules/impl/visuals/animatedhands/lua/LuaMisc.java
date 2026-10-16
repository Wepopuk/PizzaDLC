package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;

public final class LuaMisc {
   public static final class Sound {
      public void playSound(String name, double volume) {
         LocalPlayer player = Minecraft.getInstance().player;
         Identifier id = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name);
         if (player == null || id == null || !AnimatedHands.soundsEnabled()) return;
         player.playSound(SoundEvent.createVariableRangeEvent(id), (float)volume, 1.0F);
      }
   }

   public static final class Camera {
      public void setCamPos(double x, double y, double z) {
      }

      public void setCamRot(double x, double y, double z) {
      }
   }

   public static final class Keys {
      public boolean isKeyPressed(int key) {
         Minecraft mc = Minecraft.getInstance();
         return key > 0 && mc.gui.screen() == null && InputConstants.isKeyDown(mc.getWindow(), key);
      }
   }

   public static final class Debug {
      public void out(String text) {
      }

      public void clear() {
      }
   }

   private LuaMisc() {
   }
}
