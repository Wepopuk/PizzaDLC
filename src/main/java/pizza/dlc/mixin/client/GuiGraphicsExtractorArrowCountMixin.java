package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

@Mixin({GuiGraphicsExtractor.class})
public class GuiGraphicsExtractorArrowCountMixin {
   @Inject(
      method = {"itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$arrowCount(Font vanillaFont, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
      if (pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive()) return;
      if (stack.getItem() instanceof ProjectileWeaponItem) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            GuiGraphicsExtractor ctx = (GuiGraphicsExtractor)(Object)this;
            ItemStack arrow = mc.player.getProjectile(stack);
            boolean creative = mc.player.getAbilities().instabuild;
            String txt;
            int color;
            if (creative) {
               txt = "Inf";
               color = 0xFF55FF55;
            } else {
               int count = arrow.isEmpty() ? 0 : countArrows(mc.player, arrow);
               txt = String.valueOf(count);
               color = count == 0 ? 0xFFFF5555 : -1;
            }

            ItemStack icon = arrow.isEmpty() ? new ItemStack(Items.ARROW) : arrow;
            MsdfFont font = PizzaDLCFonts.sfpro();
            float size = 6.0F;
            float tw = Render2DUtil.msdfWidth(font, txt, size);
            float txX = x + 16.0F - tw - 1.0F;
            float txY = y + 1.0F;
            Render2DUtil.msdfText(ctx, font, txt, txX + 0.5F, txY + 0.5F, size, 0xFF000000);
            Render2DUtil.msdfText(ctx, font, txt, txX, txY, size, color);
            ctx.pose().pushMatrix();
            ctx.pose().translate(x + 7.0F, y + 7.0F);
            ctx.pose().scale(0.5F);
            ctx.item(icon, 0, 0);
            ctx.pose().popMatrix();
         }
      }
   }

   private static int countArrows(Player p, ItemStack arrow) {
      int c = 0;
      Inventory inv = p.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (!s.isEmpty() && s.getItem() == arrow.getItem()) {
            c += s.getCount();
         }
      }

      return c;
   }
}
