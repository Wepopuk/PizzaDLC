package pizza.dlc.api.util.combat;
import pizza.dlc.api.events.impl.combat.CombatInputEvent;
import pizza.dlc.api.events.impl.combat.CombatPacketEvent;
import pizza.dlc.api.rotation.combat.CombatRotations;

import lombok.Generated;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public final class CombatMovement implements pizza.dlc.api.interfaces.QuickImports {
   private static final CombatMovement.PacketListener f_11918 = new CombatMovement.PacketListener();
   private static boolean f_11919 = false;

   private static final float f_11931 = Float.MAX_VALUE;

   private static final float f_11934 = Float.MAX_VALUE;

   public static void m_4254(double var0) {
      float var2 = mc.player.input.getMoveVector().y;
      float var3 = mc.player.input.getMoveVector().x;
      float var4 = mc.player.getYRot();
      if (var2 == 0.0F && var3 == 0.0F) {
         mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
      } else {
         if (var2 != 0.0F) {
            if (var3 > 0.0F) {
               var4 += var2 > 0.0F ? -45.0F : 45.0F;
            } else if (var3 < 0.0F) {
               var4 += var2 > 0.0F ? 45.0F : -45.0F;
            }

            var3 = 0.0F;
            var2 = var2 > 0.0F ? 1.0F : -1.0F;
         }

         double var5 = Math.toRadians(var4 + 90.0F);
         double var7 = var2 * var0 * Math.cos(var5) + var3 * var0 * Math.sin(var5);
         double var9 = var2 * var0 * Math.sin(var5) - var3 * var0 * Math.cos(var5);
         mc.player.setDeltaMovement(var7, mc.player.getDeltaMovement().y, var9);
      }
   }

   @Generated
   private CombatMovement() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void correctInput(CombatInputEvent var0) {
      float var1 = var0.getForward();
      float var2 = var0.getStrafe();
      double var3 = Mth.wrapDegrees(
         Math.toDegrees(m_4262(mc.player.isFallFlying() ? mc.player.getYRot() : CombatRotations.state().getClientRotation().x, var1, var2))
      );
      if (var1 != 0.0F || var2 != 0.0F) {
         float var5 = 0.0F;
         float var6 = 0.0F;
         float var7 = f_11931;

         for (float var8 = -1.0F; var8 <= 1.0F; var8++) {
            for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
               if (var9 != 0.0F || var8 != 0.0F) {
                  double var10 = Mth.wrapDegrees(Math.toDegrees(m_4262(mc.player.getYRot(), var8, var9)));
                  double var12 = Math.abs(Mth.wrapDegrees(var3 - var10));
                  if (var12 < var7) {
                     var7 = (float)var12;
                     var5 = var8;
                     var6 = var9;
                  }
               }
            }
         }

         var0.setForwardPressed(var5 > 0.0F);
         var0.setBackwardPressed(var5 < 0.0F);
         var0.setLeftPressed(var6 > 0.0F);
         var0.setRightPressed(var6 < 0.0F);
         var0.setForward(var5);
         var0.setStrafe(var6);
      }
   }

   public static boolean m_4256() {
      Vec2 var0 = mc.player.input.getMoveVector();
      return var0.y != 0.0F || var0.x != 0.0F;
   }

   public static double[] m_4257() {
      float var0 = m_4260();
      if (var0 == -1.0F) {
         return new double[]{0.0, 0.0};
      }

      double var1 = Math.toRadians(var0);
      return new double[]{-Math.sin(var1), Math.cos(var1)};
   }

   public static double m_4258(boolean var0) {
      Vec2 var1 = mc.player.input.getMoveVector();
      float var2 = var1.y;
      float var3 = var1.x;
      float var4 = mc.player.getYRot();
      if (var2 < 0.0F) {
         var4 += 180.0F;
      }

      float var5 = 1.0F;
      if (var2 < 0.0F) {
         var5 = -0.5F;
      } else if (var2 > 0.0F) {
         var5 = 0.5F;
      }

      if (var3 > 0.0F) {
         var4 -= 90.0F * var5;
      }

      if (var3 < 0.0F) {
         var4 += 90.0F * var5;
      }

      return var0 ? Math.toRadians(var4) : var4;
   }

   public static void m_4259(double var0, double var2) {
      double var4 = mc.player.input.getMoveVector().y;
      double var6 = mc.player.input.getMoveVector().x;
      float var8 = mc.player.getYRot();
      double var9 = 0.0;
      if (mc.options.keyJump.isDown()) {
         var9 = var2;
      } else if (mc.options.keyShift.isDown()) {
         var9 = -var2;
      }

      if (var4 == 0.0 && var6 == 0.0) {
         mc.player.setDeltaMovement(0.0, var9, 0.0);
      } else {
         if (var4 != 0.0) {
            if (var6 > 0.0) {
               var8 += var4 > 0.0 ? -45 : 45;
            } else if (var6 < 0.0) {
               var8 += var4 > 0.0 ? 45 : -45;
            }

            var6 = 0.0;
            if (var4 > 0.0) {
               var4 = 1.0;
            } else if (var4 < 0.0) {
               var4 = -1.0;
            }
         }

         mc.player
            .setDeltaMovement(
               var4 * var0 * Mth.cos((float)Math.toRadians(var8 + 90.0F)) + var6 * var0 * Mth.sin((float)Math.toRadians(var8 + 90.0F)),
               var9,
               var4 * var0 * Mth.sin((float)Math.toRadians(var8 + 90.0F)) - var6 * var0 * Mth.cos((float)Math.toRadians(var8 + 90.0F))
            );
      }
   }

   public static float m_4260() {
      boolean var0 = mc.options.keyRight.isDown();
      boolean var1 = mc.options.keyLeft.isDown();
      boolean var2 = mc.options.keyUp.isDown();
      boolean var3 = mc.options.keyDown.isDown();
      float var4 = mc.player.getYRot();
      if (!var0 && !var3 && !var1 && !var2) {
         return -1.0F;
      }

      if (var3) {
         var4 -= 180.0F;
      }

      boolean var5 = (var3 || var2) && (!var3 || !var2);
      if (!var5) {
         if (var1) {
            var4 -= 90.0F;
         }

         if (var0) {
            var4 += 90.0F;
         }
      } else if (var4 == mc.player.getYRot()) {
         if (var1) {
            var4 -= 45.0F;
         }

         if (var0) {
            var4 += 45.0F;
         }
      } else {
         if (var1) {
            var4 += 45.0F;
         }

         if (var0) {
            var4 -= 45.0F;
         }
      }

      return Mth.wrapDegrees(var4);
   }

   private static void onPacket(CombatPacketEvent var0) {
      if (var0.getPacket() instanceof ServerboundPlayerCommandPacket var1) {
         if (var1.getAction().equals(Action.STOP_SPRINTING)) {
            f_11919 = false;
         } else if (var1.getAction().equals(Action.START_SPRINTING)) {
            f_11919 = true;
         }
      }
   }

   public static double m_4262(float var0, double var1, double var3) {
      if (var1 < 0.0) {
         var0 += 180.0F;
      }

      float var5 = 1.0F;
      if (var1 < 0.0) {
         var5 = -0.5F;
      } else if (var1 > 0.0) {
         var5 = 0.5F;
      }

      if (var3 > 0.0) {
         var0 -= 90.0F * var5;
      }

      if (var3 < 0.0) {
         var0 += 90.0F * var5;
      }

      return Math.toRadians(var0);
   }

   public static boolean m_4263() {
      return f_11919;
   }

   public static void m_4264(double var0) {
      if (m_4256()) {
         double var2 = m_4258(true);
         mc.player.setDeltaMovement(-Math.sin(var2) * var0, mc.player.getDeltaMovement().y, Math.cos(var2) * var0);
      }
   }

   public static void m_4265(CombatInputEvent var0, float var1) {
      float var2 = var0.getForward();
      float var3 = var0.getStrafe();
      double var4 = Mth.wrapDegrees(Math.toDegrees(m_4262(mc.player.isFallFlying() ? mc.player.getYRot() : var1, var2, var3)));
      if (var2 != 0.0F || var3 != 0.0F) {
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = f_11934;

         for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
            for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
               if (var10 != 0.0F || var9 != 0.0F) {
                  double var11 = Mth.wrapDegrees(Math.toDegrees(m_4262(mc.player.getYRot(), var9, var10)));
                  double var13 = Math.abs(Mth.wrapDegrees(var4 - var11));
                  if (var13 < var8) {
                     var8 = (float)var13;
                     var6 = var9;
                     var7 = var10;
                  }
               }
            }
         }

         var0.setForwardPressed(var6 > 0.0F);
         var0.setBackwardPressed(var6 < 0.0F);
         var0.setLeftPressed(var7 > 0.0F);
         var0.setRightPressed(var7 < 0.0F);
         var0.setForward(var6);
         var0.setStrafe(var7);
      }
   }

   static {
      pizza.dlc.PizzaDLC.getInstance().getBus().subscribe(f_11918);
   }

   public static class PacketListener {
      @meteordevelopment.orbit.EventHandler
      private void onPacket(CombatPacketEvent event) { CombatMovement.onPacket(event); }

   }
}
