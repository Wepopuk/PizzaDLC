package pizza.dlc.client.modules.impl.visuals.animatedhands;

public interface HmiSwing {
   int pizzadlc$hmiSwingCount();

   void pizzadlc$hmiResetMainSwing(boolean interact);

   void pizzadlc$hmiResetOffSwing(boolean interact);

   float pizzadlc$hmiMainSwing(float partialTick);

   float pizzadlc$hmiOffSwing(float partialTick);

   boolean pizzadlc$hmiMainInteract();

   boolean pizzadlc$hmiOffInteract();

   boolean pizzadlc$hmiBlockBreaking();

   boolean pizzadlc$hmiMainEvent();

   boolean pizzadlc$hmiOffEvent();
}
