package pizza.dlc.api.emote;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.PlayerSkin;

// Plays emotes on wall-clock time at the animations' native 20 tps, so playback doesn't depend on frame rate.
public final class EmoteEngine {
   public static final EmoteEngine INSTANCE = new EmoteEngine();
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float BLEND_IN = 0.15F;
   private static final float BLEND_OUT = 0.2F;
   private final Map<UUID, EmoteEngine.Playback> active = new ConcurrentHashMap<>();

   private EmoteEngine() {
   }

   public void play(UUID entity, EmoteDefinition definition) {
      if (entity != null && definition != null) {
         this.active.put(entity, new EmoteEngine.Playback(definition, System.nanoTime(), 0L));
      }
   }

   // Doesn't cut the pose off: the emote keeps running while it fades back into the vanilla pose.
   public void stop(UUID entity) {
      EmoteEngine.Playback playback = entity == null ? null : this.active.get(entity);
      if (playback != null && playback.stopNanos() == 0L) {
         this.active.replace(entity, playback, new EmoteEngine.Playback(playback.definition(), playback.startNanos(), System.nanoTime()));
      }
   }

   public void stopAll() {
      this.active.clear();
   }

   // Also expires one-shots by itself: in first person the local player is never extracted, so poseFor() isn't there to do it.
   public boolean isPlaying(UUID entity) {
      if (entity == null) {
         return false;
      }

      EmoteEngine.Playback playback = this.active.get(entity);
      if (playback == null || playback.stopNanos() != 0L) {
         return false;
      }

      EmoteAnimation animation = playback.definition().animation();
      if (!animation.loop() && animation.beginTick() + elapsedTicks(playback) >= animation.stopTick()) {
         this.active.remove(entity, playback);
         return false;
      }

      return true;
   }

   public EmoteDefinition current(UUID entity) {
      EmoteEngine.Playback playback = entity == null ? null : this.active.get(entity);
      return playback == null || playback.stopNanos() != 0L ? null : playback.definition();
   }

   // Resolve on every read rather than mutating PlayerInfo or the account skin.
   // isPlaying also expires completed one-shots when no player pose is rendered.
   public PlayerSkin skinOverrideFor(UUID entity) {
      EmoteDefinition definition = this.current(entity);
      return definition != null && this.isPlaying(entity) ? definition.skinOverride() : null;
   }

   public PlayerSkin skinFor(UUID entity, PlayerSkin original) {
      PlayerSkin override = this.skinOverrideFor(entity);
      return override == null ? original : override;
   }

   public EmoteEngine.Pose poseFor(UUID entity) {
      if (entity == null) {
         return null;
      }

      EmoteEngine.Playback playback = this.active.get(entity);
      if (playback == null) {
         return null;
      }

      long now = System.nanoTime();
      float weight = Math.min(1.0F, (now - playback.startNanos()) / 1.0E9F / BLEND_IN);
      if (playback.stopNanos() != 0L) {
         float out = 1.0F - (now - playback.stopNanos()) / 1.0E9F / BLEND_OUT;
         if (out <= 0.0F) {
            this.active.remove(entity, playback);
            return null;
         }

         weight = Math.min(weight, out);
      }

      EmoteAnimation animation = playback.definition().animation();
      float tick = animation.beginTick() + elapsedTicks(playback);
      EnumMap<EmotePart, float[]> parts;
      if (tick < animation.stopTick()) {
         parts = animation.sample(tick);
      } else if (animation.loop() && animation.endTick() > animation.beginTick()) {
         float span = animation.endTick() - animation.beginTick();
         parts = animation.sample(animation.beginTick() + (tick - animation.beginTick()) % span);
      } else {
         this.active.remove(entity, playback);
         return null;
      }

      weight = Math.max(0.0F, weight);
      return new EmoteEngine.Pose(parts, weight * weight * (3.0F - 2.0F * weight));
   }

   private static float elapsedTicks(EmoteEngine.Playback playback) {
      return (System.nanoTime() - playback.startNanos()) / 1.0E9F * TICKS_PER_SECOND;
   }

   // weight 1 = pure emote, 0 = pure vanilla pose.
   public record Pose(EnumMap<EmotePart, float[]> parts, float weight) {
   }

   private record Playback(EmoteDefinition definition, long startNanos, long stopNanos) {
   }
}
