package pizza.dlc.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import pizza.dlc.PizzaDLC;

public final class PizzaDLCSounds {
   public static final Identifier TOGGLE_ID = PizzaDLC.id("toggle");
   public static final SoundEvent TOGGLE = SoundEvent.createVariableRangeEvent(TOGGLE_ID);
   public static final Identifier CHAOS_ID = PizzaDLC.id("chaos");
   public static final SoundEvent CHAOS = SoundEvent.createVariableRangeEvent(CHAOS_ID);
   public static final Identifier PEANUT_ID = PizzaDLC.id("peanut");
   public static final SoundEvent PEANUT = SoundEvent.createVariableRangeEvent(PEANUT_ID);
   public static final Identifier S9_ID = PizzaDLC.id("s9_notification");
   public static final SoundEvent S9 = SoundEvent.createVariableRangeEvent(S9_ID);
   public static final Identifier TONOS_ID = PizzaDLC.id("tonos");
   public static final SoundEvent TONOS = SoundEvent.createVariableRangeEvent(TONOS_ID);
   public static final Identifier HAZE_ID = PizzaDLC.id("haze_notification");
   public static final SoundEvent HAZE = SoundEvent.createVariableRangeEvent(HAZE_ID);
   public static final Identifier HINT_ID = PizzaDLC.id("hint_notification");
   public static final SoundEvent HINT = SoundEvent.createVariableRangeEvent(HINT_ID);

   private PizzaDLCSounds() {
   }

   public static void init() {
      Registry.register(BuiltInRegistries.SOUND_EVENT, TOGGLE_ID, TOGGLE);
      Registry.register(BuiltInRegistries.SOUND_EVENT, CHAOS_ID, CHAOS);
      Registry.register(BuiltInRegistries.SOUND_EVENT, PEANUT_ID, PEANUT);
      Registry.register(BuiltInRegistries.SOUND_EVENT, S9_ID, S9);
      Registry.register(BuiltInRegistries.SOUND_EVENT, TONOS_ID, TONOS);
      Registry.register(BuiltInRegistries.SOUND_EVENT, HAZE_ID, HAZE);
      Registry.register(BuiltInRegistries.SOUND_EVENT, HINT_ID, HINT);
   }
}
