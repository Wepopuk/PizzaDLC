package pizza.dlc;

import com.mojang.brigadier.CommandDispatcher;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.waypoints.GpsNavigation;
import pizza.dlc.api.waypoints.GpsRoute;
import pizza.dlc.api.waypoints.WaypointsManager;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.commands.GpsCommand;

public final class GpsNavigationCheck {
   private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
   private static void close(double actual, double expected) { check(Math.abs(actual - expected) < 1.0E-6, actual + " != " + expected); }
   private static Object allocate(Class<?> type) throws Exception {
      var unsafe = Class.forName("sun.misc.Unsafe");
      Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
      return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
   }
   private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
      Field field = owner.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
   }
   public static void main(String[] args) throws Exception {
      var south = new GpsRoute(0, null, 10, null);
      var east = new GpsRoute(10, null, 0, null);
      var west = new GpsRoute(-10, null, 0, null);
      var north = new GpsRoute(0, null, -10, null);
      close(south.bearing(0, 0, 0), 0);
      close(east.bearing(0, 0, 0), -90);
      close(west.bearing(0, 0, 0), 90);
      close(north.bearing(0, 0, 0), -180);
      close(west.bearing(0, 0, 90), 0);
      close(east.bearing(0, 0, -90), 0);
      close(south.bearing(0, 0, 720), 0);
      close(new GpsRoute(10, null, 10, null).bearing(0, 0, 0), -45);
      check(GpsRoute.voiceCue(east.bearing(0, 0, 0)).equals("right"), "East guidance is mirrored");
      check(GpsRoute.voiceCue(west.bearing(0, 0, 0)).equals("left"), "West guidance is mirrored");
      check(GpsRoute.voiceCue(-180).equals("uturn") && GpsRoute.voiceCue(5).equals("straight"), "Wrong voice sector");
      var horizontal = new GpsRoute(3, null, 4, null);
      close(horizontal.distance(0, 1000, 0), 5);
      close(new GpsRoute(3, 12.0, 4, null).distance(0, 0, 0), 13);
      check(horizontal.arrived(3, 1000, 4), "XZ arrival unexpectedly depends on height");
      check(!new GpsRoute(3, 12.0, 4, null).arrived(3, 0, 4), "XYZ route arrived on the wrong floor");
      try { new GpsRoute(Double.NaN, null, 0, null); throw new AssertionError("NaN destination accepted"); }
      catch (IllegalArgumentException expected) { }

      net.minecraft.SharedConstants.tryDetectVersion();
      net.minecraft.server.Bootstrap.bootStrap();
      var mc = (Minecraft)allocate(Minecraft.class); set(Minecraft.class, null, "instance", mc);
      var self = (TestPlayer)allocate(TestPlayer.class); mc.player = self;
      set(Entity.class, self, "position", new Vec3(0, 64, 0));
      var world = (ClientLevel)allocate(ClientLevel.class); mc.level = world;
      var preferences = java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("build"), "gps-prefs-").resolve("gps.json");
      var gps = new GpsNavigation(preferences); set(GpsNavigation.class, gps, "voiceEnabled", false);
      var client = (PizzaDLCClient)allocate(PizzaDLCClient.class);
      set(PizzaDLCClient.class, null, "instance", client);
      set(PizzaDLCClient.class, client, "gpsNavigation", gps);
      var waypoints = new WaypointsManager();
      waypoints.getWaypoints().put("Home base", new WaypointsManager.Waypoint("Home base", new Vec3(12, 70, -40), false, 0, null));
      set(PizzaDLCClient.class, client, "waypointsManager", waypoints);
      var dispatcher = new CommandDispatcher<PizzaDLCCommandSource>();
      new GpsCommand().registerTo(dispatcher);
      dispatcher.execute("gps voice oksana", PizzaDLCCommandSource.INSTANCE);
      check(gps.voice().equals("oksana") && gps.voiceEnabled(), "Voice selection did not enable Oksana");
      var restored = new GpsNavigation(preferences);
      check(restored.voice().equals("oksana") && restored.voiceEnabled(), "Selected voice was not persisted");
      dispatcher.execute("gps voice off", PizzaDLCCommandSource.INSTANCE);
      check(!new GpsNavigation(preferences).voiceEnabled(), "Voice off was not persisted");
      dispatcher.execute("gps voice alice", PizzaDLCCommandSource.INSTANCE);
      check(gps.voice().equals("alice") && gps.voiceEnabled(), "Switching to Alice failed");
      check(!gps.setVoice("unknown") && gps.voice().equals("alice"), "Unknown voice replaced the selection");
      java.nio.file.Files.writeString(preferences, "{\"voice\":false}");
      var legacy = new GpsNavigation(preferences);
      check(legacy.voice().equals("alice") && !legacy.voiceEnabled(), "Legacy GPS preference migration failed");
      set(GpsNavigation.class, gps, "voiceEnabled", false);
      check(dispatcher.execute("gps -12 50", PizzaDLCCommandSource.INSTANCE) == 1, "XZ command failed");
      check(gps.route().x() == -12 && gps.route().y() == null && gps.route().z() == 50, "XZ parser mapped coordinates incorrectly");
      dispatcher.execute("gps 12 70 -40", PizzaDLCCommandSource.INSTANCE);
      check(gps.route().y() == 70 && gps.route().z() == -40, "XYZ parser mapped coordinates incorrectly");
      dispatcher.execute("gps waypoint Home base", PizzaDLCCommandSource.INSTANCE);
      check(gps.route().name().equals("Home base"), "Named waypoint with spaces was not selected");
      check(dispatcher.execute("gps waypoint missing", PizzaDLCCommandSource.INSTANCE) == 0 && gps.route() != null, "Missing waypoint destroyed current navigation");
      dispatcher.execute("gps off", PizzaDLCCommandSource.INSTANCE);
      check(gps.route() == null, "Off command retained the target");
      gps.start(new GpsRoute(0, null, 0, null)); gps.onTick(new EventTick());
      check(gps.route() == null, "GPS did not finish at the destination");
      gps.start(south); gps.onWorldLoad(new EventWorldLoad());
      check(gps.route() == null, "Target leaked across world load");
      gps.start(south); mc.level = (ClientLevel)allocate(ClientLevel.class); gps.onTick(new EventTick());
      check(gps.route() == null, "Target leaked across server/world replacement");
      mc.level = world; gps.start(south); mc.player = null; gps.onTick(new EventTick());
      check(gps.route() == null && !gps.start(south), "Disconnected navigation remained active");
      System.out.println("GPS: camera-relative bearings, left/right voices, XZ/XYZ distances and arrival, command parsing, named waypoints, voice selection/persistence/migration, stop, disconnect and world reset passed.");
   }
   private static final class TestPlayer extends LocalPlayer {
      private TestPlayer() { super(null, null, null, null, null, null, false, null); }
      @Override public void sendSystemMessage(Component component) { }
   }
}
