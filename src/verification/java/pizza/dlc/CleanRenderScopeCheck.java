package pizza.dlc;

import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.CleanFeedViewport;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;

public class CleanRenderScopeCheck {
   private static final class ModuleProbe extends Module {
      int starts, stops;
      ModuleProbe() { super("Probe", "", ModuleCategory.VISUALS); }
      @Override public void onEnable() { starts++; }
      @Override public void onDisable() { stops++; }
   }
   private static void check(boolean ok, String message) {
      if (!ok) throw new AssertionError(message);
   }
   public static void main(String[] args) throws Exception {
      ModuleProbe probe = new ModuleProbe();
      probe.setEnabled(true);
      check(probe.isEnabled(), "Enabled module before capture");
      check(CleanRenderScope.yaw(42, .5F) == 42, "Normal camera passthrough");
      try (var outer = new CleanRenderScope(new Angle(179, -20), new Angle(-179, 20))) {
         check(!probe.isEnabled(), "Visual module suppressed only during capture");
         check(probe.save().get("enabled").getAsBoolean(), "Persisted state must stay enabled");
         check(Math.abs(CleanRenderScope.yaw(0, .5F) - 180) < .001F, "Yaw uses shortest path across wrap");
         check(Math.abs(CleanRenderScope.pitch(0, .5F)) < .001F, "Pitch interpolation");
         var threadFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
         Thread other = new Thread(() -> check(!CleanRenderScope.isActive() && probe.isEnabled(), "Scope must not affect another thread"));
         other.setUncaughtExceptionHandler((thread, error) -> threadFailure.set(error));
         other.start(); other.join();
         check(threadFailure.get() == null, "Thread isolation check failed");
         try {
            try (var inner = new CleanRenderScope(null, null)) {
               check(CleanRenderScope.yaw(42, .5F) == 42, "Capture without Aura retains local angle");
               throw new IllegalStateException("simulated render failure");
            }
         } catch (IllegalStateException expected) { }
         check(CleanRenderScope.isActive() && Math.abs(CleanRenderScope.yaw(0, .5F) - 180) < .001F, "Nested failure restores outer scope");
      }
      check(!CleanRenderScope.isActive() && probe.isEnabled(), "Capture completion restores normal rendering");
      probe.setStreamProofSuspended(true);
      probe.setStreamProofSuspended(true);
      check(!probe.isEnabled() && probe.stops == 1, "Suspension cleans module state exactly once");
      check(probe.save().get("enabled").getAsBoolean(), "Suspension preserves configured state");
      probe.setEnabled(false);
      check(probe.stops == 1, "Changing configured state while suspended does not rerun cleanup");
      probe.setStreamProofSuspended(false);
      check(!probe.isEnabled() && probe.starts == 1, "Disabled module does not restart on return to main");
      probe.setStreamProofSuspended(true);
      probe.setEnabled(true);
      check(!probe.isEnabled() && probe.starts == 1, "Enabling during suspension remains inactive");
      probe.setStreamProofSuspended(false);
      probe.setStreamProofSuspended(false);
      check(probe.isEnabled() && probe.starts == 2, "Return to main restarts enabled module exactly once");
      var square = CleanFeedViewport.fit(1920, 1080, 1000, 1000);
      check(square.width() == 1000 && square.height() == 562 && square.x() == 0 && square.y() == 219, "Square window letterbox");
      check(square.sourceX(500, 800) == 400 && square.sourceY(500, 450) == 225, "GUI cursor at image centre");
      check(square.sourceY(0, 450) < 0, "Black bar must not click top GUI row");
      check(Math.abs(square.sourceY(square.targetY(123, 450), 450) - 123) < .001, "Cursor warp maps back to same GUI position");
      var wide = CleanFeedViewport.fit(800, 600, 1600, 600);
      check(wide.x() == 400 && wide.y() == 0 && wide.sourceX(400, 800) == 0, "Wide window pillarbox");
      System.out.println("PASS: camera interpolation, module suspension and resumption, config persistence, nested failure recovery, thread isolation, input viewport mapping");
   }
}

