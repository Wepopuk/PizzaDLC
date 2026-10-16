package pizza.dlc.api.interfaces;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public interface QuickImports {
   Minecraft mc = Minecraft.getInstance();
   Window window = mc.getWindow();
}
