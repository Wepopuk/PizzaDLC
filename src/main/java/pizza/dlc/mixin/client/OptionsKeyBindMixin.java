package pizza.dlc.mixin.client;

import java.io.File;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping.Category;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.editor.ItemEditorScreen;

@Mixin({Options.class})
public class OptionsKeyBindMixin {
   @Shadow
   @Final
   @Mutable
   public KeyMapping[] keyMappings;

   @Inject(
      method = {"<init>"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Options;load()V"
      )}
   )
   private void pizzadlc$addEditorBind(Minecraft minecraft, File workingDirectory, CallbackInfo ci) {
      KeyMapping km = new KeyMapping("key.pizzadlc.item_editor", 71, Category.MISC);
      ItemEditorScreen.EDITOR_BIND = km;
      KeyMapping[] arr = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 1);
      arr[arr.length - 1] = km;
      this.keyMappings = arr;
   }
}
