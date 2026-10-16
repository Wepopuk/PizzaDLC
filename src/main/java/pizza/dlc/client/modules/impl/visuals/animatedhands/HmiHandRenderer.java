package pizza.dlc.client.modules.impl.visuals.animatedhands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class HmiHandRenderer {
   private static final String SPACE_ITEM = "ITEM";
   private static final String SPACE_SCREEN = "SCREEN";
   private static final List<HmiParticle> PARTICLES = new ArrayList<>();
   private static final Frame FRAME = new Frame();
   private static final BlockDisplayContext BLOCK_DISPLAY = BlockDisplayContext.create();
   private static BlockModelResolver blockResolver;
   private static Item prevMain = Items.AIR;
   private static Item prevOff = Items.AIR;

   private HmiHandRenderer() {
   }

   public interface Access {
      void pizzadlc$hmiRenderArm(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm);

      void pizzadlc$hmiRenderMap(PoseStack pose, SubmitNodeCollector collector, int light, ItemStack stack);
   }

   static final class Frame {
      AbstractClientPlayer player;
      InteractionHand hand;
      boolean mainHand;
      float mainSwing;
      float offSwing;
      boolean mainSwitch;
      boolean offSwitch;
      boolean swingMain;
      boolean swingOff;
      boolean interact;
      boolean blockBreaking;
      final List<HmiParticle> particles = PARTICLES;
   }

   public static void beginFrame() {
      HmiRuntime.beginFrame();
   }

   public static void reset() {
      PARTICLES.clear();
      blockResolver = null;
      prevMain = Items.AIR;
      prevOff = Items.AIR;
   }

   public static void render(
      ItemInHandRenderer renderer,
      AbstractClientPlayer player,
      float partialTick,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light
   ) {
      if (player.isScoping()) return;
      Access access = (Access)renderer;
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      stack = hiddenByBowInOtherHand(player, hand, mainHand, stack);
      if (mainHand) {
         FRAME.mainSwitch = stack.getItem() != prevMain;
      } else {
         FRAME.offSwitch = stack.getItem() != prevOff;
      }

      FRAME.player = player;
      FRAME.hand = hand;
      FRAME.mainHand = mainHand;
      FRAME.interact = false;
      FRAME.blockBreaking = false;
      float swing = swingProgress;
      if (player instanceof HmiSwing tracker) {
         FRAME.mainSwing = tracker.pizzadlc$hmiMainSwing(partialTick);
         FRAME.offSwing = tracker.pizzadlc$hmiOffSwing(partialTick);
         swing = mainHand ? FRAME.mainSwing : FRAME.offSwing;
         FRAME.swingMain = tracker.pizzadlc$hmiMainEvent();
         FRAME.swingOff = tracker.pizzadlc$hmiOffEvent();
         FRAME.interact = mainHand ? tracker.pizzadlc$hmiMainInteract() : tracker.pizzadlc$hmiOffInteract();
         FRAME.blockBreaking = tracker.pizzadlc$hmiBlockBreaking();
      }

      HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean right = arm == HumanoidArm.RIGHT;
      int side = right ? 1 : -1;
      pose.pushPose();
      scenePose(pose, stack, right, swing, equipProgress);
      pose.pushPose();
      handPose(pose, stack, right, swing, equipProgress, side);
      if (!player.isInvisible()) {
         int armLight = LightCoordsUtil.lightCoordsWithEmission(light, Block.byItem(stack.getItem()).defaultBlockState().getLightEmission());
         access.pizzadlc$hmiRenderArm(pose, collector, armLight, arm);
      }
      pose.popPose();
      pose.pushPose();
      if (rendersAsBlock(stack)) {
         renderBlockItem(renderer, player, hand, stack, right, side, equipProgress, pose, collector, light);
      } else {
         itemPose(pose, stack, right, swing, equipProgress);
         ItemUseAnimation use = stack.getUseAnimation();
         if (use == ItemUseAnimation.BLOCK || use == ItemUseAnimation.BRUSH || use == ItemUseAnimation.SPEAR) {
            ItemStack shown = override(mainHand);
            renderer.renderItem(player, shown.isEmpty() ? stack.copy() : shown, displayContext(right), pose, collector, light);
         } else {
            if (stack.has(DataComponents.MAP_ID)) {
               pose.pushPose();
               pose.translate(-0.05 * side, 0.2, 0.1);
               pose.mulPose(Axis.YP.rotationDegrees(-12 * side));
               access.pizzadlc$hmiRenderMap(pose, collector, light, stack);
               pose.popPose();
            } else {
               ItemStack shown = override(mainHand);
               renderer.renderItem(player, shown.isEmpty() ? stack : shown, displayContext(right), pose, collector, light);
            }

            pose.pushPose();
            HmiParticles.draw(PARTICLES, pose, collector, SPACE_ITEM, hand, light);
            pose.popPose();
         }
      }

      pose.popPose();
      pose.popPose();
      if (mainHand) {
         prevMain = stack.getItem();
      } else {
         prevOff = stack.getItem();
      }

      pose.pushPose();
      HmiParticles.draw(PARTICLES, pose, collector, SPACE_SCREEN, hand, light);
      pose.popPose();
      HmiRuntime.mainHandOverride = ItemStack.EMPTY;
      HmiRuntime.offHandOverride = ItemStack.EMPTY;
   }

   private static ItemStack hiddenByBowInOtherHand(AbstractClientPlayer player, InteractionHand hand, boolean mainHand, ItemStack stack) {
      if (!player.isUsingItem() || player.getUsedItemHand() == hand) return stack;
      ItemStack other = mainHand ? player.getOffhandItem() : player.getMainHandItem();
      ItemUseAnimation use = other.getUseAnimation();
      boolean bowLike = use == ItemUseAnimation.BOW || use == ItemUseAnimation.CROSSBOW && !CrossbowItem.isCharged(other);
      return bowLike ? ItemStack.EMPTY : stack;
   }

   private static ItemStack override(boolean mainHand) {
      return mainHand ? HmiRuntime.mainHandOverride : HmiRuntime.offHandOverride;
   }

   private static ItemDisplayContext displayContext(boolean right) {
      return right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
   }

   private static boolean rendersAsBlock(ItemStack stack) {
      Block block = Block.byItem(stack.getItem());
      if (block == Blocks.AIR) return false;
      BlockState state = block.defaultBlockState();
      return !state.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS)
         && !state.is(BlockTags.CROPS)
         && stack.getUseAnimation() != ItemUseAnimation.EAT
         && !stack.is(Items.REDSTONE)
         && !stack.is(ItemTags.BANNERS)
         && !stack.is(ItemTags.SKULLS)
         && HmiRuntime.flag(HmiRuntime.RENDER_AS_BLOCK, stack.getItem().toString(), true);
   }

   private static void renderBlockItem(
      ItemInHandRenderer renderer,
      AbstractClientPlayer player,
      InteractionHand hand,
      ItemStack stack,
      boolean right,
      int side,
      float equipProgress,
      PoseStack pose,
      SubmitNodeCollector collector,
      int light
   ) {
      BlockState state = Block.byItem(stack.getItem()).defaultBlockState();
      itemPose(pose, stack, right, 0.0F, equipProgress);
      pose.translate(0.22 * side, 0.25, 0.2);
      if ((stack.is(Items.LEVER) || state.is(BlockTags.BUTTONS)) && state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
         state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
      }

      boolean tall = stack.getItem().toString().contains("torch")
         || state.getBlock() instanceof LanternBlock
         || state.is(BlockTags.ALL_HANGING_SIGNS);
      if (tall) {
         pose.translate(-0.05 * side, 0.0, 0.0);
         pose.scale(1.75F, 1.75F, 1.75F);
      } else {
         pose.translate(-0.25 * side, -0.05, 0.0);
      }

      if (!right) pose.translate(-0.3F, 0.0F, 0.0F);
      pose.scale(0.3F, 0.3F, 0.3F);
      pose.translate(-0.9 * side, -0.45, -0.7);
      if (stack.is(Items.BELL)) {
         renderer.renderItem(player, stack, ItemDisplayContext.NONE, pose, collector, light);
      } else {
         if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            pose.pushPose();
            pose.translate(0.0F, 1.0F, 0.0F);
            submitBlock(state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), pose, collector, light);
            pose.popPose();
         }

         if (!submitBlock(state, pose, collector, light)) {
            renderer.renderItem(player, stack, ItemDisplayContext.NONE, pose, collector, light);
         }
      }

      pose.pushPose();
      if (!right) pose.translate(1.0F, 0.0F, 0.0F);
      HmiParticles.draw(PARTICLES, pose, collector, SPACE_ITEM, hand, light);
      pose.popPose();
   }

   private static boolean submitBlock(BlockState state, PoseStack pose, SubmitNodeCollector collector, int light) {
      if (blockResolver == null) blockResolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
      BlockModelRenderState renderState = new BlockModelRenderState();
      blockResolver.update(renderState, state, BLOCK_DISPLAY);
      if (renderState.isEmpty()) return false;
      renderState.submit(pose, collector, LightCoordsUtil.lightCoordsWithEmission(light, state.getLightEmission()), OverlayTexture.NO_OVERLAY, 0);
      return true;
   }

   private static void scenePose(PoseStack pose, ItemStack stack, boolean right, float swing, float equip) {
      HmiRuntime.CONTEXT.update(pose, right, swing, stack, FRAME, equip);
      HmiRuntime.runHand();
      if (!stack.isEmpty()) pose.translate(0.0, -0.35, 0.2);
   }

   private static void handPose(PoseStack pose, ItemStack stack, boolean right, float swing, float equip, int side) {
      HmiRuntime.CONTEXT.update(pose, right, swing, stack, FRAME, equip);
      HmiRuntime.runHandRelative();
      if (stack.isEmpty()) return;
      pose.translate(1.5 * side, -0.3, -0.6);
      pose.rotateAround(Axis.XP.rotationDegrees(15.0F), 0.5F * side, 0.5F, 0.5F);
      pose.rotateAround(Axis.YP.rotationDegrees(35 * side), 0.5F * side, 0.5F, 0.5F);
      pose.rotateAround(Axis.ZP.rotationDegrees(-65 * side), 0.5F * side, 0.5F, 0.5F);
      pose.scale(0.9F, 0.9F, 0.9F);
   }

   private static void itemPose(PoseStack pose, ItemStack stack, boolean right, float swing, float equip) {
      int side = right ? 1 : -1;
      pose.translate(0.5 * side, -0.15, -0.85);
      pose.rotateAround(Axis.XP.rotationDegrees(15.0F), 0.5F, 0.5F, 0.5F);
      pose.scale(0.9F, 0.9F, 0.9F);
      HmiRuntime.CONTEXT.update(pose, right, swing, stack, FRAME, equip);
      HmiRuntime.runItem();
   }
}
