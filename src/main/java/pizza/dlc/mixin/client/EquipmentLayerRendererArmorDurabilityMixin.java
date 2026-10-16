package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.api.util.render.ArmorTint;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.ArmorDurability;

@Mixin({EquipmentLayerRenderer.class})
public class EquipmentLayerRendererArmorDurabilityMixin {
   @ModifyExpressionValue(
      method = {"renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I"
      )}
   )
   private int pizzadlc$tintByDurability(int origColor, @Local(argsOnly = true) LayerType layerType, @Local(argsOnly = true) ItemStack stack) {
      ArmorDurability m = module();
      if (m == null || !m.isEnabled()) {
         return origColor;
      }

      if (layerType != LayerType.HUMANOID && layerType != LayerType.HUMANOID_LEGGINGS && layerType != LayerType.HUMANOID_BABY) {
         return origColor;
      }

      return stack.isDamageableItem() && stack.getMaxDamage() != 0
         ? ArmorTint.multiply(origColor, ArmorTint.tintArgb(ArmorTint.durabilityFraction(stack)))
         : origColor;
   }

   private static ArmorDurability module() {
      PizzaDLCClient c = PizzaDLCClient.getInstance();
      return c != null && c.getModuleManager() != null ? c.getModuleManager().get(ArmorDurability.class) : null;
   }
}
