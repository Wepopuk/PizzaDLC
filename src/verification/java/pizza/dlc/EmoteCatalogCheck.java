package pizza.dlc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import pizza.dlc.api.emote.Axis;
import pizza.dlc.api.emote.EmoteAnimation;
import pizza.dlc.api.emote.EmoteDefinition;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePart;
import pizza.dlc.api.emote.EmoteRegistry;
import pizza.dlc.api.emote.EmoteWheel;

/** Resource loading, loop continuity and selection across every wheel page. */
public final class EmoteCatalogCheck {
   private static final Set<String> NEW_DANCES = Set.of(
      "rampage_2", "rampage_3", "rampage_4", "sturdy_drill", "aus_shuffle",
      "dnb_step", "jumpstyle", "hopak", "slip_back", "larp333", "larp333_2"
   );

   private static void check(boolean ok, String message) {
      if (!ok) throw new AssertionError(message);
   }

   public static void main(String[] args) throws Exception {
      Set<String> ids = new HashSet<>();
      for (EmoteDefinition definition : EmoteRegistry.ALL) {
         String id = definition.id();
         check(ids.add(id), "Duplicate emote: " + id);
         check(EmoteRegistry.byId(id) == definition, "Registry lookup: " + id);
         try (var stream = EmoteCatalogCheck.class.getResourceAsStream("/assets/pizzadlc/textures/emotes/" + id + ".png")) {
            check(stream != null && ImageIO.read(stream) != null, "Missing or invalid icon: " + id);
         }
         EmoteAnimation animation = definition.animation();
         check(!animation.sample(0).isEmpty(), "Animation failed to load: " + id);
         for (float tick = 0; tick <= animation.durationTicks(); tick += .5F) {
            Map<EmotePart, float[]> sampled = animation.sample(tick);
            if (NEW_DANCES.contains(id) || id.equals("ziga")) check(sampled.size() == 6, "Incomplete pose: " + id);
            for (float[] values : sampled.values()) {
               for (float value : values) {
                  check(!Float.isInfinite(value), "Infinite channel: " + id);
                  if (NEW_DANCES.contains(id) || id.equals("ziga")) check(Float.isFinite(value), "Undriven channel: " + id);
               }
            }
         }
         if (NEW_DANCES.contains(id)) {
            check(animation.loop(), "New dance must loop: " + id);
            var first = animation.sample(animation.beginTick());
            var last = animation.sample(animation.endTick());
            for (EmotePart part : EmotePart.values()) {
               for (int axis = 0; axis < first.get(part).length; axis++) {
                  check(Math.abs(first.get(part)[axis] - last.get(part)[axis]) < .0001F, "Loop seam: " + id);
               }
            }
         }
      }
      check(ids.containsAll(NEW_DANCES), "New dances absent from registry");
      verifyEmoteSkin();
      check(EmoteWheel.entry(-1) == null && EmoteWheel.entry(EmoteRegistry.ALL.size()) == null, "Invalid wheel index");
      check(!EmoteWheel.onScroll(-1), "Closed wheel must not consume scroll");
      int pages = (EmoteRegistry.ALL.size() + 11) / 12;
      for (int page = 0; page < pages; page++) {
         int count = Math.min(12, EmoteRegistry.ALL.size() - page * 12);
         for (int slot = 0; slot < count; slot++) {
            EmoteWheel.open();
            double angle = -Math.PI / 2 + slot * Math.PI * 2 / count;
            EmoteWheel.onMouseTurn(Math.cos(angle) * 20, Math.sin(angle) * 20);
            int selected = EmoteWheel.close();
            check(selected == page * 12 + slot, "Wrong selection on page " + page + ", slot " + slot);
            check(EmoteWheel.entry(selected) == EmoteRegistry.ALL.get(selected), "Selection changed after closing");
         }
         EmoteWheel.open();
         for (int i = 0; i < 4; i++) EmoteWheel.onScroll(-.25);
         check(EmoteWheel.close() == EmoteWheel.NONE, "Page change at centre must not select");
      }
      // Forward wrap, backward wrap and reopening after a partial trackpad scroll.
      EmoteWheel.open();
      EmoteWheel.onMouseTurn(0, -20);
      check(EmoteWheel.close() == 0, "Forward page wrap");
      EmoteWheel.open();
      EmoteWheel.onScroll(1);
      EmoteWheel.onMouseTurn(0, -20);
      check(EmoteWheel.close() == (pages - 1) * 12, "Backward page wrap");
      EmoteWheel.open();
      EmoteWheel.onScroll(-.5);
      EmoteWheel.cancel();
      check(EmoteWheel.close() == EmoteWheel.CANCELLED, "Cancelled wheel must not pick");
      EmoteWheel.open();
      EmoteWheel.onScroll(-.5);
      EmoteWheel.onMouseTurn(0, -20);
      check(EmoteWheel.close() == (pages - 1) * 12, "Partial scroll leaked across opens");
      System.out.println("PASS: " + ids.size() + " emotes, icons, finite poses, seamless loops, paged selection, trackpad input and temporary skin lifecycle");
   }

