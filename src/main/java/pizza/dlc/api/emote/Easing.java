package pizza.dlc.api.emote;

import java.util.Locale;
import java.util.function.Function;

// Penner curves under the names used by the bundled kosmx PlayerAnimator files ("InOutSine", "EASEOUTQUART", "outExpo", ...).
public enum Easing {
   LINEAR(t -> t),
   CONSTANT(t -> 0.0F),
   EASE_IN_QUAD(t -> t * t),
   EASE_OUT_QUAD(t -> 1.0F - (1.0F - t) * (1.0F - t)),
   EASE_IN_OUT_QUAD(t -> t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 2.0) / 2.0F),
   EASE_IN_CUBIC(t -> t * t * t),
   EASE_OUT_CUBIC(t -> 1.0F - (float)Math.pow(1.0F - t, 3.0)),
   EASE_IN_OUT_CUBIC(t -> t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F),
   EASE_IN_SINE(t -> 1.0F - (float)Math.cos(t * Math.PI / 2.0)),
   EASE_OUT_SINE(t -> (float)Math.sin(t * Math.PI / 2.0)),
   EASE_IN_OUT_SINE(t -> -((float)Math.cos(Math.PI * t) - 1.0F) / 2.0F),
   EASE_IN_QUART(t -> t * t * t * t),
   EASE_OUT_QUART(t -> 1.0F - (float)Math.pow(1.0F - t, 4.0)),
   EASE_IN_OUT_QUART(t -> t < 0.5F ? 8.0F * t * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 4.0) / 2.0F),
   EASE_IN_EXPO(t -> t <= 0.0F ? 0.0F : (float)Math.pow(2.0, 10.0 * t - 10.0)),
   EASE_OUT_EXPO(t -> t >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * t)),
   EASE_IN_OUT_EXPO(t -> {
      if (t <= 0.0F) return 0.0F;
      if (t >= 1.0F) return 1.0F;
      return t < 0.5F ? (float)Math.pow(2.0, 20.0 * t - 10.0) / 2.0F : (2.0F - (float)Math.pow(2.0, -20.0 * t + 10.0)) / 2.0F;
   }),
   EASE_IN_CIRC(t -> 1.0F - (float)Math.sqrt(1.0 - Math.pow(t, 2.0))),
   EASE_OUT_CIRC(t -> (float)Math.sqrt(1.0 - Math.pow(t - 1.0, 2.0))),
   EASE_IN_OUT_CIRC(
      t -> t < 0.5F
         ? (1.0F - (float)Math.sqrt(1.0 - Math.pow(2.0 * t, 2.0))) / 2.0F
         : ((float)Math.sqrt(1.0 - Math.pow(-2.0 * t + 2.0, 2.0)) + 1.0F) / 2.0F
   );

   private final Function<Float, Float> curve;

   Easing(Function<Float, Float> curve) {
      this.curve = curve;
   }

   public float apply(float t) {
      float clamped = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
      return this.curve.apply(clamped);
   }

   public static Easing parse(String raw) {
      if (raw == null || raw.isBlank()) {
         return LINEAR;
      }

      String normalized = raw.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
      return switch (normalized) {
         case "linear" -> LINEAR;
         case "constant", "step" -> CONSTANT;
         case "inquad", "easeinquad" -> EASE_IN_QUAD;
         case "outquad", "easeoutquad" -> EASE_OUT_QUAD;
         case "inoutquad", "easeinoutquad" -> EASE_IN_OUT_QUAD;
         case "incubic", "easeincubic" -> EASE_IN_CUBIC;
         case "outcubic", "easeoutcubic" -> EASE_OUT_CUBIC;
         case "inoutcubic", "easeinoutcubic" -> EASE_IN_OUT_CUBIC;
         case "insine", "easeinsine" -> EASE_IN_SINE;
         case "outsine", "easeoutsine" -> EASE_OUT_SINE;
         case "inoutsine", "easeinoutsine" -> EASE_IN_OUT_SINE;
         case "inquart", "easeinquart" -> EASE_IN_QUART;
         case "outquart", "easeoutquart" -> EASE_OUT_QUART;
         case "inoutquart", "easeinoutquart" -> EASE_IN_OUT_QUART;
         case "inexpo", "easeinexpo" -> EASE_IN_EXPO;
         case "outexpo", "easeoutexpo" -> EASE_OUT_EXPO;
         case "inoutexpo", "easeinoutexpo" -> EASE_IN_OUT_EXPO;
         case "incirc", "easeincirc" -> EASE_IN_CIRC;
         case "outcirc", "easeoutcirc" -> EASE_OUT_CIRC;
         case "inoutcirc", "easeinoutcirc" -> EASE_IN_OUT_CIRC;
         default -> LINEAR;
      };
   }
}
