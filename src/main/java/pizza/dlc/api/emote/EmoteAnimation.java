package pizza.dlc.api.emote;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

// Per (part, axis) sparse curves; each channel is interpolated on its own, so a keyframe touching one axis leaves the others alone.
public final class EmoteAnimation {
   private final String id;
   private final String displayName;
   private final float beginTick;
   private final float endTick;
   private final float stopTick;
   private final boolean loop;
   private final Map<EmotePart, Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>>> channels;

   EmoteAnimation(
      String id, String displayName, float beginTick, float endTick, float stopTick, boolean loop,
      Map<EmotePart, Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>>> channels
   ) {
      this.id = id;
      this.displayName = displayName;
      this.beginTick = beginTick;
      this.endTick = Math.max(endTick, beginTick + 1.0F);
      this.stopTick = Math.max(stopTick, this.endTick);
      this.loop = loop;
      this.channels = channels;
   }

   public String id() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public float beginTick() {
      return this.beginTick;
   }

   public float endTick() {
      return this.endTick;
   }

   public float stopTick() {
      return this.stopTick;
   }

   public boolean loop() {
      return this.loop;
   }

   public float durationTicks() {
      return this.stopTick;
   }

   // NaN in a slot means "this animation never drives that axis", keep the vanilla pose there.
   public EnumMap<EmotePart, float[]> sample(float tick) {
      EnumMap<EmotePart, float[]> result = new EnumMap<>(EmotePart.class);

      for (Map.Entry<EmotePart, Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>>> partEntry : this.channels.entrySet()) {
         float[] values = new float[Axis.values().length];
         Arrays.fill(values, Float.NaN);
         boolean any = false;

         for (Map.Entry<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>> axisEntry : partEntry.getValue().entrySet()) {
            float sampled = sampleChannel(axisEntry.getValue(), tick);
            if (!Float.isNaN(sampled)) {
               values[axisEntry.getKey().ordinal()] = sampled;
               any = true;
            }
         }

         if (any) {
            result.put(partEntry.getKey(), values);
         }
      }

      return result;
   }

   private static float sampleChannel(NavigableMap<Float, EmoteAnimation.ChannelPoint> channel, float tick) {
      if (channel.isEmpty()) {
         return Float.NaN;
      }

      Map.Entry<Float, EmoteAnimation.ChannelPoint> floor = channel.floorEntry(tick);
      Map.Entry<Float, EmoteAnimation.ChannelPoint> ceiling = channel.ceilingEntry(tick);
      if (floor == null) {
         return ceiling.getValue().value();
      }

      if (ceiling == null || ceiling.getKey().equals(floor.getKey())) {
         return floor.getValue().value();
      }

      EmoteAnimation.ChannelPoint from = floor.getValue();
      EmoteAnimation.ChannelPoint to = ceiling.getValue();
      float span = to.tick() - from.tick();
      if (span <= 1.0E-4F) {
         return to.value();
      }

      float progress = (tick - from.tick()) / span;
      float eased = to.easingIn().apply(progress);
      return from.value() + (to.value() - from.value()) * eased;
   }

   public record ChannelPoint(float tick, float value, Easing easingIn) {
   }

   static final class Builder {
      private final Map<EmotePart, Map<Axis, TreeMap<Float, EmoteAnimation.ChannelPoint>>> channels = new EnumMap<>(EmotePart.class);

      void put(EmotePart part, Axis axis, float tick, float value, Easing easingIn) {
         this.channels
            .computeIfAbsent(part, ignored -> new EnumMap<>(Axis.class))
            .computeIfAbsent(axis, ignored -> new TreeMap<>())
            .put(tick, new EmoteAnimation.ChannelPoint(tick, value, easingIn));
      }

      Map<EmotePart, Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>>> build() {
         Map<EmotePart, Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>>> result = new EnumMap<>(EmotePart.class);

         for (Map.Entry<EmotePart, Map<Axis, TreeMap<Float, EmoteAnimation.ChannelPoint>>> partEntry : this.channels.entrySet()) {
            Map<Axis, NavigableMap<Float, EmoteAnimation.ChannelPoint>> axes = new EnumMap<>(Axis.class);
            axes.putAll(partEntry.getValue());
            result.put(partEntry.getKey(), axes);
         }

         return result;
      }
   }
}
