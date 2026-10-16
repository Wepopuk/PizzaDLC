package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.waypoints.GpsRoute;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(name = "GPS", aliases = {"gps", "nav"})
public final class GpsCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".gps", ".gps <x> <z>", ".gps <x> <y> <z>", ".gps waypoint <name>", ".gps off", ".gps voice on|off|alice|oksana");
         return 1;
      });
      builder.then(arg("x", IntegerArgumentType.integer(-30000000, 30000000))
         .then(arg("second", IntegerArgumentType.integer(-30000000, 30000000))
            .executes(context -> start(new GpsRoute(IntegerArgumentType.getInteger(context, "x"), null,
               IntegerArgumentType.getInteger(context, "second"), null)))
            .then(arg("z", IntegerArgumentType.integer(-30000000, 30000000)).executes(context -> start(
               new GpsRoute(IntegerArgumentType.getInteger(context, "x"), (double)IntegerArgumentType.getInteger(context, "second"),
                  IntegerArgumentType.getInteger(context, "z"), null))))));
      builder.then(literal("off").executes(context -> {
         PizzaDLCClient.getInstance().getGpsNavigation().stop();
         ChatUtility.send(Translations.tr("gps.stopped"));
         return 1;
      }));
      builder.then(literal("waypoint").then(arg("name", StringArgumentType.greedyString()).suggests((context, suggestions) -> {
         var manager = PizzaDLCClient.getInstance().getWaypointsManager();
         String prefix = suggestions.getRemaining().toLowerCase(Locale.ROOT);
         for (String name : manager.getWaypoints().keySet()) if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) suggestions.suggest(name);
         return suggestions.buildFuture();
      }).executes(context -> {
         String name = StringArgumentType.getString(context, "name");
         var waypoint = PizzaDLCClient.getInstance().getWaypointsManager().getWaypoints().get(name);
         if (waypoint == null) { ChatUtility.sendError(Translations.tr("gps.missing_waypoint", name)); return 0; }
         return start(new GpsRoute(waypoint.pos.x, waypoint.pos.y, waypoint.pos.z, waypoint.name));
      })));
      builder.then(literal("voice")
         .then(literal("on").executes(context -> voice(true)))
         .then(literal("off").executes(context -> voice(false)))
         .then(literal("alice").executes(context -> chooseVoice("alice")))
         .then(literal("oksana").executes(context -> chooseVoice("oksana"))));
   }

   private int start(GpsRoute route) {
      var gps = PizzaDLCClient.getInstance().getGpsNavigation();
      if (!gps.start(route)) { ChatUtility.sendError(Translations.tr("gps.join_world")); return 0; }
      String destination = route.name() != null ? route.name()
         : (int)route.x() + " " + (route.y() == null ? "" : route.y().intValue() + " ") + (int)route.z();
      ChatUtility.send(Translations.tr("gps.started", destination));
      return 1;
   }

   private int voice(boolean enabled) {
      var gps = PizzaDLCClient.getInstance().getGpsNavigation();
      if (!gps.setVoiceEnabled(enabled)) { ChatUtility.sendError(Translations.tr("gps.save_error")); return 0; }
      ChatUtility.send(Translations.tr(enabled ? "gps.voice_on" : "gps.voice_off"));
      if (enabled && !gps.hasVoicePack()) ChatUtility.send(Translations.tr("gps.voice_missing"));
      return 1;
   }

   private int chooseVoice(String name) {
      var gps = PizzaDLCClient.getInstance().getGpsNavigation();
      if (!gps.setVoice(name)) { ChatUtility.sendError(Translations.tr("gps.save_error")); return 0; }
      ChatUtility.send(Translations.tr("gps.voice_selected", Translations.tr("gps.voice." + name)));
      return 1;
   }
}
