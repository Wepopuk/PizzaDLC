package meteordevelopment.orbit;

import meteordevelopment.orbit.listeners.IListener;
import meteordevelopment.orbit.listeners.LambdaListener;

public interface IEventBus {
  void registerLambdaFactory(String paramString, LambdaListener.Factory paramFactory);
  
  boolean isListening(Class<?> paramClass);
  
  <T> T post(T paramT);
  
  <T extends ICancellable> T post(T paramT);
  
  void subscribe(Object paramObject);
  
  void subscribe(Class<?> paramClass);
  
  void subscribe(IListener paramIListener);
  
  void unsubscribe(Object paramObject);
  
  void unsubscribe(Class<?> paramClass);
  
  void unsubscribe(IListener paramIListener);
}