   private static void verifyEmoteSkin() throws Exception {
      EmoteDefinition ziga = EmoteRegistry.byId("ziga");
      check(ziga != null && ziga.skinOverride() != null, "Gesture skin missing");
      PlayerSkin skin = ziga.skinOverride();
      check(skin.model() == PlayerModelType.WIDE, "Provided skin needs wide arms");
      check(skin.body().texturePath().toString().equals("pizzadlc:textures/emotes/skins/ziga.png"), "Skin texture path");
      try (var stream = EmoteCatalogCheck.class.getResourceAsStream("/assets/pizzadlc/" + skin.body().texturePath().getPath())) {
         check(stream != null, "Skin is not bundled");
         var image = ImageIO.read(stream);
         check(image != null && image.getWidth() == 64 && image.getHeight() == 64, "Invalid skin dimensions");
      }
      for (EmoteDefinition definition : EmoteRegistry.ALL) {
         if (definition != ziga) check(definition.skinOverride() == null, "Skin override leaked to " + definition.id());
      }
      EmoteAnimation animation = ziga.animation();
      check(!animation.loop(), "Gesture must finish automatically");
      int pitch = Axis.ROT_PITCH.ordinal();
      check(animation.sample(12).get(EmotePart.RIGHT_ARM)[pitch] < -2, "Gesture arm did not rise");
      check(Math.abs(animation.sample(animation.stopTick()).get(EmotePart.RIGHT_ARM)[pitch]) < .0001F, "Gesture arm did not return");

      PlayerSkin original = PlayerSkin.insecure(
         new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("pizzadlc", "verification/original")),
         null, null, PlayerModelType.SLIM
      );
      UUID player = UUID.randomUUID();
      UUID otherPlayer = UUID.randomUUID();
      EmoteEngine engine = EmoteEngine.INSTANCE;
      engine.stopAll();
      check(engine.skinFor(player, original) == original, "Idle skin changed");
      check(engine.skinFor(null, original) == original, "Null player skin changed");
      engine.play(player, ziga);
      check(engine.skinFor(player, original) == skin, "Gesture skin not applied");
      check(engine.skinFor(otherPlayer, original) == original, "Other player's skin changed");
      engine.stop(player);
      check(engine.skinFor(player, original) == original, "Cancelled gesture retained skin");
      engine.play(player, ziga);
      engine.play(player, EmoteRegistry.byId("rampage"));
      check(engine.skinFor(player, original) == original, "Switching emotes retained skin");
      engine.play(player, ziga);
      engine.stopAll();
      check(engine.skinFor(player, original) == original, "Disconnect cleanup retained skin");
      engine.play(player, ziga);
      // Verify timeout restoration even without poseFor (first-person rendering).
      Thread.sleep((long)Math.ceil(animation.durationTicks() * 50) + 50);
      check(engine.skinFor(player, original) == original, "Completed gesture retained skin");
      check(engine.current(player) == null, "Completed gesture was not expired");
   }
}
