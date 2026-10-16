package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.ClipEngine;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "VClip",
   aliases = {"vclip"}
)
public class VClipCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(ctx -> {
         CommandUsage.send(".vclip <блоки> up|down", ".vclip 400 up", ".vclip 400 down");
         return 1;
      });
      builder.then(
         ((RequiredArgumentBuilder)((RequiredArgumentBuilder)arg("blocks", IntegerArgumentType.integer(1))
            .then(literal("up").executes(ctx -> this.run(IntegerArgumentType.getInteger(ctx, "blocks"), 1))))
         .then(literal("down").executes(ctx -> this.run(IntegerArgumentType.getInteger(ctx, "blocks"), -1))))
            .executes(ctx -> this.run(IntegerArgumentType.getInteger(ctx, "blocks"), 1))
      );
   }

   private int run(int blocks, int sign) {
      if (mc.player == null) {
         return 1;
      }

      ClipEngine.INSTANCE.clip(0.0, sign, 0.0, blocks);
      ChatUtility.send(
         ChatFormatting.GRAY
            + "VClip "
            + ChatFormatting.WHITE
            + blocks
            + ChatFormatting.GRAY
            + (sign > 0 ? " вверх" : " вниз")
            + " — нужен серверный /fly или креатив"
      );
      return 1;
   }
}
