package pizza.dlc.client.modules.impl.misc;

import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.events.impl.render.EventRenderHud;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "FreeCam",
   description = "Свободная камера (тело остаётся на месте)",
   category = ModuleCategory.OTHER
)
public class FreeCam extends Module {
   private final SliderSetting speed = new SliderSetting("Скорость", this, 1.0F, 1.0F, 5.0F, 0.01F);
   private final SliderSetting verticalSpeed = new SliderSetting("Скорость по вертикали", this, 0.42F, 0.1F, 5.0F, 0.01F);
   private final BooleanSetting freeze = new BooleanSetting("Заморозка", this);
   private final BooleanSetting showBody = new BooleanSetting("Показывать тело", "Рисует твою модельку — видно, где осталось тело", this);
   private final BooleanSetting showDelta = new BooleanSetting("Смещение камеры", "Координаты и дистанция от тела до камеры, под прицелом", this);
   private float fakeYaw;
   private float fakePitch;
   private float prevFakeYaw;
   private float prevFakePitch;
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;
   private float lockedYaw;
   private float lockedPitch;
   private Vec3 frozenPos;
   private boolean freezeActive;
   private boolean prevSmartCull;
   private static final float LINE_H = 9.0F;
   private static final float CROSSHAIR_GAP = 14.0F;

   public FreeCam() {
      this.showBody.setEnabled(true);
      this.showDelta.setEnabled(true);
   }

   public static FreeCam get() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      return cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(FreeCam.class) : null;
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null) {
         this.prevSmartCull = mc.smartCull;
         mc.smartCull = false;
         this.frozenPos = mc.player.position();
         this.freezeActive = this.freeze.isEnabled();
         this.fakePitch = mc.player.getXRot();
         this.fakeYaw = mc.player.getYRot();
         this.prevFakePitch = this.fakePitch;
         this.prevFakeYaw = this.fakeYaw;
         this.fakeX = mc.player.getX();
         this.fakeY = mc.player.getY() + mc.player.getEyeHeight();
         this.fakeZ = mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         this.lockedYaw = mc.player.getYRot();
         this.lockedPitch = mc.player.getXRot();
         super.onEnable();
      } else {
         this.setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.level != null) {
         mc.smartCull = this.prevSmartCull;
      }

      this.frozenPos = null;
      this.freezeActive = false;
      super.onDisable();
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (!this.isEnabled()) return;

      if (e.getPacket() instanceof ClientboundRespawnPacket || e.getPacket() instanceof ClientboundLoginPacket) {
         mc.execute(() -> {
            if (this.isEnabled()) this.setEnabled(false);
         });
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (!this.isEnabled()) return;

      if (this.freeze.isEnabled() && e.getPacket() instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) return;

      this.prevFakeYaw = this.fakeYaw;
      this.prevFakePitch = this.fakePitch;
      if (this.freeze.isEnabled()) {
         if (!this.freezeActive) {
            this.frozenPos = mc.player.position();
            this.freezeActive = true;
         }

         if (this.frozenPos != null) {
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setPos(this.frozenPos.x, this.frozenPos.y, this.frozenPos.z);
            mc.player.fallDistance = 0.0;
         }
      } else {
         this.freezeActive = false;
      }

      mc.player.setYRot(this.lockedYaw);
      mc.player.setXRot(this.lockedPitch);
      mc.player.setYHeadRot(this.lockedYaw);
      mc.player.setYBodyRot(this.lockedYaw);
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) return;

      float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      double ix = Mth.lerp(pt, mc.player.xo, mc.player.getX());
      double iy = Mth.lerp(pt, mc.player.yo, mc.player.getY());
      double iz = Mth.lerp(pt, mc.player.zo, mc.player.getZ());
      AABB box = mc.player.getBoundingBox().move(ix - mc.player.getX(), iy - mc.player.getY(), iz - mc.player.getZ());
      Render3DUtil.drawBox(e, box, ThemeManager.accentAlpha(144), true);
   }

   @EventHandler
   public void onRenderHud(EventRenderHud e) {
      if (!this.isEnabled() || !this.showDelta.isEnabled() || mc.player == null || mc.level == null) return;

      Vec3 delta = new Vec3(this.getFakeX(), this.getFakeY(), this.getFakeZ()).subtract(mc.player.getEyePosition());
      String axes = String.format(Locale.ROOT, "X %.1f | Y %.1f | Z %.1f", delta.x, delta.y, delta.z);
      String dist = String.format(Locale.ROOT, "дистанция: %.1f", delta.length());
      GuiGraphicsExtractor ctx = e.getContext();
      MsdfFont font = PizzaDLCFonts.sfpro();
      float cx = mc.getWindow().getGuiScaledWidth() / 2.0F;
      float top = mc.getWindow().getGuiScaledHeight() / 2.0F + CROSSHAIR_GAP;
      int color = -1;
      Render2DUtil.msdfText(ctx, font, axes, cx - Render2DUtil.msdfWidth(font, axes, 7.0F) / 2.0F, top, 7.0F, color);
      Render2DUtil.msdfText(ctx, font, dist, cx - Render2DUtil.msdfWidth(font, dist, 7.0F) / 2.0F, top + LINE_H, 7.0F, color);
   }

   @EventHandler
   public void onInputMove(EventInputMove e) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) return;

      double[] motion = calculateMotion(this.speed.getValue(), e.getForward(), e.getStrafe(), this.fakeYaw);
      this.prevFakeX = this.fakeX;
      this.prevFakeY = this.fakeY;
      this.prevFakeZ = this.fakeZ;
      this.fakeX = this.fakeX + motion[0];
      this.fakeZ = this.fakeZ + motion[1];
      if (e.isJump()) {
         this.fakeY = this.fakeY + this.verticalSpeed.getValue();
      }

      if (e.isSneaking()) {
         this.fakeY = this.fakeY - this.verticalSpeed.getValue();
      }

      e.setForward(0.0F);
      e.setStrafe(0.0F);
      e.setJump(false);
      e.setSneaking(false);
      e.setSprint(false);
   }

   public boolean showBody() {
      return this.showBody.isEnabled();
   }

   public void addLookDelta(double deltaX, double deltaY) {
      this.fakeYaw += (float)(deltaX * 0.15);
      this.fakePitch += (float)(deltaY * 0.15);
      this.fakePitch = Mth.clamp(this.fakePitch, -90.0F, 90.0F);
   }

   public float getFakeYaw() {
      return Mth.lerp(this.pt(), this.prevFakeYaw, this.fakeYaw);
   }

   public float getFakePitch() {
      return Mth.lerp(this.pt(), this.prevFakePitch, this.fakePitch);
   }

   public double getFakeX() {
      return Mth.lerp(this.pt(), this.prevFakeX, this.fakeX);
   }

   public double getFakeY() {
      return Mth.lerp(this.pt(), this.prevFakeY, this.fakeY);
   }

   public double getFakeZ() {
      return Mth.lerp(this.pt(), this.prevFakeZ, this.fakeZ);
   }

   private float pt() {
      return mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
   }

   private static double[] calculateMotion(float speed, float forward, float strafe, float yaw) {
      if (forward == 0.0F && strafe == 0.0F) {
         return new double[]{0.0, 0.0};
      }

      if (forward != 0.0F) {
         if (strafe > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (strafe < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         strafe = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double radians = Math.toRadians(yaw + 90.0F);
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      double x = forward * speed * cos + strafe * speed * sin;
      double z = forward * speed * sin - strafe * speed * cos;
      return new double[]{x, z};
   }
}
