package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiSwing;

public final class LuaPlayer {
   public double getHealth(AbstractClientPlayer p) {
      return p.getHealth();
   }

   public boolean isSneaking(AbstractClientPlayer p) {
      return p.isShiftKeyDown();
   }

   public boolean isOnGround(AbstractClientPlayer p) {
      return p.onGround();
   }

   public boolean isSwimming(AbstractClientPlayer p) {
      return p.isSwimming();
   }

   public boolean isClimbing(AbstractClientPlayer p) {
      return p.onClimbable();
   }

   public boolean isCrawling(AbstractClientPlayer p) {
      return p.isVisuallyCrawling();
   }

   public boolean isSubmergedInWater(AbstractClientPlayer p) {
      return p.isUnderWater();
   }

   public boolean isTouchingWater(AbstractClientPlayer p) {
      return p.isInWater();
   }

   public boolean isUsingSpyglass(AbstractClientPlayer p) {
      return p.isScoping();
   }

   public boolean isUsingRiptide(AbstractClientPlayer p) {
      return p.isAutoSpinAttack();
   }

   public double getX(AbstractClientPlayer p) {
      return p.getX();
   }

   public double getY(AbstractClientPlayer p) {
      return p.getY();
   }

   public double getZ(AbstractClientPlayer p) {
      return p.getZ();
   }

   public double getXSpeed(AbstractClientPlayer p) {
      return p.getDeltaMovement().x;
   }

   public double getYSpeed(AbstractClientPlayer p) {
      return p.getDeltaMovement().y;
   }

   public double getZSpeed(AbstractClientPlayer p) {
      return p.getDeltaMovement().z;
   }

   public double getSpeed(AbstractClientPlayer p) {
      return p.getDeltaMovement().length();
   }

   public boolean isUsingItem(AbstractClientPlayer p) {
      return p.isUsingItem();
   }

   public double getYaw(AbstractClientPlayer p) {
      return p.getYHeadRot();
   }

   public double getPitch(AbstractClientPlayer p) {
      return p.getXRot();
   }

   public ItemStack getMainItem(AbstractClientPlayer p) {
      return p.getMainHandItem();
   }

   public ItemStack getOffhandItem(AbstractClientPlayer p) {
      return p.getOffhandItem();
   }

   public InteractionHand getActiveHand(AbstractClientPlayer p) {
      return p.getUsedItemHand();
   }

   public int getAge(AbstractClientPlayer p) {
      return p.tickCount;
   }

   public boolean isItemCoolingDown(ItemStack stack, AbstractClientPlayer p) {
      return p.getCooldowns().isOnCooldown(stack);
   }

   public double getSwingCount(AbstractClientPlayer p) {
      return p instanceof HmiSwing swing ? swing.pizzadlc$hmiSwingCount() : 0.0;
   }

   public String getStandingBlock(AbstractClientPlayer p) {
      return blockAt(p, p.blockPosition().below());
   }

   public String getBlockBelow(AbstractClientPlayer p, int depth) {
      return blockAt(p, p.blockPosition().below().below(depth));
   }

   public String getBlockAbove(AbstractClientPlayer p, int height) {
      return blockAt(p, p.blockPosition().above().above().above(height));
   }

   public boolean hasVehicle(AbstractClientPlayer p, int unused) {
      return p.isPassenger();
   }

   private static String blockAt(AbstractClientPlayer p, BlockPos pos) {
      return BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()).toString();
   }
}
