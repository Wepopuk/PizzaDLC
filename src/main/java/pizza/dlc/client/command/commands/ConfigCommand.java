package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.io.File;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.config.ConfigManager;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Config",
   aliases = {"cfg", "configs", "profile"}
)
public class ConfigCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".cfg", ".cfg list", ".cfg dir", ".cfg save <name>", ".cfg load <name>", ".cfg delete <name>", ".cfg reset");
         return 1;
      });
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("list").executes(context -> {
         StringBuilder configs = new StringBuilder();

         for (String configName : PizzaDLCClient.getInstance().getConfigManager().configNames()) {
            String cleanName = configName.replace("." + "pizzadlc".toLowerCase(), "");
            configs.append(cleanName).append(", ");
         }

         if (configs.length() > 0) {
            String list = configs.substring(0, configs.length() - 2);
            ChatUtility.send(ChatFormatting.GRAY + "Available configs: " + ChatFormatting.WHITE + list);
         } else {
            ChatUtility.send(ChatFormatting.RED + "No configs found.");
         }

         return 1;
      }));
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("dir").executes(context -> {
         try {
            File dir = ConfigManager.configDirectory;
            if (!dir.exists()) {
               dir.mkdirs();
            }

            Util.getPlatform().openFile(dir);
            ChatUtility.send(ChatFormatting.GRAY + "Opened config directory.");
         } catch (Exception ex) {
            ChatUtility.send(ChatFormatting.RED + "Failed to open directory: " + ex);
            PizzaDLC.LOGGER.error("Failed to open config directory", ex);
         }

         return 1;
      }));
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("save").then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            if (PizzaDLCClient.getInstance().getConfigManager().saveConfig(name)) {
               ChatUtility.send(ChatFormatting.GREEN + "Successfully saved config: " + ChatFormatting.WHITE + name);
            } else {
               ChatUtility.send(ChatFormatting.RED + "Failed to save config!");
            }

            return 1;
         }))
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("load")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.greedyString()).suggests((context, suggestionsBuilder) -> {
               String remaining = suggestionsBuilder.getRemaining().toLowerCase();

               for (String configName : PizzaDLCClient.getInstance().getConfigManager().configNames()) {
                  String cleanName = configName.replace("." + "pizzadlc".toLowerCase(), "");
                  if (cleanName.toLowerCase().startsWith(remaining)) {
                     suggestionsBuilder.suggest(cleanName);
                  }
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               if (PizzaDLCClient.getInstance().getConfigManager().loadConfig(name)) {
                  ChatUtility.send(ChatFormatting.GREEN + "Successfully loaded config: " + ChatFormatting.WHITE + name);
               } else {
                  ChatUtility.send(ChatFormatting.RED + "Failed to load config! Does it exist?");
               }

               return 1;
            }))
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("delete")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.greedyString()).suggests((context, suggestionsBuilder) -> {
               String remaining = suggestionsBuilder.getRemaining().toLowerCase();

               for (String configName : PizzaDLCClient.getInstance().getConfigManager().configNames()) {
                  String cleanName = configName.replace("." + "pizzadlc".toLowerCase(), "");
                  if (cleanName.toLowerCase().startsWith(remaining)) {
                     suggestionsBuilder.suggest(cleanName);
                  }
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               if (PizzaDLCClient.getInstance().getConfigManager().deleteConfig(name)) {
                  ChatUtility.send(ChatFormatting.GREEN + "Successfully deleted config: " + ChatFormatting.WHITE + name);
               } else {
                  ChatUtility.send(ChatFormatting.RED + "Failed to delete config! (Check spelling)");
               }

               return 1;
            }))
      );
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("reset").executes(context -> {
         try {
            if (PizzaDLCClient.getInstance() != null) {
               PizzaDLCClient.getInstance().resetToDefaults();
               ChatUtility.send(ChatFormatting.GREEN + "Successfully reset settings to defaults.");
            } else {
               ChatUtility.send(ChatFormatting.RED + "Client is not initialized.");
            }
         } catch (Exception ex) {
            ChatUtility.send(ChatFormatting.RED + "Failed to reset: " + ex);
            PizzaDLC.LOGGER.error("Failed to reset config", ex);
         }

         return 1;
      }));
   }
}
