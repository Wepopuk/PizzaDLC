package pizza.dlc.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import meteordevelopment.orbit.EventHandler;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventMessage;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.commands.BindCommand;
import pizza.dlc.client.command.commands.BlockESPCommand;
import pizza.dlc.client.command.commands.ConfigCommand;
import pizza.dlc.client.command.commands.FriendCommand;
import pizza.dlc.client.command.commands.HClipCommand;
import pizza.dlc.client.command.commands.HelpCommand;
import pizza.dlc.client.command.commands.LanguageCommand;
import pizza.dlc.client.command.commands.MacrosCommand;
import pizza.dlc.client.command.commands.VClipCommand;
import pizza.dlc.client.command.commands.TPCommand;
import pizza.dlc.client.command.commands.WaypointsCommand;
import pizza.dlc.client.command.commands.ItemFinderCommand;

public class CommandManager {
   public static final CommandDispatcher<PizzaDLCCommandSource> DISPATCHER = new CommandDispatcher();
   public static final String PREFIX = ".";
   private final List<Command> commands = new ArrayList<>();

   public CommandManager() {
      PizzaDLC.getInstance().getBus().subscribe(this);
   }

   public CommandManager init() {
      this.register(
         new HelpCommand(),
         new pizza.dlc.client.command.commands.NeuroCommand(),
         new LanguageCommand(),
         new ConfigCommand(),
         new FriendCommand(),
         new MacrosCommand(),
         new BindCommand(),
         new WaypointsCommand(),
         new pizza.dlc.client.command.commands.GpsCommand(),
         new BlockESPCommand(),
         new VClipCommand(),
         new HClipCommand(),
         new TPCommand(),
         new ItemFinderCommand()
      );
      return this;
   }

   private void register(@NonNull Command... commands) {
      if (commands == null) {
         throw new NullPointerException("commands is marked non-null but is null");
      }

      for (Command command : commands) {
         command.registerTo(DISPATCHER);
         PizzaDLC.getInstance().getBus().subscribe(command);
         this.commands.add(command);
      }
   }

   public <T extends Command> T get(String name) {
      return (T)this.commands.stream().filter(command -> command.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public <T extends Command> T get(Class<T> clazz) {
      return (T)this.commands.stream().filter(command -> command.getClass().equals(clazz)).findFirst().orElse(null);
   }

   @EventHandler
   public void onMessage(EventMessage event) {
      String message = event.getMessage();
      if (message.startsWith(PREFIX)) {
         try {
            DISPATCHER.execute(message.substring(PREFIX.length()), PizzaDLCCommandSource.INSTANCE);
         } catch (CommandSyntaxException error) {
            ChatUtility.send(message.toLowerCase(java.util.Locale.ROOT).startsWith(".neuro") ? "Neuro: "+error.getRawMessage().getString()+"; .neuro help" : Translations.tr("commands.global.error"));
         }

         event.setCancelled(true);
      }
   }

   public List<Command> getCommands() {
      return this.commands;
   }
}
