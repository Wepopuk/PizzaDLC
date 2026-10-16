package meteordevelopment.orbit.listeners;
import java.util.function.Consumer;

public class ConsumerListener<T>
implements IListener
{
private final Class<T> target;
private final int priority;
private final Consumer<T> executor;
public ConsumerListener(Class<T> target, int priority, Consumer<T> executor) {
this.target = target;
this.priority = priority;
this.executor = executor;
}
public ConsumerListener(Class<T> target, Consumer<T> executor) {
this(target, 0, executor);
}
public void call(Object event) {
this.executor.accept((T)event);
}
public Class<T> getTarget() {
return this.target;
}
public int getPriority() {
return this.priority;
}
public boolean isStatic() {
return false;
}
}

