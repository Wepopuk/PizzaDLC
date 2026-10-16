package pizza.dlc.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.client.command.api.CommandInfo;

public abstract class Command implements QuickImports {
   protected static final int SINGLE_SUCCESS = 1;
   private final String name;
   private final String description;
   private final List<String> aliases;
   private final CommandInfo info = this.getClass().getAnnotation(CommandInfo.class);

   public Command() {
      this.name = this.info.name();
      this.description = "commands.%s.description".formatted(this.name.toLowerCase().replace(" ", "_"));
      this.aliases = List.of(this.info.aliases());
   }

   protected static <T> RequiredArgumentBuilder<PizzaDLCCommandSource, T> arg(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   protected static LiteralArgumentBuilder<PizzaDLCCommandSource> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   public void registerTo(CommandDispatcher<PizzaDLCCommandSource> dispatcher) {
      Set<String> names = new LinkedHashSet<>();
      names.add(this.name);
      names.add(this.name.toLowerCase(Locale.ROOT));
      this.aliases.forEach(alias -> {
         names.add(alias);
         names.add(alias.toLowerCase(Locale.ROOT));
      });
      names.forEach(alias -> this.register(dispatcher, alias));
   }

   public void register(CommandDispatcher<PizzaDLCCommandSource> dispatcher, String name) {
      LiteralArgumentBuilder<PizzaDLCCommandSource> builder = LiteralArgumentBuilder.literal(name);
      this.build(builder);
      dispatcher.register(builder);
   }

   public String tr(String string) {
      return Translations.tr("commands.%s.%s".formatted(this.name.toLowerCase().replace(" ", "_"), string));
   }

   public void display(Component component) {
      if (mc.player == null) return;

      mc.player.sendSystemMessage(component);
   }

   public abstract void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder);

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public List<String> getAliases() {
      return this.aliases;
   }

   public CommandInfo getInfo() {
      return this.info;
   }
}
