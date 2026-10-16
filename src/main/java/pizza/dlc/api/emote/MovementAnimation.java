package pizza.dlc.api.emote;

import java.util.Arrays;
import java.util.EnumMap;

/** Tick-driven visual poses. Sampling never advances playback or changes player physics. */
public final class MovementAnimation {
   private int tick = Integer.MIN_VALUE;
   private float run, previousRun;
   private float walk, previousWalk;
   private double jumpTick = Double.NaN, landingTick = Double.NaN;
   private float landingAir;
   private float landingJumpAge;
   private boolean grounded;
   private boolean sprintJump;

   public void reset() {
      tick = Integer.MIN_VALUE;
      run = previousRun = 0;
      walk = previousWalk = 0;
      jumpTick = landingTick = Double.NaN;
      landingAir = 0;
      landingJumpAge = 0;
      grounded = false;
      sprintJump = false;
   }

   public void jumped(int atTick) {
      jumped(atTick, false);
   }

   public void jumped(int atTick, boolean fromSprint) {
      jumpTick = atTick;
      landingTick = Double.NaN;
      sprintJump = fromSprint;
   }

   public void tick(int atTick, boolean allowed, boolean onGround, boolean sprinting, float speed) {
      if (!allowed || !Float.isFinite(speed) || speed > 1.5F) { reset(); return; }
      if (tick == atTick) return;
      if (tick != Integer.MIN_VALUE && (atTick < tick || atTick - tick > 2)) reset();
      previousRun = run;
      previousWalk = walk;
      float target = sprinting && onGround ? clamp((speed - .015F) / .18F, 0, 1) : 0;
      float walkTarget = !sprinting && onGround ? clamp((speed - .015F) / .19F, 0, 1) : 0;
      run += clamp(target - run, -.2F, .2F);
      walk += clamp(walkTarget - walk, -.15F, .15F);
      if (run < .0001F) run = 0;
      if (walk < .0001F) walk = 0;
      // Only an actual jump can produce the basketball pose and its landing recovery.
      if (onGround && !grounded && !Double.isNaN(jumpTick) && atTick > jumpTick + 1) {
         landingTick = atTick;
         landingAir = 1 - smooth(((float)(atTick - jumpTick) - 12) / 7);
         landingJumpAge = (float)(atTick - jumpTick);
         jumpTick = Double.NaN;
      }
      grounded = onGround;
      tick = atTick;
   }

   public EmoteEngine.Pose sample(float partialTick, float walkPosition, boolean walking, boolean running, boolean jumping, float strength) {
      return sample(partialTick, walkPosition, walking, running, jumping, false, strength);
   }

