package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.modules.impl.player.ItemFinder;

@CommandInfo(
   name = "ItemFinder",
   aliases = {"itemfinder", "finditem", "if"}
)
public class ItemFinderCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(
         context -> {
            CommandUsage.send(
               ".itemfinder search <предмет>",
               ".itemfinder stop",
               ".itemfinder radius <1-32>"
            );
            return 1;
         }
      );
      builder.then(
         literal("search")
            .then(
               arg("item", StringArgumentType.greedyString())
                  .executes(
                     context -> {
                        String item = StringArgumentType.getString(context, "item").trim();
                        PizzaDLCClient.getInstance()
                           .getModuleManager()
                           .get(ItemFinder.class)
                           .startSearch(item);
                        return 1;
                     }
                  )
            )
      );
      builder.then(
         literal("stop")
            .executes(
               context -> {
                  PizzaDLCClient.getInstance().getModuleManager().get(ItemFinder.class).stopSearch();
                  return 1;
               }
            )
      );
      builder.then(
         literal("radius")
            .then(
               arg("value", IntegerArgumentType.integer(1, 32))
                  .executes(
                     context -> {
                        int value = IntegerArgumentType.getInteger(context, "value");
                        ItemFinder module = PizzaDLCClient.getInstance().getModuleManager().get(ItemFinder.class);
                        module.setRadius(value);
                        ChatUtility.send(ChatFormatting.GRAY + "Радиус ItemFinder: " + ChatFormatting.WHITE + value);
                        return 1;
                     }
                  )
            )
      );
   }
}
