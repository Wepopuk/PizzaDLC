package pizza.dlc.client.modules.impl.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.rotation.angle.GrimSmoothMode;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ClickAction",
   category = ModuleCategory.PLAYER
)
public class ClickAction extends Module {
   public final BindSetting pearlBind = new BindSetting("Бинд перл", this, -1);
   public final BindSetting expBind = new BindSetting("Бинд опыт", this, -1);
   public final BindSetting snowballBind = new BindSetting("Бинд снежок", this, -1);
   public final BindSetting eggBind = new BindSetting("Бинд яйцо", this, -1);
   public final BindSetting windBind = new BindSetting("Бинд вихрь", this, -1);
   private final BooleanSetting windAimDown = new BooleanSetting("Целить вниз (взлёт)", this);
   private final BooleanSetting windPerfectJump = new BooleanSetting("Идеальный прыжок", this);
   private final SliderSetting windJumpDelay = new SliderSetting("Задержка прыжка (тиков)", this, 1.0F, 0.0F, 4.0F, 1.0F);
   private Item pendingItem;
   private boolean pendingAimDown;
   private int jumpCountdown;
   private static final int ROTATION_PRIORITY = 60;
   private static final float ROTATION_LANDED_MIN_TOL = 6.0F;
   private static final int AIM_TIMEOUT_TICKS = 6;
   private Angle windTargetAngle;
   private int windAimTicks;

   public ClickAction() {
      this.windAimDown.setEnabled(true);
      this.windPerfectJump.setVisible(this.windAimDown::isEnabled);
      this.windJumpDelay.setVisible(() -> this.windAimDown.isEnabled() && this.windPerfectJump.isEnabled());
      this.jumpCountdown = -1;
      this.windAimTicks = -1;
   }

   @EventHandler
   public void onKey(EventKey e) {
      if (e.getAction() != 1 || mc.gui.screen() != null) return;

      int k = e.getKey();
      if (this.pearlBind.getKey() == k) {
         this.pendingItem = Items.ENDER_PEARL;
      } else if (this.expBind.getKey() == k) {
         this.pendingItem = Items.EXPERIENCE_BOTTLE;
      } else if (this.snowballBind.getKey() == k) {
         this.pendingItem = Items.SNOWBALL;
      } else if (this.eggBind.getKey() == k) {
         this.pendingItem = Items.EGG;
      } else {
         if (this.windBind.getKey() != k) {
            return;
         }

         this.pendingItem = Items.WIND_CHARGE;
         this.pendingAimDown = this.windAimDown.isEnabled();
      }

      e.setCancelled(true);
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;

      if (this.jumpCountdown > 0) {
         this.jumpCountdown--;
      }

      if (this.windAimTicks >= 0) {
         this.requestWindAim();
         if (this.rotationDelivered() || ++this.windAimTicks > AIM_TIMEOUT_TICKS) {
            this.windAimTicks = -1;
            this.windTargetAngle = null;
            this.throwWind();
         }
      } else if (this.pendingItem != null) {
         Item item = this.pendingItem;
         boolean aimDown = this.pendingAimDown;
         this.pendingItem = null;
         this.pendingAimDown = false;
         if (item == Items.WIND_CHARGE && aimDown) {
            this.windAimTicks = 0;
            this.requestWindAim();
         } else {
            SwapSystem.useItem(item);
         }
      }
   }

   private void throwWind() {
      SwapSystem.useItem(Items.WIND_CHARGE);
      if (this.windPerfectJump.isEnabled()) {
         this.jumpCountdown = (int)this.windJumpDelay.getValue();
      }
   }

   private void requestWindAim() {
      RotationController c = RotationController.INSTANCE;
      if (c != null && mc.player != null) {
         this.windTargetAngle = new Angle(mc.player.getYRot(), 90.0F);
         RotationPlan plan = new RotationPlan(this.windTargetAngle, null, null, new GrimSmoothMode(true), 3, 3.0F, true, true);
         c.rotateTo(plan, ROTATION_PRIORITY, this);
      }
   }

   private boolean rotationDelivered() {
      RotationController c = RotationController.INSTANCE;
      if (c != null && this.windTargetAngle != null) {
         float tol = Math.max(ROTATION_LANDED_MIN_TOL, 5.0F * (float)Angle.computeGcd());
         return RotationController.computeRotationDifference(c.getRotation(), this.windTargetAngle) < tol;
      }

      return true;
   }

   @EventHandler
   public void onInputMove(EventInputMove e) {
      if (this.jumpCountdown != 0) return;

      this.jumpCountdown = -1;
      e.setJump(true);
   }
}
