package pizza.dlc.api.rotation;

import java.util.PriorityQueue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventRotationVector;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.Module;

public class RotationController implements QuickImports {
   public static RotationController INSTANCE;
   public static volatile boolean attackReady = false;
   private RotationPlan lastRotationPlan;
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle = Angle.DEFAULT;
   private int tickCounter;
   private final PriorityQueue<RotationTask> activeTasks = new PriorityQueue<>((r1, r2) -> Integer.compare(r2.priority, r1.priority));
   private int lastUpdateTick = Integer.MIN_VALUE;

   public RotationController() {
      INSTANCE = this;
   }

   public void setRotation(Angle value) {
      if (value == null) {
         this.previousAngle = this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
      } else {
         this.previousAngle = this.currentAngle;
      }

      this.currentAngle = value;
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public Angle getPreviousRotation() {
      return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(mc.player.yRotO, mc.player.xRotO);
   }

   public Angle getMoveRotation() {
      RotationPlan rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public RotationPlan getCurrentRotationPlan() {
      RotationPlan active = this.fetchActiveTaskValue();
      return active != null ? active : this.lastRotationPlan;
   }

   public void rotateTo(Angle angle, int reset, RotationConfig config, int priority, Module provider) {
      this.rotateTo(config.createRotationPlan(angle, angle.toVector(), null, reset), priority, provider);
   }

   public void rotateTo(Angle angle, RotationConfig config, int priority, Module provider) {
      this.rotateTo(config.createRotationPlan(angle, angle.toVector(), null, 1), priority, provider);
   }

   public void rotateTo(RotationPlan plan, int priority, Module provider) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed() || StreamProofRenderer.isVanillaGameplayActive()) return;
      this.activeTasks.removeIf(r -> r.provider == provider);
      this.activeTasks.add(new RotationTask(this.tickCounter + 1, priority, provider, plan));
   }

   public void update() {
      if (pizza.dlc.api.neuro.NeuroSystem.managed() || StreamProofRenderer.isVanillaGameplayActive()) return;
      if (mc.player != null) {
         int now = mc.player.tickCount;
         if (now != this.lastUpdateTick) {
            this.lastUpdateTick = now;
            RotationPlan activePlan = this.getCurrentRotationPlan();
            if (activePlan != null) {
               Angle clientAngle = AngleUtil.cameraAngle();
               if (this.lastRotationPlan != null) {
                  double differenceFromCurrentToPlayer = computeRotationDifference(this.serverAngle, clientAngle);
                  if (activePlan.getTicksUntilReset() <= this.tickCounter && differenceFromCurrentToPlayer < activePlan.getResetThreshold()) {
                     this.setRotation(null);
                     this.lastRotationPlan = null;
                     this.tickCounter = 0;
                     return;
                  }
               }

               Angle newAngle = activePlan.nextRotation(this.currentAngle != null ? this.currentAngle : clientAngle, this.fetchActiveTaskValue() == null)
                  .adjustSensitivity();
               this.setRotation(newAngle);
               this.lastRotationPlan = activePlan;
               this.tickCounter++;
            }
         }
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot(Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return Mth.wrapDegrees(a - b);
   }

   public boolean isActive() {
      return this.currentAngle != null && !StreamProofRenderer.isVanillaGameplayActive();
   }

   public void release(Module provider) {
      this.activeTasks.removeIf(r -> r.provider == provider);
      if (this.activeTasks.isEmpty()) {
         this.clear();
      }
   }

   public void clear() {
      this.activeTasks.clear();
      this.currentAngle = null;
      this.previousAngle = null;
      this.lastRotationPlan = null;
      this.tickCounter = 0;
   }

   public Vec3 fixVelocity(Vec3 currVelocity, Vec3 movementInput, float speed) {
      if (this.isActive()) {
         float yaw = this.currentAngle.getYaw();
         double d = movementInput.lengthSqr();
         if (d < 1.0E-7) {
            return Vec3.ZERO;
         }

         Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
         float f = Mth.sin(yaw * (float) (Math.PI / 180.0));
         float g = Mth.cos(yaw * (float) (Math.PI / 180.0));
         return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
      }

      return currVelocity;
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      SwapSystem.tick();
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) { this.clear(); return; }
      this.update();
      if (this.isActive()) {
         Angle a = this.getRotation();
         e.setYaw(a.getYaw());
         e.setPitch(a.getPitch());
      }

      e.addPostAction(() -> this.serverAngle = new Angle(e.getYaw(), e.getPitch()));
   }

   @EventHandler
   public void onRotationVector(EventRotationVector e) {
      if (!this.isActive()) return;

      e.setYaw(this.getMoveRotation().getYaw());
   }

   private RotationPlan fetchActiveTaskValue() {
      while (!this.activeTasks.isEmpty() && this.activeTasks.peek() != null) {
         RotationTask task = this.activeTasks.peek();
         if (task.expiresAt <= this.tickCounter || !task.provider.isEnabled()) {
            this.activeTasks.poll();
            continue;
         }
         break;
      }

      return !this.activeTasks.isEmpty() && this.activeTasks.peek() != null ? this.activeTasks.peek().plan : null;
   }

   public TaskSnapshot snapshotActiveTask() {
      RotationTask task = this.activeTasks.peek();
      if (task == null) {
         return TaskSnapshot.NONE;
      }

      boolean alive = task.expiresAt > this.tickCounter && task.provider.isEnabled();
      return new TaskSnapshot(alive, task.provider.getName(), task.priority, task.expiresAt);
   }

   public RotationPlan getLastRotationPlan() {
      return this.lastRotationPlan;
   }

   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   public Angle getPreviousAngle() {
      return this.previousAngle;
   }

   public Angle getServerAngle() {
      return this.serverAngle;
   }

   public int getTickCounter() {
      return this.tickCounter;
   }

   public PriorityQueue<RotationTask> getActiveTasks() {
      return this.activeTasks;
   }

   public int getLastUpdateTick() {
      return this.lastUpdateTick;
   }

   private record RotationTask(int expiresAt, int priority, Module provider, RotationPlan plan) {
   }

   public record TaskSnapshot(boolean alive, String owner, int priority, int expiresAt) {
      public static final TaskSnapshot NONE = new TaskSnapshot(false, "—", 0, 0);
   }
}
