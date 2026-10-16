package pizza.dlc.client.modules.impl.visuals.animatedhands;

import java.util.Locale;
import java.util.Map;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.MovementAnimation;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Animations",
   description = "Анимации рук, ходьбы, бега и прыжка",
   category = ModuleCategory.VISUALS
)
public class AnimatedHands extends Module {
   private static final int DEFAULT_INSPECT_KEY = 74;
   private static final long RELOAD_DEBOUNCE_MS = 250L;
   private static AnimatedHands instance;
   private final BooleanSetting hands = new BooleanSetting("Анимация рук", this);
   private final BooleanSetting walking = new BooleanSetting("Анимация ходьбы", this);
   private final BooleanSetting running = new BooleanSetting("Анимация бега", this);
   private final BooleanSetting jumping = new BooleanSetting("Баскетбольный прыжок", this);
   private final SliderSetting strength = new SliderSetting("Выразительность движений", this, 1, .1F, 1, .05F);
   private final MovementAnimation movement = new MovementAnimation();
   private LivingEntity movementPlayer;
   private final SliderSetting offsetX = new SliderSetting("Смещение X", this, 0.0F, -1.0F, 1.0F, 0.01F);
   private final SliderSetting offsetY = new SliderSetting("Смещение Y", this, 0.0F, -1.0F, 1.0F, 0.01F);
   private final SliderSetting offsetZ = new SliderSetting("Смещение Z", this, 0.0F, -1.0F, 1.0F, 0.01F);
   private final BindSetting inspectKey = new BindSetting("Осмотр предмета", this, DEFAULT_INSPECT_KEY);
   private final BooleanSetting particles = new BooleanSetting("Частицы", this);
   private final BooleanSetting sounds = new BooleanSetting("Звуки", this);
   private String pendingKey;
   private long pendingSinceMs;

   public AnimatedHands() {
      instance = this;
      this.hands.setEnabled(true);
      this.walking.setEnabled(true);
      this.running.setEnabled(true);
      this.jumping.setEnabled(true);
      this.particles.setEnabled(true);
      this.sounds.setEnabled(true);
   }

   public static boolean active() {
      return instance != null && instance.isEnabled() && instance.hands.isEnabled();
   }

   private static boolean bodyActive() {
      return instance != null && instance.isEnabled();
   }

   private static boolean canAnimate(LivingEntity player) {
      return player.isAlive() && player.getPose() == Pose.STANDING && !player.isPassenger()
         && !player.isInWater() && !player.isInLava() && !player.onClimbable()
         && !player.isFallFlying() && !player.isAutoSpinAttack()
         && mc.player != null && !mc.player.getAbilities().flying;
   }

   private void trackPlayer(LivingEntity player) {
      if (this.movementPlayer != player) {
         this.movement.reset();
         this.movementPlayer = player;
      }
   }

   public static void movementTick(LivingEntity player) {
      if (!bodyActive() || player != mc.player) return;
      instance.trackPlayer(player);
      float speed = (float)Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);
      instance.movement.tick(player.tickCount, canAnimate(player), player.onGround(), player.isSprinting(), speed);
   }

   public static void jumped(LivingEntity player) {
      if (!bodyActive() || player != mc.player || !instance.jumping.isEnabled() || !canAnimate(player)
         || player.getDeltaMovement().y <= 0) return;
      instance.trackPlayer(player);
      boolean fromSprint = player.isSprinting() && mc.player.input.getMoveVector().lengthSquared() > 1.0E-4F;
      instance.movement.jumped(player.tickCount, fromSprint);
   }

   public static EmoteEngine.Pose movementPose(Avatar player, AvatarRenderState state, float partialTick) {
      if (!bodyActive() || player != mc.player || instance.movementPlayer != player || !canAnimate(player)
         || state.attackTime > 0 || state.isSpectator) return null;
      return instance.movement.sample(partialTick, state.walkAnimationPos, instance.walking.isEnabled(), instance.running.isEnabled(),
         instance.jumping.isEnabled(), state.isUsingItem || player.isUsingItem(), instance.strength.getValue());
   }

   public static boolean particlesEnabled() {
      return instance != null && instance.particles.isEnabled();
   }

   public static boolean soundsEnabled() {
      return instance != null && instance.sounds.isEnabled();
   }

   public static boolean prepareFrame() {
      if (!active()) return false;
      HmiHandRenderer.beginFrame();
      return instance.ensureScripts();
   }

   @Override
   public void onEnable() {
      this.movement.reset();
      this.movementPlayer = null;
      HmiRuntime.invalidate();
      HmiHandRenderer.reset();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.movement.reset();
      this.movementPlayer = null;
      HmiHandRenderer.reset();
      super.onDisable();
   }

   private boolean ensureScripts() {
      String key = this.scriptKey();
      long now = System.currentTimeMillis();
      if (!key.equals(this.pendingKey)) {
         this.pendingKey = key;
         this.pendingSinceMs = now;
      }
      boolean stable = now - this.pendingSinceMs >= RELOAD_DEBOUNCE_MS;
      if (HmiRuntime.loadedKey() == null || stable) return HmiRuntime.ensureLoaded(key, this.vars());
      return HmiRuntime.hasScripts();
   }

   private String scriptKey() {
      return String.format(Locale.ROOT, "%.3f|%.3f|%.3f|%d", this.offsetX.getValue(), this.offsetY.getValue(), this.offsetZ.getValue(), this.inspectKey.getKey());
   }

   private Map<String, String> vars() {
      return Map.of(
         "xOffset", number(this.offsetX.getValue()),
         "yOffset", number(this.offsetY.getValue()),
         "zOffset", number(this.offsetZ.getValue()),
         "inspectKeybind", Integer.toString(this.inspectKey.getKey())
      );
   }

   private static String number(float value) {
      return String.format(Locale.ROOT, "%.4f", value);
   }
}
