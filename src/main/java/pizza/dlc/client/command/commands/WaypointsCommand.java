package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.waypoints.WaypointsManager;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Waypoints",
   aliases = {"way", "waypoint", "wps"}
)
public class WaypointsCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".way", ".way add <name>", ".way add <name> <x> <y> <z>", ".way del <name>", ".way list", ".way clear");
         return 1;
      });
      builder.then(
         literal("add")
            .then(
               (RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.string()).executes(context -> {
                  if (mc.player == null) {
                     return 1;
                  }

                  String name = StringArgumentType.getString(context, "name");
                  BlockPos pos = mc.player.blockPosition();
                  return this.doAdd(name, new Vec3(pos.getX(), pos.getY(), pos.getZ()));
               }))
                  .then(
                     RequiredArgumentBuilder.<PizzaDLCCommandSource, Integer>argument("x", IntegerArgumentType.integer())
                        .then(
                           RequiredArgumentBuilder.<PizzaDLCCommandSource, Integer>argument("y", IntegerArgumentType.integer())
                              .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, Integer>argument("z", IntegerArgumentType.integer()).executes(context -> {
                                 String name = StringArgumentType.getString(context, "name");
                                 int x = IntegerArgumentType.getInteger(context, "x");
                                 int y = IntegerArgumentType.getInteger(context, "y");
                                 int z = IntegerArgumentType.getInteger(context, "z");
                                 return this.doAdd(name, new Vec3(x, y, z));
                              }))
                        )
                  )
            )
      );
      builder.then(literal("del").then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
         String remaining = suggestionsBuilder.getRemaining().toLowerCase();

         for (WaypointsManager.Waypoint waypoint : PizzaDLCClient.getInstance().getWaypointsManager().getWaypoints().values()) {
            if (waypoint == null || waypoint.temp || !waypoint.name.toLowerCase().startsWith(remaining)) continue;

            suggestionsBuilder.suggest(waypoint.name);
         }

         return suggestionsBuilder.buildFuture();
      }).executes(context -> {
         String name = StringArgumentType.getString(context, "name");
         WaypointsManager manager = PizzaDLCClient.getInstance().getWaypointsManager();
         if (manager.del(name)) {
            manager.save();
            ChatUtility.send(ChatFormatting.GRAY + "Deleted waypoint: " + ChatFormatting.WHITE + name);
         } else {
            ChatUtility.sendError("Waypoint not found: " + name);
         }

         return 1;
      })));
      builder.then(literal("clear").executes(context -> {
         WaypointsManager manager = PizzaDLCClient.getInstance().getWaypointsManager();
         manager.clearPersistent();
         manager.save();
         ChatUtility.send(ChatFormatting.GRAY + "Cleared waypoints.");
         return 1;
      }));
      builder.then(
         literal("list")
            .executes(
               context -> {
                  WaypointsManager manager = PizzaDLCClient.getInstance().getWaypointsManager();
                  if (manager.getWaypoints().isEmpty()) {
                     ChatUtility.send(ChatFormatting.GRAY + "No waypoints.");
                     return 1;
                  }

                  StringBuilder waypoints = new StringBuilder();

                  for (WaypointsManager.Waypoint waypoint : manager.getWaypoints().values()) {
                     if (waypoint == null || waypoint.temp) continue;

                     waypoints.append(waypoint.name)
                        .append(" (")
                        .append((int)waypoint.pos.x)
                        .append(" ")
                        .append((int)waypoint.pos.y)
                        .append(" ")
                        .append((int)waypoint.pos.z)
                        .append("), ");
                  }

                  if (waypoints.length() >= 2) {
                     waypoints.setLength(waypoints.length() - 2);
                  }

                  ChatUtility.send(ChatFormatting.GRAY + "Waypoints: " + ChatFormatting.WHITE + waypoints);
                  return 1;
               }
            )
      );
   }

   private int doAdd(String name, Vec3 pos) {
      WaypointsManager manager = PizzaDLCClient.getInstance().getWaypointsManager();
      if (manager.add(name, pos)) {
         manager.save();
         ChatUtility.send(
            ChatFormatting.GRAY
               + "Added waypoint: "
               + ChatFormatting.WHITE
               + name
               + ChatFormatting.GRAY
               + " ("
               + (int)pos.x
               + " "
               + (int)pos.y
               + " "
               + (int)pos.z
               + ")"
         );
      } else {
         ChatUtility.sendError("Waypoint already exists: " + name);
      }

      return 1;
   }
}
