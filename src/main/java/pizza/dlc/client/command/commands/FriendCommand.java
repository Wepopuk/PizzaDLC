package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.filemanager.impl.FriendManager;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Friend",
   aliases = {"f", "friends", "friend"}
)
public class FriendCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".f", ".f add <name>", ".f remove <name>", ".f del <name>", ".f list", ".f clear");
         return 1;
      });
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("add")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
               if (mc.getConnection() != null) {
                  String remaining = suggestionsBuilder.getRemaining().toLowerCase();

                  for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
                     if (entry.getProfile() == null || entry.getProfile().name() == null) continue;

                     String playerName = entry.getProfile().name();
                     if (playerName.toLowerCase().startsWith(remaining)) {
                        suggestionsBuilder.suggest(playerName);
                     }
                  }
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               FriendManager friendManager = PizzaDLC.getInstance().getFriendManager();
               if (friendManager.isFriend(name)) {
                  ChatUtility.send(ChatFormatting.RED + name + " is already your friend!");
               } else {
                  friendManager.getItems().add(name);
                  friendManager.save();
                  ChatUtility.send(ChatFormatting.GREEN + "Successfully added " + ChatFormatting.WHITE + name + ChatFormatting.GREEN + " to friends.");
               }

               return 1;
            }))
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("remove")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
               String remaining = suggestionsBuilder.getRemaining().toLowerCase();

               for (String friend : PizzaDLC.getInstance().getFriendManager().getItems()) {
                  if (!friend.toLowerCase().startsWith(remaining)) continue;

                  suggestionsBuilder.suggest(friend);
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               FriendManager friendManager = PizzaDLC.getInstance().getFriendManager();
               if (friendManager.isFriend(name)) {
                  friendManager.removeFriend(name);
                  friendManager.save();
                  ChatUtility.send(ChatFormatting.GREEN + "Removed " + ChatFormatting.WHITE + name + ChatFormatting.GREEN + " from friends.");
               } else {
                  ChatUtility.send(ChatFormatting.RED + name + " is not in your friend list!");
               }

               return 1;
            }))
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("del")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
               String remaining = suggestionsBuilder.getRemaining().toLowerCase();

               for (String friend : PizzaDLC.getInstance().getFriendManager().getItems()) {
                  if (!friend.toLowerCase().startsWith(remaining)) continue;

                  suggestionsBuilder.suggest(friend);
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               FriendManager friendManager = PizzaDLC.getInstance().getFriendManager();
               if (friendManager.isFriend(name)) {
                  friendManager.removeFriend(name);
                  friendManager.save();
                  ChatUtility.send(ChatFormatting.GREEN + "Removed " + ChatFormatting.WHITE + name + ChatFormatting.GREEN + " from friends.");
               } else {
                  ChatUtility.send(ChatFormatting.RED + name + " is not in your friend list!");
               }

               return 1;
            }))
      );
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("list").executes(context -> {
         FriendManager friendManager = PizzaDLC.getInstance().getFriendManager();
         if (friendManager.getItems().isEmpty()) {
            ChatUtility.send(ChatFormatting.RED + "Your friend list is empty.");
         } else {
            StringBuilder friendsList = new StringBuilder();

            for (String friend : friendManager.getItems()) {
               friendsList.append(friend).append(", ");
            }

            String result = friendsList.substring(0, friendsList.length() - 2);
            ChatUtility.send(ChatFormatting.GRAY + "Friends (" + friendManager.getItems().size() + "): " + ChatFormatting.WHITE + result);
         }

         return 1;
      }));
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("clear").executes(context -> {
         FriendManager friendManager = PizzaDLC.getInstance().getFriendManager();
         if (friendManager.getItems().isEmpty()) {
            ChatUtility.send(ChatFormatting.RED + "Friend list is already empty.");
         } else {
            int count = friendManager.getItems().size();
            friendManager.getItems().clear();
            friendManager.save();
            ChatUtility.send(ChatFormatting.GREEN + "Removed all " + count + " friends.");
         }

         return 1;
      }));
   }
}
