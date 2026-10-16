package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.client.modules.impl.misc.Tweaks;

@Mixin({Hud.class})
public class HudScoreboardMixin {
   @ModifyExpressionValue(
      method = {"displayScoreboardSidebar"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"
      )}
   )
   private NumberFormat pizzadlc$blankScoreboardNumbers(NumberFormat original) {
      return (NumberFormat)(Tweaks.hideScoreboardNumbers() ? BlankFormat.INSTANCE : original);
   }
}
