package pizza.dlc;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import pizza.dlc.api.config.Config;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePart;
import pizza.dlc.api.emote.MovementAnimation;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;

/** Motion transitions and legacy config migration, without a game window. */
public final class MovementAnimationCheck {
   private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
   private static EmoteEngine.Pose sample(MovementAnimation animation, float partial, float phase) {
      return animation.sample(partial, phase, true, true, true, 1);
   }

   public static void main(String[] args) throws Exception {
      MovementAnimation animation = new MovementAnimation();
      check(sample(animation, 1, 0) == null, "Fresh state must not animate");
      animation.tick(0, true, true, false, 0);
      check(sample(animation, 1, 0) == null, "Standing must preserve vanilla");
      animation.tick(1, true, true, true, 0);
      check(sample(animation, 1, 0) == null, "Sprinting into a wall must not run");
      for (int tick = 2; tick < 8; tick++) animation.tick(tick, true, true, true, .3F);
      var run = sample(animation, 1, 0);
      check(run != null && run.weight() == 1, "Sprint must blend to full motion");
      check(run.parts().get(EmotePart.BODY)[3] > .3F, "Run must lean the torso");
      check(Float.isNaN(run.parts().get(EmotePart.HEAD)[3]) && Float.isNaN(run.parts().get(EmotePart.HEAD)[4]), "Head must retain view direction");
      for (float phase = 0; phase < 100; phase += .25F) {
         var pose = sample(animation, .5F, phase);
         valid(pose);
         float[] body = pose.parts().get(EmotePart.BODY), head = pose.parts().get(EmotePart.HEAD);
         float[] shoulder = pose.parts().get(EmotePart.RIGHT_ARM);
         check(distance(body, head) < .0001F, "Neck must remain attached to the torso");
         check(Math.abs(distance(body, shoulder) - (float)Math.sqrt(29)) < .0001F, "Shoulder must rotate with torso");
      }
      var repeated = sample(animation, 1, 0);
      for (EmotePart part : EmotePart.values()) check(Arrays.equals(run.parts().get(part), repeated.parts().get(part)), "Rendering must not advance playback");
      check(animation.sample(1, 0, false, false, false, 1) == null, "Motion toggles");
      check(Math.abs(animation.sample(1, 0, false, true, false, .5F).weight() - .5F) < .0001F, "Intensity must scale pose blend");
      for (int tick = 8; tick < 15; tick++) animation.tick(tick, true, true, false, 0);
      check(sample(animation, 1, 0) == null, "Stopping must blend back to vanilla");
      animation.tick(15, true, false, false, 0);
      check(sample(animation, 1, 0) == null, "Falling off a ledge must not trigger a jump");
      animation.tick(16, true, true, false, 0);
      check(sample(animation, 1, 0) == null, "A fall must not trigger basketball recovery");
      animation.jumped(17);
      var jumpFrames = new ArrayList<JsonObject>();
      for (int tick = 17; tick <= 36; tick++) {
         animation.tick(tick, true, tick >= 28, false, 0);
         var pose = sample(animation, 1, 0);
         if (tick == 17) check(pose == null, "Takeoff must start with zero weight");
         if (tick == 21) {
            valid(pose);
            check(pose.parts().get(EmotePart.RIGHT_LEG)[3] > .5F, "Jump must tuck feet backwards");
         }
         if (tick == 28) check(pose.weight() > .95F && pose.parts().get(EmotePart.RIGHT_LEG)[3] > .5F, "Landing must retain airborne body pose before recovery");
         if (pose != null) gentleArms(pose, .3F);
         if (tick == 36) check(pose == null, "Landing must finish without a stuck pose");
         jumpFrames.add(frame(tick - 17, pose));
      }
      animation.jumped(37);
      animation.tick(37, false, false, false, 0);
      check(sample(animation, 1, 0) == null, "Swimming, riding or flying must reset animation");
      animation.tick(38, true, true, true, .3F);
      animation.tick(100, true, true, false, 0);
      check(sample(animation, 1, 0) == null, "Tick gaps must discard old motion");
      animation.tick(101, true, true, true, 3);
      check(sample(animation, 1, 0) == null, "Teleport distance must not animate a sprint");
      // A sprint jump restores the overhead reach, except while actively using an item.
      animation.reset();
      for (int tick = 0; tick < 8; tick++) animation.tick(tick, true, true, true, .3F);
      animation.jumped(8, true);
      var sprintJumpFrames = new ArrayList<JsonObject>();
      for (int tick = 8; tick < 28; tick++) {
         animation.tick(tick, true, tick >= 19, false, 0);
         var pose = sample(animation, 1, tick * .45F);
         if (tick == 12) {
            check(pose.parts().get(EmotePart.RIGHT_ARM)[3] < -2.6F && pose.parts().get(EmotePart.LEFT_ARM)[3] < -2.2F,
               "Jumping from a sprint must restore the overhead reach");
            var eating = animation.sample(1, tick * .45F, true, true, true, true, 1);
            itemUseArms(eating);
            check(eating.parts().get(EmotePart.RIGHT_LEG)[3] > .5F, "Eating must retain the body jump pose");
            check(sample(animation, 1, tick * .45F).parts().get(EmotePart.RIGHT_ARM)[3] < -2.6F,
               "Finishing item use must restore the running jump pose");
         }
         if (tick == 19) check(pose.parts().get(EmotePart.RIGHT_ARM)[3] < -2.6F, "Landing must ease out of the sprint jump reach");
         if (tick == 27) check(pose == null, "Sprint jump recovery must finish");
         sprintJumpFrames.add(frame(tick - 8, pose));
      }
      // The next jump from rest must not inherit the preceding sprint jump's reach.
      animation.jumped(28, false);
      for (int tick = 28; tick <= 32; tick++) animation.tick(tick, true, false, false, 0);
      gentleArms(sample(animation, 1, 0), .3F);
      itemUseArms(animation.sample(1, 0, true, true, true, true, 1));
      verifyConfig();
      var runFrames = new ArrayList<JsonObject>();
      animation.reset();
      for (int tick = 0; tick < 8; tick++) animation.tick(tick, true, true, true, .3F);
      for (int tick = 0; tick < 24; tick++) runFrames.add(frame(tick, sample(animation, 1, tick * .45F)));
      JsonObject preview = new JsonObject();
      var gson = new GsonBuilder().setPrettyPrinting().create();
      preview.add("running", gson.toJsonTree(runFrames));
      preview.add("walking", gson.toJsonTree(verifyWalking()));
      preview.add("jump", gson.toJsonTree(jumpFrames));
      preview.add("sprintJump", gson.toJsonTree(sprintJumpFrames));
      Path target = Path.of("build/movement-previews/poses.json");
      Files.createDirectories(target.getParent());
      Files.writeString(target, gson.toJson(preview));
      System.out.println("Movement animations verified: transitions, anchors, jump, toggles and legacy config");
   }

