package pizza.dlc.api.util.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.player.SwapSettings;

/** Ordered swap tasks from GuiMove: rage, stationary legit with a hold, and sprint-free Matrix. */
public final class InventorySwapQueue implements QuickImports {
   public enum Mode { RAGE, LEGIT, MATRIX }
   private static final Engine ENGINE = new Engine();
   private static boolean executing;
   private InventorySwapQueue() {}

   public static Mode configuredMode() {
      var settings = SwapSettings.get();
      return settings == null ? Mode.RAGE : settings.actionMode.is("Легитный") ? Mode.LEGIT
         : settings.actionMode.is("Matrix") ? Mode.MATRIX : Mode.RAGE;
   }
   public static boolean executing() { return executing; }
   public static boolean submit(Object owner, String name, Mode mode, int delay, Runnable cleanup, Runnable... steps) {
      if (mc.player == null || mc.level == null || mc.gameMode == null || steps.length == 0
          || pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive()
          || pizza.dlc.api.util.render.streamproof.StreamProofRenderer.isVanillaGameplayActive()) return false;
      LocalPlayer player = mc.player; ClientLevel level = mc.level;
      BooleanSupplier context = () -> mc.player == player && mc.level == level && player.isAlive()
         && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty();
      BooleanSupplier valid = () -> context.getAsBoolean() && (!(owner instanceof Module module) || module.isEnabled());
      Runnable safeCleanup = () -> { if (context.getAsBoolean() && cleanup != null) runInternal(cleanup); };
      Runnable[] wrapped = new Runnable[steps.length];
      for (int i=0; i<steps.length; i++) { Runnable step=steps[i]; wrapped[i]=() -> runInternal(step); }
      if (!valid.getAsBoolean()) return false;
      int hold = SwapSettings.get() == null ? 4 : (int)SwapSettings.get().stopTicks.getValue();
      return ENGINE.submit(owner, name, mode, delay, hold, valid, safeCleanup, wrapped);
   }
   private static void runInternal(Runnable action) {
      boolean previous=executing, click=SwapSystem.isInternalClick(); executing=true; SwapSystem.setInternalClick(true);
      try { action.run(); } finally { executing=previous; SwapSystem.setInternalClick(click); }
   }
   public static void tick() {
      boolean moving=mc.player != null && mc.player.input != null && mc.player.input.getMoveVector().lengthSquared()>0;
      boolean sprinting=mc.player != null && (mc.player.isSprinting() || mc.player.wasSprinting);
      ENGINE.tick(moving, sprinting);
   }
   public static boolean blocksMovement() { return ENGINE.blocksMovement(); }
   public static boolean blocksSprint() { return ENGINE.blocksSprint(); }
   public static void cancel(Object owner) { ENGINE.cancel(owner); }
   public static void clear() { ENGINE.clear(); }

   // Independent clock/input state makes task ordering and cancellation verifiable without a server.
   public static final class Engine {
      private final Deque<Task> tasks = new ArrayDeque<>();
      private long tick;
      public boolean submit(Object owner, String name, Mode mode, int delay, int hold, BooleanSupplier valid,
                            Runnable cleanup, Runnable... steps) {
         if (tasks.stream().anyMatch(t -> t.owner == owner && t.name.equals(name))) return false;
         Task task=new Task(owner,name,mode,Math.max(0,delay),Math.max(0,hold),valid,cleanup,steps,tick);
         tasks.addLast(task);
         if (mode == Mode.RAGE && tasks.peekFirst() == task && steps.length == 1) advance(task,false,false);
         return true;
      }
      public void tick(boolean moving, boolean sprinting) {
         tick++;
         tasks.removeIf(t -> { if (t.valid.getAsBoolean()) return false; cleanup(t); return true; });
         Task task=tasks.peekFirst(); if (task != null) advance(task,moving,sprinting);
      }
      private void advance(Task t, boolean moving, boolean sprinting) {
         if (!t.valid.getAsBoolean()) { tasks.remove(t); cleanup(t); return; }
         if (t.finished >= 0) {
            if (tick-t.finished >= t.hold) tasks.remove(t);
            return;
         }
         if (t.mode == Mode.LEGIT && (moving || tick == t.created) || t.mode == Mode.MATRIX && sprinting) return;
         if (t.index > 0 && tick-t.lastStep < t.delay) return;
         try { t.steps[t.index++].run(); }
         catch (RuntimeException | Error exception) { tasks.remove(t);cleanup(t);throw exception; }
         t.lastStep=tick;
         if (t.index == t.steps.length) {
            t.finished=tick;
            if (t.mode != Mode.LEGIT || t.hold == 0) tasks.remove(t);
         }
      }
      public boolean blocksMovement() { return tasks.stream().anyMatch(t -> t.mode == Mode.LEGIT && t.valid.getAsBoolean()); }
      public boolean blocksSprint() { return tasks.stream().anyMatch(t -> t.mode != Mode.RAGE && t.valid.getAsBoolean()); }
      public void cancel(Object owner) { tasks.removeIf(t -> { if (t.owner != owner) return false; cleanup(t); return true; }); }
      public void clear() { for (Task t:tasks) cleanup(t); tasks.clear(); }
      public int size() { return tasks.size(); }
      private static void cleanup(Task t) { if (t.index > 0 && t.finished < 0 && t.cleanup != null) t.cleanup.run(); }
      private static final class Task {
         final Object owner; final String name; final Mode mode; final int delay,hold; final BooleanSupplier valid;
         final Runnable cleanup; final Runnable[] steps; final long created; int index; long lastStep,finished=-1;
         Task(Object owner,String name,Mode mode,int delay,int hold,BooleanSupplier valid,Runnable cleanup,Runnable[] steps,long created) {
            this.owner=owner;this.name=name;this.mode=mode;this.delay=delay;this.hold=hold;this.valid=valid;
            this.cleanup=cleanup;this.steps=steps;this.created=created;
         }
      }
   }
}
