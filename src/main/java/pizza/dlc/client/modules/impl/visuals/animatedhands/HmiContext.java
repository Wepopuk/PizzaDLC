package pizza.dlc.client.modules.impl.visuals.animatedhands;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class HmiContext {
   public PoseStack matrices;
   public boolean bl;
   public float swingProgress;
   public ItemStack item;
   public AbstractClientPlayer player;
   public InteractionHand hand;
   public boolean mainHand;
   public float deltaTime;
   public float equipProgress;
   public float mainHandSwingProgress;
   public float offHandSwingProgress;
   public boolean mainHandSwitchEvent;
   public boolean offHandSwitchEvent;
   public boolean swingMHand;
   public boolean swingOHand;
   public boolean interact;
   public boolean blockBreaking;
   public List<HmiParticle> particles;

   void update(PoseStack matrices, boolean right, float swing, ItemStack item, HmiHandRenderer.Frame frame, float equip) {
      this.matrices = matrices;
      this.bl = right;
      this.swingProgress = swing;
      this.item = item;
      this.player = frame.player;
      this.hand = frame.hand;
      this.mainHand = frame.mainHand;
      this.deltaTime = HmiRuntime.deltaTime();
      this.equipProgress = equip;
      this.mainHandSwingProgress = frame.mainSwing;
      this.offHandSwingProgress = frame.offSwing;
      this.mainHandSwitchEvent = frame.mainSwitch;
      this.offHandSwitchEvent = frame.offSwitch;
      this.swingMHand = frame.swingMain;
      this.swingOHand = frame.swingOff;
      this.interact = frame.interact;
      this.blockBreaking = frame.blockBreaking;
      this.particles = frame.particles;
   }
}