   private static void valid(EmoteEngine.Pose pose) {
      check(pose != null && pose.parts().size() == 6 && pose.weight() > 0 && pose.weight() <= 1, "Invalid pose");
      for (float[] values : pose.parts().values()) for (float v : values) check(!Float.isInfinite(v), "Infinite bone channel");
   }

   private static void gentleArms(EmoteEngine.Pose pose, float limit) {
      check(pose != null, "Jump must still animate the body");
      for (EmotePart arm : new EmotePart[]{EmotePart.RIGHT_ARM, EmotePart.LEFT_ARM}) {
         float[] values = pose.parts().get(arm);
         for (int axis = 3; axis < 6; axis++) check(Float.isFinite(values[axis]) && Math.abs(values[axis] * pose.weight()) < limit,
            "Jump and landing arm sway must stay near the body");
      }
   }

   private static void itemUseArms(EmoteEngine.Pose pose) {
      valid(pose);
      for (EmotePart arm : new EmotePart[]{EmotePart.RIGHT_ARM, EmotePart.LEFT_ARM}) {
         float[] values = pose.parts().get(arm);
         for (int axis = 3; axis < 6; axis++) check(Float.isNaN(values[axis]), "Item use must keep vanilla arm rotations");
      }
   }

   private static ArrayList<JsonObject> verifyWalking() {
      MovementAnimation animation = new MovementAnimation();
      for (int tick = 0; tick < 8; tick++) animation.tick(tick, true, true, false, .215F);
      var walk = sample(animation, 1, 0);
      valid(walk);
      check(walk.weight() == 1 && walk.parts().get(EmotePart.BODY)[3] > 0 && walk.parts().get(EmotePart.BODY)[3] < .1F, "Walk must have a gentle torso lean");
      check(Math.abs(walk.parts().get(EmotePart.RIGHT_ARM)[3]) < .5F, "Walk arms must swing less than sprint arms");
      check(walk.parts().get(EmotePart.RIGHT_LEG)[3] > 0 && walk.parts().get(EmotePart.LEFT_LEG)[3] < 0, "Walking steps must alternate");
      check(animation.sample(1, 0, false, true, false, 1) == null, "Walking toggle must preserve vanilla walking");
      var frames = new ArrayList<JsonObject>();
      for (int tick = 0; tick < 32; tick++) frames.add(frame(tick, sample(animation, 1, tick * .3F)));
      float previousLean = walk.parts().get(EmotePart.BODY)[3] * walk.weight();
      for (int tick = 8; tick < 16; tick++) {
         animation.tick(tick, true, true, true, .3F);
         var pose = sample(animation, 1, 0);
         valid(pose);
         float lean = pose.parts().get(EmotePart.BODY)[3] * pose.weight();
         check(Math.abs(lean - previousLean) < .1F, "Walk-to-sprint transition must blend");
         previousLean = lean;
      }
      for (int tick = 16; tick < 24; tick++) animation.tick(tick, true, true, false, .215F);
      check(sample(animation, 1, 0).parts().get(EmotePart.BODY)[3] < .1F, "Sprint must return to walking");
      for (int tick = 24; tick < 32; tick++) animation.tick(tick, true, true, false, .002F);
      check(sample(animation, 1, 0) == null, "Walking must stop when standing still");
      animation.tick(32, true, true, false, .07F);
      var slow = sample(animation, 1, 0);
      check(slow.weight() < walk.weight(), "Slow walking must reduce motion");
      return frames;
   }

