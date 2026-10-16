package pizza.dlc.client.command.commands;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;
import pizza.dlc.client.modules.Module;

@CommandInfo(
   name = "bind",
   aliases = {}
)
public class BindCommand extends Command {
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
      "f12",
      "none"
   };

   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".bind", ".bind <module> <key>", ".bind <module> none  (снять бинд)", ".bind list", ".bind clear");
         return 1;
      });
      builder.then(
         LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("list")
            .executes(
               context -> {
                  boolean any = false;
                  ChatUtility.send(ChatFormatting.GRAY + "Bound modules:");

                  for (Module m : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
                     if (m.getKey() > 0) {
                        any = true;
                        ChatUtility.send(
                           ChatFormatting.WHITE
                              + "- "
                              + ChatFormatting.AQUA
                              + m.getName()
                              + ChatFormatting.GRAY
                              + " ["
                              + ChatFormatting.YELLOW
                              + KeyNameUtil.getKeyName(m.getKey()).toUpperCase()
                              + ChatFormatting.GRAY
                              + "]"
                        );
                     }
                  }

                  if (!any) {
                     ChatUtility.send(ChatFormatting.RED + "No bound modules.");
                  }

                  return 1;
               }
            )
      );
      builder.then(LiteralArgumentBuilder.<PizzaDLCCommandSource>literal("clear").executes(context -> {
         int n = 0;

         for (Module m : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
            if (m.getKey() > 0) {
               m.setKey(-1);
               n++;
            }
         }

         PizzaDLCClient.getInstance().getConfigManager().save();
         ChatUtility.send(ChatFormatting.GREEN + "Cleared " + n + " bind(s).");
         return 1;
      }));
      builder.then(
         RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("module", StringArgumentType.word())
            .suggests((context, sb) -> {
               String rem = sb.getRemaining().toLowerCase();

               for (Module m : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
                  String noSpace = m.getName().replace(" ", "");
                  if (noSpace.toLowerCase().startsWith(rem)) {
                     sb.suggest(noSpace);
                  }
               }

               return sb.buildFuture();
            })
            .then(
               RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("key", StringArgumentType.word())
                  .suggests((context, sb) -> {
                     String rem = sb.getRemaining().toLowerCase();

                     for (String k : KEY_SUGGESTIONS) {
                        if (!k.startsWith(rem)) continue;

                        sb.suggest(k);
                     }

                     return sb.buildFuture();
                  })
                  .executes(
                     context -> {
                        String moduleName = StringArgumentType.getString(context, "module");
                        String keyName = StringArgumentType.getString(context, "key");
                        Module m = findModule(moduleName);
                        if (m == null) {
                           ChatUtility.send(ChatFormatting.RED + "Module not found: " + ChatFormatting.WHITE + moduleName);
                           return 1;
                        }

                        if (!keyName.equalsIgnoreCase("none") && !keyName.equalsIgnoreCase("unbind") && !keyName.equalsIgnoreCase("remove")) {
                           int keyCode = parseKey(keyName);
                           if (keyCode <= 0) {
                              ChatUtility.send(ChatFormatting.RED + "Invalid key: " + ChatFormatting.WHITE + keyName);
                              return 1;
                           }

                           m.setKey(keyCode);
                           PizzaDLCClient.getInstance().getConfigManager().save();
                           ChatUtility.send(
                              ChatFormatting.GREEN
                                 + "Bound "
                                 + ChatFormatting.WHITE
                                 + m.getName()
                                 + ChatFormatting.GRAY
                                 + " to "
                                 + ChatFormatting.YELLOW
                                 + keyName.toUpperCase()
                           );
                           return 1;
                        }

                        m.setKey(-1);
                        PizzaDLCClient.getInstance().getConfigManager().save();
                        ChatUtility.send(ChatFormatting.GREEN + "Unbound " + ChatFormatting.WHITE + m.getName());
                        return 1;
                     }
                  )
            )
      );
   }

   private static Module findModule(String input) {
      String norm = input.replace(" ", "");

      for (Module m : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
         if (!m.getName().replace(" ", "").equalsIgnoreCase(norm)) continue;

         return m;
      }

      return null;
   }

   private static int parseKey(String name) {
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
