package pizza.dlc.api.ui.menu;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

/** Main options page; native controls retain their callbacks, permission checks and tooltips. */
public final class ClientSettingsScreen extends OptionsSubScreen {
   private static final Set<String> SIDEBAR_LINKS = Set.of("options.skinCustomisation", "options.sounds", "options.video",
      "options.controls", "options.language", "options.chat", "options.resourcepack", "options.accessibility");
   private final OptionsScreen source;

   public ClientSettingsScreen(OptionsScreen source) {
      super(source.getLastScreen(), Minecraft.getInstance().options, source.getTitle());
      this.source = source;
   }

   @Override
   protected void addOptions() {
      this.source.init(this.width, this.height);
      for (var child : this.source.children()) {
         if (!(child instanceof AbstractWidget widget)) continue;
         if (widget instanceof AbstractSliderButton) {
            // Main-menu FOV must keep its OptionInstance so closing applies pending changes.
            this.list.addBig(this.options.fov());
         } else if (widget instanceof CycleButton<?>) {
            this.list.addBig(widget);
         } else if (widget instanceof AbstractButton) {
            if (widget.getMessage().equals(CommonComponents.GUI_DONE)) continue;
            if (widget.getMessage().getContents() instanceof TranslatableContents text && SIDEBAR_LINKS.contains(text.getKey())) continue;
            this.list.addBig(widget);
         }
      }
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      if (CleanRenderScope.isActive()) {
         ScreenFrame.withLayout(true, () -> {
            this.source.resize(this.width, this.height);
            this.source.extractRenderState(g, mouseX, mouseY, partialTick);
         });
      } else super.extractRenderState(g, mouseX, mouseY, partialTick);
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return ScreenFrame.isVanillaLayout() ? this.source.mouseClicked(event, doubleClick) : super.mouseClicked(event, doubleClick);
   }
   @Override public boolean mouseReleased(MouseButtonEvent event) {
      return ScreenFrame.isVanillaLayout() ? this.source.mouseReleased(event) : super.mouseReleased(event);
   }
   @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      return ScreenFrame.isVanillaLayout() ? this.source.mouseDragged(event, dx, dy) : super.mouseDragged(event, dx, dy);
   }
   @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
      return ScreenFrame.isVanillaLayout() ? this.source.mouseScrolled(x, y, dx, dy) : super.mouseScrolled(x, y, dx, dy);
   }
   @Override public boolean keyPressed(KeyEvent event) {
      return ScreenFrame.isVanillaLayout() ? this.source.keyPressed(event) : super.keyPressed(event);
   }
}
