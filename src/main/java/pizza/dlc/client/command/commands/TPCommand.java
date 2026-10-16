package pizza.dlc.client.command.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import pizza.dlc.client.command.PizzaDLCCommandSource;
import pizza.dlc.client.command.Command;
import pizza.dlc.client.command.CommandUsage;
import pizza.dlc.client.command.VoidTeleport;
import pizza.dlc.client.command.api.CommandInfo;

@CommandInfo(name = "TP", aliases = {"tp", "voidtp"})
public final class TPCommand extends Command {
    @Override
    public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> builder) {
        builder.executes(ctx -> {
            CommandUsage.send(".tp <x> <y> <z>", ".tp 100 64 -200", ".tp cancel");
            return SINGLE_SUCCESS;
        });
        builder.then(literal("cancel").executes(ctx -> {
            VoidTeleport.get().cancel();
            return SINGLE_SUCCESS;
        }));
        builder.then(arg("x", DoubleArgumentType.doubleArg(-30000000.0, 30000000.0))
                .then(arg("y", DoubleArgumentType.doubleArg())
                        .then(arg("z", DoubleArgumentType.doubleArg(-30000000.0, 30000000.0))
                                .executes(ctx -> {
                                    if (mc.player != null && mc.level != null) {
                                        VoidTeleport.get().start(mc.player, mc.level,
                                                DoubleArgumentType.getDouble(ctx, "x"),
                                                DoubleArgumentType.getDouble(ctx, "y"),
                                                DoubleArgumentType.getDouble(ctx, "z"));
                                    }
                                    return SINGLE_SUCCESS;
                                }))));
    }
}