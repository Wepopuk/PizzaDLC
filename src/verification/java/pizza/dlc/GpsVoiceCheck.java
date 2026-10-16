package pizza.dlc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.client.sounds.JOrbisAudioStream;

/** Decode every bundled GPS phrase with Minecraft's own Vorbis reader. */
public final class GpsVoiceCheck {
   private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static JsonObject json(String path) throws Exception {
      try (var stream = GpsVoiceCheck.class.getResourceAsStream(path)) {
         check(stream != null, "Missing " + path);
         return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }
   public static void main(String[] args) throws Exception {
      var sounds = json("/assets/pizzadlc/sounds.json");
      var source = json("/assets/pizzadlc/sounds/gps/source.json");
      check(source.get("source").getAsString().equals("yandex_navigator_all_voices_250421.scs"), "Wrong voice archive");
      for (String voice : new String[]{"alice", "oksana"}) {
         var clips = source.getAsJsonObject("voices").getAsJsonObject(voice).getAsJsonObject("clips");
         check(clips.getAsJsonObject("left").get("sample").getAsString().equals("turn_left"), "Left phrase mapping incorrect");
         check(clips.getAsJsonObject("right").get("sample").getAsString().equals("turn_right"), "Right phrase mapping incorrect");
         for (String cue : new String[]{"start", "straight", "left", "right", "uturn", "arrived"}) {
            var entry = sounds.getAsJsonObject("gps." + voice + "." + cue).getAsJsonArray("sounds").get(0).getAsJsonObject();
            check(entry.get("name").getAsString().equals("pizzadlc:gps/" + voice + "/" + cue), "Incorrect sound event file");
            check(entry.get("stream").getAsBoolean(), "Voice clip is not streamed");
            String path = "/assets/pizzadlc/sounds/gps/" + voice + "/" + cue + ".ogg";
            byte[] data;
            try (var input = GpsVoiceCheck.class.getResourceAsStream(path)) {
               check(input != null, "Missing " + path); data = input.readAllBytes();
            }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
            check(hash.equals(clips.getAsJsonObject(cue).get("sha256").getAsString()), "Voice clip differs from imported archive manifest");
            try (var decoder = new JOrbisAudioStream(new java.io.ByteArrayInputStream(data))) {
               check(decoder.getFormat().getChannels() == 1 && decoder.getFormat().getSampleRate() == 48000, "Unexpected voice format");
               int[] samples = {0}; double[] energy = {0};
               while (decoder.readChunk(value -> {
                  check(Float.isFinite(value), "Corrupt decoded audio");
                  samples[0]++; energy[0] += value * value;
               })) { }
               double seconds = samples[0] / 48000.0;
               check(seconds > 0.2 && seconds < 10, "Empty or oversized GPS phrase");
               check(energy[0] / samples[0] > 1.0E-6, "GPS phrase is silent");
            }
         }
      }
      System.out.println("GPS voices: all 12 Alice/Oksana phrases decoded by Minecraft; sound events, source hashes, direction mappings and non-silent PCM passed.");
   }
}