   public EmoteEngine.Pose sample(float partialTick, float walkPosition, boolean walking, boolean running, boolean jumping,
      boolean usingItem, float strength) {
      if (tick == Integer.MIN_VALUE || !Float.isFinite(strength) || !Float.isFinite(partialTick)
         || !Float.isFinite(walkPosition)) return null;
      float partial = clamp(partialTick, 0, 1);
      double now = tick - 1 + partial;
      float runWeight = running ? lerp(previousRun, run, partial) : 0;
      float walkWeight = walking ? lerp(previousWalk, walk, partial) : 0;
      float strideWeight = clamp(runWeight + walkWeight, 0, 1);
      float sprintMix = strideWeight > 0 ? runWeight / (runWeight + walkWeight) : 0;
      float air = 0, land = 0;
      float jumpAge = 0;
      if (jumping && !Double.isNaN(jumpTick)) {
         float age = (float)(now - jumpTick);
         jumpAge = age;
         air = smooth(age / 3) * (1 - smooth((age - 12) / 7));
      }
      if (jumping && !Double.isNaN(landingTick)) {
         float age = (float)(now - landingTick);
         jumpAge = landingJumpAge + age;
         // Carry the airborne body pose across the landing tick, then ease into the recoil.
         air = landingAir * (1 - smooth(age / 3));
         if (age >= 0) land = smooth(age / 1.5F) * (1 - smooth((age - 1.5F) / 4)) * .65F;
      }
      float weight = Math.max(strideWeight, Math.max(air, land)) * clamp(strength, 0, 1);
      if (weight <= .0001F) return null;
      float total = Math.max(strideWeight, Math.max(air, land));
      float j = air / total, l = land / total, gait = strideWeight / total * (1 - j) * (1 - l);
      float phase = walkPosition * .6662F;
      float swing = (float)Math.cos(phase);
      float lean = lerp(.075F, .34F, sprintMix) * gait - .13F * j + .24F * l;
      float twist = (float)Math.sin(phase) * lerp(.035F, .07F, sprintMix) * gait;
      float roll = (float)Math.sin(phase) * lerp(.025F, .055F, sprintMix) * gait;
      float bounce = -(float)Math.abs(Math.sin(phase * 2)) * lerp(.18F, .4F, sprintMix) * gait + .5F * l;
      EnumMap<EmotePart, float[]> parts = new EnumMap<>(EmotePart.class);
      // Rotate upper-body anchors around the hips: head and shoulders stay attached to the torso.
      upper(parts, EmotePart.BODY, 0, 0, lean, twist, roll, bounce);
      upper(parts, EmotePart.HEAD, 0, 0, lean, twist, roll, bounce);
      upper(parts, EmotePart.RIGHT_ARM, -5, 2, lean, twist, roll, bounce);
      upper(parts, EmotePart.LEFT_ARM, 5, 2, lean, twist, roll, bounce);
      rotation(parts.get(EmotePart.BODY), lean, twist, roll);
      float armSwing = swing * lerp(.45F, .95F, sprintMix) * gait;
      float armSway = (float)Math.sin(jumpAge * .7F) * .12F * j * (sprintJump ? 0 : 1);
      float armPitch = lean * gait - .08F * j - .10F * l;
      float armRoll = lerp(.045F, .09F, sprintMix) * gait + .045F * j + .025F * l;
      // The takeoff selects the jump style. Item use is checked on every frame, including midair.
      if (!usingItem) {
         float reach = sprintJump ? j : 0;
         rotation(parts.get(EmotePart.RIGHT_ARM), -armSwing + armPitch + armSway - 2.72F * reach, twist, armRoll + .055F * reach);
         rotation(parts.get(EmotePart.LEFT_ARM), armSwing + armPitch - armSway - 2.27F * reach, twist, -armRoll - .175F * reach);
      }
      float step = swing * lerp(.62F, 1.05F, sprintMix) * gait;
      parts.put(EmotePart.RIGHT_LEG, bone(Float.NaN, 12 + bounce, 0, step + .65F * j - .25F * l, 0, .025F * j));
      parts.put(EmotePart.LEFT_LEG, bone(Float.NaN, 12 + bounce, 0, -step + .4F * j - .25F * l, 0, -.025F * j));
      return new EmoteEngine.Pose(parts, weight);
   }

   private static void upper(EnumMap<EmotePart, float[]> parts, EmotePart part, float x, float y,
      float pitch, float yaw, float roll, float bounce) {
      float dy = y - 12;
      float py = dy * (float)Math.cos(pitch), pz = dy * (float)Math.sin(pitch);
      float px = x * (float)Math.cos(yaw) + pz * (float)Math.sin(yaw);
      float z = -x * (float)Math.sin(yaw) + pz * (float)Math.cos(yaw);
      float rx = px * (float)Math.cos(roll) - py * (float)Math.sin(roll);
      float ry = px * (float)Math.sin(roll) + py * (float)Math.cos(roll);
      float[] values = new float[6];
      Arrays.fill(values, Float.NaN);
      values[0] = rx; values[1] = ry + 12 + bounce; values[2] = z;
      parts.put(part, values);
   }

   private static float[] bone(float x, float y, float z, float pitch, float yaw, float roll) {
      return new float[]{x, y, z, pitch, yaw, roll};
   }

   private static void rotation(float[] values, float pitch, float yaw, float roll) {
      values[3] = pitch; values[4] = yaw; values[5] = roll;
   }

   private static float smooth(float value) { float t = clamp(value, 0, 1); return t * t * (3 - 2 * t); }
   private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
   private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
