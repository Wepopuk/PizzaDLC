package pizza.dlc.client.command.commands;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import pizza.dlc.api.neuro.NeuroSystem;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.command.*;
import pizza.dlc.client.command.api.CommandInfo;
@CommandInfo(name="Neuro",aliases={"neuro"})
public final class NeuroCommand extends Command {
 private int run(Runnable task){try{task.run();return 1;}catch(IllegalArgumentException|IllegalStateException e){ChatUtility.sendError("Neuro: "+e.getMessage());return 0;}}
 private NeuroSystem system(){if(NeuroSystem.get()==null)throw new IllegalStateException("Система ещё не инициализирована");return NeuroSystem.get();}
 private int help(){
  NeuroSystem.say(".neuro bots spawn [1–8] | bots remove; попадания по ботам локальные.");
  NeuroSystem.say(".neuro train start [имя] → ручной бой с ходьбой и прыжками → train stop → model select <имя> → включить Attack Aura (Neuro)");
  NeuroSystem.say(".neuro model list | status. Настройки: NeuroTrainer. Сбор и применение взаимоисключающие.");
  NeuroSystem.say("Сравнение: .neuro eval start manual|model [seed] → eval stop. Повтори тот же seed из той же позиции.");return 1;
 }
 @Override public void build(LiteralArgumentBuilder<PizzaDLCCommandSource> b){
  b.executes(c->help()).then(literal("help").executes(c->help()));
  b.then(literal("status").executes(c->run(()->system().status())));
  b.then(literal("bots")
   .then(literal("spawn").executes(c->run(()->system().spawn(1)))
    .then(arg("count",IntegerArgumentType.integer(1,8)).executes(c->run(()->system().spawn(IntegerArgumentType.getInteger(c,"count"))))))
   .then(literal("remove").executes(c->run(()->system().remove()))));
  b.then(literal("train")
   .then(literal("start").executes(c->run(()->system().train(null)))
    .then(arg("name",StringArgumentType.word()).executes(c->run(()->system().train(StringArgumentType.getString(c,"name"))))))
   .then(literal("stop").executes(c->run(()->system().stopTraining()))));
  b.then(literal("model").then(literal("list").executes(c->run(()->system().list())))
   .then(literal("select").then(arg("name",StringArgumentType.word()).executes(c->run(()->system().load(StringArgumentType.getString(c,"name"))))))
   .then(literal("load").then(arg("name",StringArgumentType.word()).executes(c->run(()->system().load(StringArgumentType.getString(c,"name")))))));
  b.then(literal("use").then(literal("on").executes(c->run(()->system().use(true)))).then(literal("off").executes(c->run(()->system().use(false)))));
  var start=literal("start");
  for(String mode:new String[]{"manual","model"})start.then(literal(mode).executes(c->run(()->system().evaluate(mode,731)))
   .then(arg("seed",LongArgumentType.longArg()).executes(c->run(()->system().evaluate(mode,LongArgumentType.getLong(c,"seed"))))));
  b.then(literal("eval").then(start).then(literal("stop").executes(c->run(()->system().stopEvaluation()))));
 }
}
