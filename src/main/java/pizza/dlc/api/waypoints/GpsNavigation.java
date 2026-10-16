package pizza.dlc.api.waypoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

public final class GpsNavigation implements QuickImports {
   private final Path preferences;
   private GpsRoute route;
   private ClientLevel world;
   private int arrivalTicks;
   private boolean voiceEnabled = true;
   private String voice = "alice";
   private SimpleSoundInstance playing;
   private int ticks;
   private int lastVoiceTick = -1000;
   private String candidateCue;
   private String spokenCue;
   private int stableTicks;
   private double spokenX, spokenZ;

   public GpsNavigation() {
      this(PizzaDLC.DIRECTORY.toPath().resolve("gps.json"));
   }

   public GpsNavigation(Path preferences) {
      this.preferences = preferences;
      if (Files.isRegularFile(preferences)) {
         try {
            var data = JsonParser.parseString(Files.readString(preferences)).getAsJsonObject();
            if (data.has("voice")) voiceEnabled = data.get("voice").getAsBoolean();
            if (data.has("voiceName") && supportedVoice(data.get("voiceName").getAsString())) voice = data.get("voiceName").getAsString();
         } catch (Exception e) {
            PizzaDLC.LOGGER.warn("Cannot read GPS preferences", e);
         }
      }
   }

   public boolean start(GpsRoute target) {
      if (mc.player == null || mc.level == null) return false;
      stop();
      route = target;
      world = mc.level;
      play("start");
      lastVoiceTick = ticks;
      return true;
   }

   public void stop() {
      route = null;
      world = null;
      arrivalTicks = 0;
      candidateCue = spokenCue = null;
      stableTicks = 0;
      stopVoice();
   }

   public GpsRoute route() { return route; }
   public boolean voiceEnabled() { return voiceEnabled; }
   public String voice() { return voice; }

   public static boolean supportedVoice(String name) { return "alice".equals(name) || "oksana".equals(name); }

   public boolean setVoice(String name) {
      if (!supportedVoice(name)) return false;
      stopVoice();
      voice = name;
      voiceEnabled = true;
      spokenCue = candidateCue = null;
      stableTicks = 0;
      lastVoiceTick = ticks - 120;
      return savePreferences();
   }

   public boolean setVoiceEnabled(boolean enabled) {
      voiceEnabled = enabled;
      if (!enabled) stopVoice();
      return savePreferences();
   }

   private boolean savePreferences() {
      try {
         Files.createDirectories(preferences.getParent());
         var data = new JsonObject();
         data.addProperty("voice", voiceEnabled);
         data.addProperty("voiceName", voice);
         Files.writeString(preferences, data + "\n");
         return true;
      } catch (Exception e) {
         PizzaDLC.LOGGER.warn("Cannot save GPS preferences", e);
         return false;
      }
   }

   public boolean hasVoicePack() {
      return soundId("straight") != null;
   }

   private Identifier soundId(String cue) {
      // Existing custom GPS packs keep their original generic sound names.
      if (mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("pizzadlc", "sounds/gps/" + cue + ".ogg")).isPresent()) {
         return Identifier.fromNamespaceAndPath("pizzadlc", "gps." + cue);
      }
      if (mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("pizzadlc", "sounds/gps/" + voice + "/" + cue + ".ogg")).isPresent()) {
         return Identifier.fromNamespaceAndPath("pizzadlc", "gps." + voice + "." + cue);
      }
      return null;
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad event) { stop(); }

   @EventHandler
   public void onTick(EventTick event) {
      ticks++;
      if (world == null) return;
      if (mc.player == null || mc.level != world) { stop(); return; }
      if (arrivalTicks > 0) { if (--arrivalTicks == 0) stop(); return; }
      if (route == null) return;
      if (StreamProofRenderer.isVanillaGameplayActive()) { stopVoice(); return; }
      var player = mc.player;
      if (route.arrived(player.getX(), player.getY(), player.getZ())) {
         route = null;
         arrivalTicks = 60;
         play("arrived");
         return;
      }
      String cue = GpsRoute.voiceCue(route.bearing(player.getX(), player.getZ(), cameraYaw()));
      if (!cue.equals(candidateCue)) { candidateCue = cue; stableTicks = 0; }
      stableTicks++;
      boolean moved = spokenCue == null || Math.hypot(player.getX() - spokenX, player.getZ() - spokenZ) >= 3;
      boolean changed = !cue.equals(spokenCue);
      if (voiceEnabled && stableTicks >= 20 && ticks - lastVoiceTick >= (changed ? 120 : 300) && moved) {
         play(cue);
         spokenCue = cue;
         spokenX = player.getX();
         spokenZ = player.getZ();
         lastVoiceTick = ticks;
      }
   }

   private float cameraYaw() {
      return mc.gameRenderer.mainCamera().yRot();
   }

   private void stopVoice() {
      if (playing != null) { mc.getSoundManager().stop(playing); playing = null; }
   }

   private void play(String cue) {
      if (!voiceEnabled || CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return;
      var id = soundId(cue);
      if (id == null) return;
      stopVoice();
      playing = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1.0F, 0.8F);
      mc.getSoundManager().play(playing);
   }

   /** Draw beside the native action-bar message without replacing server messages. */
   public void renderActionBar(GuiGraphicsExtractor ctx, boolean serverMessage) {
      if (mc.player == null || mc.level != world || CleanRenderScope.isActive()
         || StreamProofRenderer.isVanillaGameplayActive() || route == null && arrivalTicks == 0) return;
      var font = PizzaDLCFonts.sfpro();
      String text;
      double bearing = 0;
      if (route == null) {
         text = Translations.tr("gps.arrived");
      } else {
         var player = mc.player;
         bearing = route.bearing(player.getX(), player.getZ(), cameraYaw());
         text = "GPS  ·  " + Math.round(route.distance(player.getX(), player.getY(), player.getZ())) + " " + Translations.tr("gps.meters");
         if (route.y() != null && Math.abs(route.y() - player.getY()) > 3) {
            long height = Math.round(route.y() - player.getY());
            text += "  ·  Y " + (height > 0 ? "+" : "") + height;
         }
      }
      float size = 12.0F;
      float width = Render2DUtil.msdfWidth(font, text, size) + (route != null ? 40 : 20);
      float x = (ctx.guiWidth() - width) / 2;
      float y = ctx.guiHeight() - 78 - (serverMessage ? 24 : 0);
      ctx.nextStratum();
      Render2DUtil.roundedRect(ctx, x, y, width, 22, 6, 0xD91A1B21);
      if (route != null) {
         ctx.pose().pushMatrix();
         ctx.pose().translate(x + 15, y + 11);
         ctx.pose().rotate((float)-Math.toRadians(bearing));
         int color = 0xFFBD8DFF;
         Render2DUtil.line(ctx, 0, 6, 0, -6, 2, color);
         Render2DUtil.line(ctx, 0, -6, -4, -2, 2, color);
         Render2DUtil.line(ctx, 0, -6, 4, -2, 2, color);
         ctx.pose().popMatrix();
      }
      Render2DUtil.msdfText(ctx, font, text, x + (route != null ? 29 : 10), y + 6, size, -1);
   }
}