   private static float distance(float[] a, float[] b) {
      return (float)Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
   }

   private static void verifyConfig() {
      AnimatedHands module = new AnimatedHands();
      check(module.getName().equals("Animations"), "Module must be renamed");
      JsonObject old = module.save();
      old.getAsJsonObject("Settings").addProperty("Смещение X", .37F);
      JsonObject modules = new JsonObject();
      modules.add("AnimatedHands", old);
      module.load(Config.moduleState(modules, module.getName()));
      SliderSetting offset = (SliderSetting)module.getSettings().stream().filter(s -> s.getName().equals("Смещение X")).findFirst().orElseThrow();
      check(Math.abs(offset.getValue() - .37F) < .0001F, "Old hand offsets must migrate");
      JsonObject current = module.save();
      modules.add("Animations", current);
      check(Config.moduleState(modules, module.getName()) == current, "New config must take priority");
      check(Config.moduleState(modules, "Missing") == null, "Absent modules must stay absent");
   }

   private static JsonObject frame(int tick, EmoteEngine.Pose pose) {
      float[][] neutral = {{0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0}, {-5, 2, 0, 0, 0, 0}, {5, 2, 0, 0, 0, 0}, {-1.9F, 12, 0, 0, 0, 0}, {1.9F, 12, 0, 0, 0, 0}};
      EmotePart[] order = {EmotePart.HEAD, EmotePart.BODY, EmotePart.RIGHT_ARM, EmotePart.LEFT_ARM, EmotePart.RIGHT_LEG, EmotePart.LEFT_LEG};
      String[] names = {"head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"};
      String[] axes = {"x", "y", "z", "pitch", "yaw", "roll"};
      JsonObject frame = new JsonObject(); frame.addProperty("tick", tick);
      for (int i = 0; i < order.length; i++) {
         float[] values = pose == null ? null : pose.parts().get(order[i]);
         JsonObject bone = new JsonObject();
         for (int axis = 0; axis < 6; axis++) {
            float value = neutral[i][axis];
            if (values != null && !Float.isNaN(values[axis])) value += (values[axis] - value) * pose.weight();
            bone.addProperty(axes[axis], axis < 3 ? value : Math.toDegrees(value));
         }
         frame.add(names[i], bone);
      }
      return frame;
   }
}
