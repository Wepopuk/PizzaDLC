package pizza.dlc.api.emote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.LoggerFactory;

// kosmx PlayerAnimator JSON: a flat "moves" list, each a tick + partial per-part update ({"rightArm": {"yaw": 90}}) and the easing into it.
final class EmoteJsonLoader {
   private EmoteJsonLoader() {
   }

   static EmoteAnimation load(String id, String displayName, String resourcePath) {
      try (InputStream stream = EmoteJsonLoader.class.getResourceAsStream(resourcePath)) {
         if (stream == null) {
            throw new IOException("Missing bundled resource: " + resourcePath);
         }

         JsonObject root;
         try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
         }

         JsonObject emote = root.getAsJsonObject("emote");
         boolean degrees = getBoolean(emote, "degrees", true);
         boolean loop = getBoolean(emote, "isLoop", false);
         float beginTick = getFloat(emote, "beginTick", 0.0F);
         float endTick = getFloat(emote, "endTick", beginTick);
         float stopTick = getFloat(emote, "stopTick", endTick);
         EmoteAnimation.Builder builder = new EmoteAnimation.Builder();
         JsonElement movesElement = emote.get("moves");
         if (movesElement != null && movesElement.isJsonArray()) {
            for (JsonElement moveElement : movesElement.getAsJsonArray()) {
               if (!moveElement.isJsonObject()) continue;

               JsonObject move = moveElement.getAsJsonObject();
               float tick = getFloat(move, "tick", 0.0F);
               Easing easing = Easing.parse(getString(move, "easing", "linear"));

               for (Map.Entry<String, JsonElement> entry : move.entrySet()) {
                  if (entry.getKey().equals("tick") || entry.getKey().equals("easing")) continue;

                  EmotePart part = EmotePart.fromJsonKey(entry.getKey());
                  if (part == null || !entry.getValue().isJsonObject()) continue;

                  for (Map.Entry<String, JsonElement> axisEntry : entry.getValue().getAsJsonObject().entrySet()) {
                     Axis axis = Axis.fromJsonKey(axisEntry.getKey());
                     if (axis == null || !axisEntry.getValue().isJsonPrimitive()) continue;

                     float raw = axisEntry.getValue().getAsFloat();
                     float value = axis.isRotation() && degrees ? (float)Math.toRadians(raw) : raw;
                     builder.put(part, axis, tick, value, easing);
                  }
               }
            }
         }

         return new EmoteAnimation(id, displayName, beginTick, endTick, stopTick, loop, builder.build());
      } catch (Exception exception) {
         LoggerFactory.getLogger(EmoteJsonLoader.class).error("Failed to load bundled emote '{}' from {}", id, resourcePath, exception);
         return new EmoteAnimation(id, displayName, 0.0F, 1.0F, 1.0F, false, Map.of());
      }
   }

   private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
      return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : fallback;
   }

   private static float getFloat(JsonObject object, String key, float fallback) {
      return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsFloat() : fallback;
   }

   private static String getString(JsonObject object, String key, String fallback) {
      return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
   }
}
