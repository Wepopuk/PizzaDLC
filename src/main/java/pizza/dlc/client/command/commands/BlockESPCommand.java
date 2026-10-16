package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;
import pizza.dlc.client.modules.impl.visuals.BlockEsp;

@CommandInfo(
   name = "BlockESP",
   aliases = {"blockesp", "besp"}
)
public class BlockESPCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(
         context -> {
            CommandUsage.send(
               ".blockesp", ".blockesp add <block>", ".blockesp remove <block>", ".blockesp list", ".blockesp clear", ".blockesp seed <значение|clear>"
            );
            return 1;
         }
      );
      builder.then(literal("add").then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("block", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
         String remaining = suggestionsBuilder.getRemaining().toLowerCase();

         for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            String path = id.getPath();
            String full = id.toString();
            if (path.startsWith(remaining) || full.startsWith(remaining)) {
               suggestionsBuilder.suggest(path);
            }
         }

         return suggestionsBuilder.buildFuture();
      }).executes(context -> {
         String blockName = StringArgumentType.getString(context, "block");
         Block block = this.blockFromName(blockName);
         if (block != null && block != Blocks.AIR) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
            if (module.addBlock(block)) {
               ChatUtility.send(ChatFormatting.GRAY + "Added: " + ChatFormatting.WHITE + id);
            } else {
               ChatUtility.sendError("Already in list: " + id);
            }

            return 1;
         }

         ChatUtility.sendError("Block not found: " + blockName);
         return 1;
      })));
      builder.then(literal("remove").then(RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("block", StringArgumentType.string()).suggests((context, suggestionsBuilder) -> {
         BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
         String remaining = suggestionsBuilder.getRemaining().toLowerCase();

         for (Block block : module.getBlockList()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && (id.getPath().startsWith(remaining) || id.toString().startsWith(remaining))) {
               suggestionsBuilder.suggest(id.getPath());
            }
         }

         return suggestionsBuilder.buildFuture();
      }).executes(context -> {
         String blockName = StringArgumentType.getString(context, "block");
         Block block = this.blockFromName(blockName);
         Identifier id = block == null ? Identifier.tryParse(blockName) : BuiltInRegistries.BLOCK.getKey(block);
         BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
         if (module.removeBlock(block)) {
            ChatUtility.send(ChatFormatting.GRAY + "Removed: " + ChatFormatting.WHITE + id);
         } else {
            ChatUtility.sendError("Not in list: " + id);
         }

         return 1;
      })));
      builder.then(literal("list").executes(context -> {
         BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
         if (module.getBlockList().isEmpty()) {
            ChatUtility.send(ChatFormatting.GRAY + "Block list is empty.");
            return 1;
         }

         StringBuilder blocks = new StringBuilder();

         for (Block block : module.getBlockList()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) {
               blocks.append(id.getPath()).append(", ");
            }
         }

         if (blocks.length() >= 2) {
            blocks.setLength(blocks.length() - 2);
         }

         ChatUtility.send(ChatFormatting.GRAY + "Blocks (" + module.getBlockList().size() + "): " + ChatFormatting.WHITE + blocks);
         return 1;
      }));
      builder.then(literal("clear").executes(context -> {
         BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
         if (module.getBlockList().isEmpty()) {
            ChatUtility.send(ChatFormatting.GRAY + "Block list is already empty.");
         } else {
            int count = module.getBlockList().size();
            module.clearBlocks();
            ChatUtility.send(ChatFormatting.GRAY + "Cleared " + ChatFormatting.WHITE + count + ChatFormatting.GRAY + " blocks.");
         }

         return 1;
      }));
      builder.then(
         (literal("seed")
            .executes(
               context -> {
                  BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
                  if (module.hasSeed()) {
                     ChatUtility.send(
                        ChatFormatting.GRAY
                           + "Сид: "
                           + ChatFormatting.WHITE
                           + module.getSeed()
                           + ChatFormatting.GRAY
                           + (module.isSeedMode() ? " (режим вкл)" : " — включи «Работать по сиду»")
                     );
                  } else {
                     ChatUtility.send(ChatFormatting.GRAY + "Сид не задан. " + ChatFormatting.WHITE + ".blockesp seed <значение>");
                  }

                  return 1;
               }
            ))
            .then(
               RequiredArgumentBuilder.<PizzaDLCCommandSource, String>argument("value", StringArgumentType.greedyString())
                  .executes(
                     context -> {
                        String raw = StringArgumentType.getString(context, "value").trim();
                        BlockEsp module = PizzaDLCClient.getInstance().getModuleManager().get(BlockEsp.class);
                        if (raw.equalsIgnoreCase("clear")) {
                           module.clearSeed();
                           ChatUtility.send(ChatFormatting.GRAY + "Сид очищен.");
                           return 1;
                        }

                        long parsed;
                        try {
                           parsed = Long.parseLong(raw);
                        } catch (NumberFormatException ignored) {
                           parsed = raw.hashCode();
                        }

                        module.setSeed(parsed);
                        ChatUtility.send(
                           ChatFormatting.GRAY
                              + "Сид установлен: "
                              + ChatFormatting.WHITE
                              + parsed
                              + ChatFormatting.GRAY
                              + ". Включи «Работать по сиду» в Block ESP."
                        );
                        return 1;
                     }
                  )
            )
      );
   }

   private Block blockFromName(String blockName) {
      Identifier id = Identifier.tryParse(blockName);
      if (id == null && !blockName.contains(":")) {
         id = Identifier.withDefaultNamespace(blockName);
      }

      return id == null ? null : (Block)BuiltInRegistries.BLOCK.getValue(id);
   }
}
