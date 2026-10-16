package meteordevelopment.orbit;
public interface ICancellable
{
void setCancelled(boolean paramBoolean);
default void cancel() {
setCancelled(true);
}
boolean isCancelled();
}

