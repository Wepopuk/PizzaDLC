package pizza.dlc.client.command.commands;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import pizza.dlc.api.macros.Macro;
import pizza.dlc.api.macros.constructor.MacroBlock;
import pizza.dlc.api.macros.constructor.impl.SendMessageBlock;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Macros",
   aliases = {"macro", "m"}
)
public class MacrosCommand extends Command {
   private static final String[] KEY_SUGGESTIONS = new String[]{
      "r",
      "g",
      "v",
      "x",
      "z",
      "c",
      "f",
      "h",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "0",
      "space",
      "enter",
      "tab",
      "escape",
      "backspace",
      "lshift",
      "rshift",
      "lctrl",
      "rctrl",
      "lalt",
      "ralt",
      "f1",
      "f2",
      "f3",
      "f4",
      "f5",
      "f6",
      "f7",
      "f8",
      "f9",
      "f10",
      "f11",
      "f12"
   };

   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".macro", ".macro add <name> <key> <message>", ".macro list", ".macro delete <name>");
         return 1;
      });
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("add")
            .then(
               RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.word())
                  .then(
                     RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("key", StringArgumentType.word())
                        .suggests((context, suggestionsBuilder) -> {
                           String remaining = suggestionsBuilder.getRemaining().toLowerCase();

                           for (String key : KEY_SUGGESTIONS) {
                              if (!key.startsWith(remaining)) continue;

                              suggestionsBuilder.suggest(key);
                           }

                           return suggestionsBuilder.buildFuture();
                        })
                        .then(
                           RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("message", StringArgumentType.greedyString())
                              .executes(
                                 context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    String keyName = StringArgumentType.getString(context, "key");
                                    String message = StringArgumentType.getString(context, "message");
                                    int keyCode = this.parseKey(keyName);
                                    if (keyCode != -1 && keyCode != 0) {
                                       MacroBlock chatBlock = new SendMessageBlock(message);
                                       Macro macro = new Macro(name, Collections.singletonList(chatBlock));
                                       macro.setKey(keyCode);
                                       PizzaDLCClient.getInstance().getMacros().getRegisteredMacros().add(macro);
                                       PizzaDLCClient.getInstance().getMacros().save();
                                       String readableKey = keyName.toUpperCase();
                                       ChatUtility.send(
                                          ChatFormatting.GREEN
                                             + "Added macro "
                                             + ChatFormatting.WHITE
                                             + name
                                             + ChatFormatting.GRAY
                                             + " bound to "
                                             + ChatFormatting.YELLOW
                                             + readableKey
                                       );
                                       return 1;
                                    }

                                    ChatUtility.send(ChatFormatting.RED + "Invalid key: " + ChatFormatting.WHITE + keyName);
                                    return 1;
                                 }
                              )
                        )
                  )
            )
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("list")
            .executes(
               context -> {
                  if (PizzaDLCClient.getInstance().getMacros().getRegisteredMacros().isEmpty()) {
                     ChatUtility.send(ChatFormatting.RED + "No macros found.");
                     return 1;
                  }

                  ChatUtility.send(ChatFormatting.GRAY + "Registered Macros:");

                  for (Macro macro : PizzaDLCClient.getInstance().getMacros().getRegisteredMacros()) {
                     String keyName = KeyNameUtil.getKeyName(macro.getKey());
                     ChatUtility.send(
                        ChatFormatting.WHITE
                           + "- "
                           + ChatFormatting.AQUA
                           + macro.getName()
                           + ChatFormatting.GRAY
                           + " ["
                           + ChatFormatting.YELLOW
                           + keyName.toUpperCase()
                           + ChatFormatting.GRAY
                           + "]"
                     );
                  }

                  return 1;
               }
            )
      );
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("delete")
            .then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("name", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
               String remaining = suggestionsBuilder.getRemaining().toLowerCase();

               for (Macro macro : PizzaDLCClient.getInstance().getMacros().getRegisteredMacros()) {
                  if (!macro.getName().toLowerCase().startsWith(remaining)) continue;

                  suggestionsBuilder.suggest(macro.getName());
               }

               return suggestionsBuilder.buildFuture();
            }).executes(context -> {
               String name = StringArgumentType.getString(context, "name");
               boolean removed = PizzaDLCClient.getInstance().getMacros().getRegisteredMacros().removeIf(macro -> macro.getName().equalsIgnoreCase(name));
               if (removed) {
                  PizzaDLCClient.getInstance().getMacros().save();
                  ChatUtility.send(ChatFormatting.GREEN + "Deleted " + name);
               } else {
                  ChatUtility.send(ChatFormatting.RED + "Not found: " + name);
               }

               return 1;
            }))
      );
   }

   private int parseKey(String name) {
      name = name.toLowerCase();

      try {
         Key key = InputConstants.getKey("key.keyboard." + name);
         if (key != InputConstants.UNKNOWN) {
            return key.getValue();
         }
      } catch (Exception ignored) {
      }
      int commonKey = switch (name) {
         case "space" -> 32;
         case "enter", "return" -> 257;
         case "tab" -> 258;
         case "escape", "esc" -> 256;
         case "backspace", "back" -> 259;
         case "lshift", "left_shift", "shift" -> 340;
         case "rshift", "right_shift" -> 344;
         case "lctrl", "left_ctrl", "ctrl", "control" -> 341;
         case "rctrl", "right_ctrl" -> 345;
         case "lalt", "left_alt", "alt" -> 342;
         case "ralt", "right_alt" -> 346;
         default -> -1;
      };
      if (commonKey != -1) {
         return commonKey;
      }

      try {
         return Integer.parseInt(name);
      } catch (NumberFormatException ignored) {
         return -1;
      }
   }
}
