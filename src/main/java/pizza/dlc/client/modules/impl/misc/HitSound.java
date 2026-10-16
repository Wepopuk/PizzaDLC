package pizza.dlc.client.modules.impl.misc;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ButtonSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.settings.impl.StringSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "HitSound",
   category = ModuleCategory.OTHER,
   description = "Свой звук при попадании по цели"
)
public class HitSound extends Module {
   private static final String STRONG = "Ванильный удар";
   private static final String CRIT = "Ванильный крит";
   private static final String WEAK = "Слабый удар";
   private static final String KNOCKBACK = "Отброс";
   private static final String SPEAR = "Копьё";
   private static final String DOOR = "Стук в дверь";
   private static final String SERVER = "С сервера";
   private static final long HIT_CONFIRM_MS = 1500L;
   private static final long CAPTURE_WINDOW_MS = 400L;
   private static final double CAPTURE_RANGE = 8.0;
   private static final double OWN_SOUND_RANGE = 2.0;
   private static final int CAPTURE_CONFIRMATIONS = 2;
   private final ModeSetting sound = new ModeSetting("Звук", this, STRONG, CRIT, WEAK, KNOCKBACK, SPEAR, DOOR, SERVER);
   private final SliderSetting volume = new SliderSetting("Громкость", this, 1.0F, 0.0F, 1.0F, 0.01F);
   private final SliderSetting pitch = new SliderSetting("Питч", this, 1.0F, 0.5F, 2.0F, 0.01F);
   private final BooleanSetting randomPitch = new BooleanSetting("Случайный питч", this);
   private final BooleanSetting onlyHits = new BooleanSetting("Только попадания", "Звук только когда цель реально получила от тебя урон", this);
   private final BooleanSetting muteVanilla = new BooleanSetting("Убрать ванильный звук удара", this);
   private final StringSetting captured = new StringSetting(
      "Звук сервера", "Запоминается сам: выбери «С сервера» и ударь кого-нибудь пару раз на сервере с включённым звуком", "", 120, this
   );
   private final ButtonSetting preview = new ButtonSetting("Прослушать", this, this::play);
   private final ButtonSetting forget = new ButtonSetting("Забыть звук сервера", this, this::forgetCaptured);
   private final Map<String, Integer> captureCounts = new ConcurrentHashMap<>();
   private final Set<String> countedThisAttack = ConcurrentHashMap.newKeySet();
   private volatile long lastAttackMs;
   private volatile int lastTarget = -1;
   private String lastMode;

   public HitSound() {
      this.onlyHits.setEnabled(true);
      this.randomPitch.setEnabled(true);
      this.muteVanilla.setEnabled(true);
      this.captured.setVisible(() -> this.sound.is(SERVER));
      this.forget.setVisible(() -> this.sound.is(SERVER) && !this.captured.getText().isBlank());
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      String mode = this.sound.get();
      if (this.lastMode != null && !this.lastMode.equals(mode)) {
         this.play();
      }

      this.lastMode = mode;
   }

   @EventHandler
   public void onSend(PacketEvent.Send e) {
      if (!(e.getPacket() instanceof ServerboundAttackPacket attack)) return;

      this.lastAttackMs = System.currentTimeMillis();
      this.lastTarget = attack.entityId();
      this.countedThisAttack.clear();
      if (!this.onlyHits.isEnabled()) {
         this.play();
      }
   }

   @EventHandler
   public void onReceive(PacketEvent.Receive e) {
      LocalPlayer self = mc.player;
      if (self == null) return;

      if (e.getPacket() instanceof ClientboundDamageEventPacket damage) {
         int me = self.getId();
         boolean mine = damage.sourceCauseId() == me || damage.sourceDirectId() == me;
         if (this.onlyHits.isEnabled() && mine && damage.entityId() != me && this.recentAttack(HIT_CONFIRM_MS)) {
            mc.execute(this::play);
         }
      } else if (e.getPacket() instanceof ClientboundSoundPacket packet) {
         double distSq = self.distanceToSqr(packet.getX(), packet.getY(), packet.getZ());
         if (this.handleSound(packet.getSound(), distSq <= OWN_SOUND_RANGE * OWN_SOUND_RANGE, distSq <= CAPTURE_RANGE * CAPTURE_RANGE)) {
            e.setCancelled(true);
         }
      } else if (e.getPacket() instanceof ClientboundSoundEntityPacket packet) {
         boolean own = packet.getId() == self.getId();
         if (this.handleSound(packet.getSound(), own, own || packet.getId() == this.lastTarget)) {
            e.setCancelled(true);
         }
      }
   }

   private boolean handleSound(Holder<SoundEvent> holder, boolean own, boolean near) {
      String id = holder.value().location().toString();
      if (this.muteVanilla.isEnabled() && own && id.startsWith("minecraft:entity.player.attack.")) return true;

      if (near && this.sound.is(SERVER) && this.captured.getText().isBlank() && this.recentAttack(CAPTURE_WINDOW_MS) && !ignoredForCapture(id)) {
         this.countCandidate(id);
      }

      return false;
   }

   private void countCandidate(String id) {
      if (!this.countedThisAttack.add(id)) return;

      if (this.captureCounts.merge(id, 1, Integer::sum) >= CAPTURE_CONFIRMATIONS) {
         this.captureCounts.clear();
         mc.execute(() -> {
            this.captured.setText(id);
            NotificationManager.add("HitSound", "Запомнил звук сервера: " + id, NotificationType.SUCCESS, 3500L);
         });
      }
   }

   private static boolean ignoredForCapture(String id) {
      return id.contains("entity.player.attack")
         || id.contains(".hurt")
         || id.contains(".death")
         || id.contains("armor.equip")
         || id.contains(".step")
         || id.contains("item.pickup")
         || id.contains("levelup")
         || id.contains("ambient")
         || id.contains("music");
   }

   private boolean recentAttack(long windowMs) {
      return System.currentTimeMillis() - this.lastAttackMs <= windowMs;
   }

   private void forgetCaptured() {
      this.captured.setText("");
      this.captureCounts.clear();
      this.countedThisAttack.clear();
   }

   private SoundEvent selected() {
      return switch (this.sound.get()) {
         case CRIT -> SoundEvents.PLAYER_ATTACK_CRIT;
         case WEAK -> SoundEvents.PLAYER_ATTACK_WEAK;
         case KNOCKBACK -> SoundEvents.PLAYER_ATTACK_KNOCKBACK;
         case SPEAR -> SoundEvents.SPEAR_WOOD_HIT.value();
         case DOOR -> SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;
         case SERVER -> this.capturedSound();
         default -> SoundEvents.PLAYER_ATTACK_STRONG;
      };
   }

   private SoundEvent capturedSound() {
      String text = this.captured.getText().trim();
      Identifier id = text.isEmpty() ? null : Identifier.tryParse(text);
      return id == null ? null : SoundEvent.createVariableRangeEvent(id);
   }

   private void play() {
      SoundEvent event = this.selected();
      if (event == null || mc.getSoundManager() == null) return;

      float p = this.pitch.getValue();
      if (this.randomPitch.isEnabled()) {
         p *= 0.94F + ThreadLocalRandom.current().nextFloat() * 0.12F;
      }

      mc.getSoundManager().play(SimpleSoundInstance.forUI(event, p, this.volume.getValue()));
   }
}
