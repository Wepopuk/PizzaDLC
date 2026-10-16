package pizza.dlc.api.events.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import pizza.dlc.api.events.Event;

public class EventRender3D extends Event {
   PoseStack pose;
   SubmitNodeCollector collector;
   CameraRenderState camera;

   public EventRender3D(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      this.pose = pose;
      this.collector = collector;
      this.camera = camera;
   }

   public PoseStack getPose() {
      return this.pose;
   }

   public SubmitNodeCollector getCollector() {
      return this.collector;
   }

   public CameraRenderState getCamera() {
      return this.camera;
   }
}
