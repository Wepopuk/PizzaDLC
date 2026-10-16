package pizza.dlc.api.emote;

import java.util.Locale;

// kosmx PlayerAnimator bone names; "torso" is an older alias of "body".
public enum EmotePart {
   HEAD,
   BODY,
   RIGHT_ARM,
   LEFT_ARM,
   RIGHT_LEG,
   LEFT_LEG;

   public static EmotePart fromJsonKey(String key) {
      if (key == null) {
         return null;
      }

      return switch (key.toLowerCase(Locale.ROOT)) {
         case "head" -> HEAD;
         case "torso", "body" -> BODY;
         case "rightarm" -> RIGHT_ARM;
         case "leftarm" -> LEFT_ARM;
         case "rightleg" -> RIGHT_LEG;
         case "leftleg" -> LEFT_LEG;
         default -> null;
      };
   }
}
