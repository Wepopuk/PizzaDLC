package meteordevelopment.orbit.listeners;

public interface IListener {
  void call(Object paramObject);
  
  Class<?> getTarget();
  
  int getPriority();
  
  @Deprecated
  boolean isStatic();
}

