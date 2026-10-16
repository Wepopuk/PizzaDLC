package pizza.dlc.mixin.client;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.CommandManager;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin({CommandSuggestions.class})
public abstract class CommandSuggestionsMixin {
   @Shadow
   @Final
   Minecraft minecraft;
   @Shadow
   @Final
   EditBox input;
   @Shadow
   private ParseResults<?> currentParse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private boolean currentParseIsCommand;
   @Shadow
   private boolean currentParseIsMessage;
   @Unique
   private static final Set<String> pizzadlc$TAB_ALLOWLIST = Set.of("waypoints", "friend", "bind", "macro", "blockesp", "cfg", "lang", "gps", "neuro");

   @Shadow
   public abstract void showSuggestions(boolean narrateFirstSuggestion);

   @Inject(
      method = {"updateCommandInfo"},
      at = {@At("TAIL")}
   )
   public void refreshClientCommands(CallbackInfo ci) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (this.minecraft.player != null) {
         String text = this.input.getValue();
         int prefixLength = ".".length();
         if (text != null && text.length() >= prefixLength && text.startsWith(".")) {
            this.input.setSuggestion(null);
            StringReader stringReader = new StringReader(text);
            stringReader.setCursor(prefixLength);
            ParseResults<PizzaDLCCommandSource> parse = CommandManager.DISPATCHER.parse(stringReader, PizzaDLCCommandSource.INSTANCE);
            this.currentParse = parse;
            this.currentParseIsCommand = true;
            this.currentParseIsMessage = false;
            int cursor = this.input.getCursorPosition();
            if (cursor >= prefixLength) {
               boolean atRoot = text.substring(prefixLength, Math.min(cursor, text.length())).indexOf(32) < 0;
               this.pendingSuggestions = CommandManager.DISPATCHER.getCompletionSuggestions(parse, cursor);
               if (atRoot) {
                  this.pendingSuggestions = this.pendingSuggestions.thenApply(CommandSuggestionsMixin::pizzadlc$filterRoot);
               }

               this.pendingSuggestions.thenRun(() -> {
                  if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
                     this.showSuggestions(false);
                     this.input.setSuggestion(null);
                  }
               });
            }
         }
      }
   }

   @Unique
   private static Suggestions pizzadlc$filterRoot(Suggestions suggestions) {
      List<Suggestion> kept = new ArrayList<>();

      for (Suggestion suggestion : suggestions.getList()) {
         if (!pizzadlc$TAB_ALLOWLIST.contains(suggestion.getText())) continue;

         kept.add(suggestion);
      }

      return new Suggestions(suggestions.getRange(), kept);
   }
}
