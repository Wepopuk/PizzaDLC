package pizza.dlc.client.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Help",
   aliases = {"help", "commands", "helpme"}
)
public class HelpCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         if (mc.player != null) {
            ChatUtility.send(this.tr("available_list"));
         }

         for (Command command : PizzaDLCClient.getInstance().getCommandManager().getCommands()) {
            this.displayCommandInfo(command.getName(), Translations.tr(command.getDescription()), command.getAliases());
         }

         return 1;
      });
   }

   private void displayCommandInfo(String name, String description, List<String> aliases) {
      if (mc.player == null) return;

      MutableComponent nameText = Component.literal(name).withStyle(ChatFormatting.WHITE);
      MutableComponent descriptionText = Component.literal(" - " + description).withStyle(ChatFormatting.GRAY);
      MutableComponent fullText = nameText.append(descriptionText);
      this.display(fullText);
   }
}
