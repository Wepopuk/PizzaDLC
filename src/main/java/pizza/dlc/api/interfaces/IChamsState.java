package pizza.dlc.api.interfaces;

public interface IChamsState {
   int pizzadlc$getChamsSolid();

   int pizzadlc$getChamsGlow();

   boolean pizzadlc$isChamsHideSkin();

   void pizzadlc$setChams(int solidColor, int glowColor, boolean hideSkin);
}
