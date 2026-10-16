package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.ClipEngine;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(
   name = "HClip",
   aliases = {"hclip"}
)
public class HClipCommand extends Command {
   @Override
   public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
      builder.executes(ctx -> {
         CommandUsage.send(".hclip <блоки: + вперёд / − назад>", ".hclip 10", ".hclip -10");
         return 1;
      });
      builder.then(
         arg("blocks", IntegerArgumentType.integer())
            .executes(
               ctx -> {
                  int n = IntegerArgumentType.getInteger(ctx, "blocks");
                  if (mc.player != null && n != 0) {
                     double yawRad = mc.player.getYRot() * (float) (Math.PI / 180.0);
                     double fx = -Math.sin(yawRad);
                     double fz = Math.cos(yawRad);
                     double s = Math.signum((float)n);
                     ClipEngine.INSTANCE.clip(fx * s, 0.0, fz * s, Math.abs(n));
                     ChatUtility.send(
                        ChatFormatting.GRAY
                           + "HClip "
                           + ChatFormatting.WHITE
                           + Math.abs(n)
                           + ChatFormatting.GRAY
                           + (n > 0 ? " вперёд" : " назад")
                           + " — нужен серверный /fly или креатив"
                     );
                     return 1;
                  }

                  return 1;
               }
            )
      );
   }
}
