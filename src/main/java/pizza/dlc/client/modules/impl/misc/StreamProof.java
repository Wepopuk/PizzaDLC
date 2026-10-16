package pizza.dlc.client.modules.impl.misc;

import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name = "StreamProof", category = ModuleCategory.OTHER,
   description = "Второе окно Minecraft без визуалов клиента, с управлением для игры и записи")
public class StreamProof extends Module {
   public final SliderSetting captureFps = new SliderSetting("FPS записи", this, 30, 10, 360, 1);
   public final BooleanSetting oneWindow = new BooleanSetting("Одно окно",
      "Экспериментальный режим Windows: чистый захват и клиент поверх, только оконный режим", this);
   public final BooleanSetting auraCamera = new BooleanSetting("Повороты Aura", "Показывать наведение на цель в чистом окне", this);

   public StreamProof() {
      this.auraCamera.setEnabled(true);
   }

   @Override
   public void onDisable() {
      super.onDisable();
      StreamProofRenderer.close();
   }

   public static StreamProof get() {
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      return client == null || client.getModuleManager() == null ? null : client.getModuleManager().get(StreamProof.class);
   }
}
