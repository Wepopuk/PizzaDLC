package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "Language",
   aliases = {"lang", "language"}
)
public class LanguageCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(context -> {
         CommandUsage.send(".lang", ".lang <en|ru|uk|pl|de>", ".lang en", ".lang ru", ".lang uk", ".lang pl", ".lang de");
         return 1;
      });
      builder.then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("language", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
         for (String lang : TranslationManager.getInstance().getAvailableLanguages()) {
            if (!lang.toLowerCase().startsWith(suggestionsBuilder.getRemaining().toLowerCase())) continue;

            suggestionsBuilder.suggest(lang);
         }

         return suggestionsBuilder.buildFuture();
      }).executes(context -> {
         String language = StringArgumentType.getString(context, "language");
         List<String> availableLangs = TranslationManager.getInstance().getAvailableLanguages();
         if (pizza.dlc.api.translation.ClientLanguage.byCode(language) != null) {
            if (TranslationManager.getInstance().setLanguage(language)) {
               pizza.dlc.api.translation.MinecraftLanguageSync.applySavedLanguage();
               ChatUtility.send(this.tr("set").formatted(language));
            }
            else ChatUtility.send(pizza.dlc.api.translation.Translations.tr("language.save_error"));
         } else {
            ChatUtility.send(this.tr("error_not_found").formatted(language));
            ChatUtility.send(this.tr("available_list").formatted(String.join(", ", availableLangs)));
         }

         return 1;
      }));
   }
}
