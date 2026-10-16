package meteordevelopment.orbit.listeners;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import meteordevelopment.orbit.EventHandler;

public class LambdaListener
implements IListener
{
private static boolean isJava1dot8;
private static Constructor<MethodHandles.Lookup> lookupConstructor;
private static Method privateLookupInMethod;
private final Class<?> target;
private final boolean isStatic;
private final int priority;
private Consumer<Object> executor;
public LambdaListener(Factory factory, Class<?> klass, Object object, Method method) {
this.target = method.getParameters()[0].getType();
this.isStatic = Modifier.isStatic(method.getModifiers());
this.priority = ((EventHandler)method.<EventHandler>getAnnotation(EventHandler.class)).priority(); try {
MethodHandles.Lookup lookup; MethodHandle methodHandle;
MethodType invokedType;
String name = method.getName();
if (isJava1dot8) {
boolean a = lookupConstructor.isAccessible();
lookupConstructor.setAccessible(true);
lookup = lookupConstructor.newInstance(new Object[] { klass });
lookupConstructor.setAccessible(a);
} else {
lookup = factory.create(privateLookupInMethod, klass);
}
MethodType methodType = MethodType.methodType(void.class, method.getParameters()[0].getType());
if (this.isStatic) {
methodHandle = lookup.findStatic(klass, name, methodType);
invokedType = MethodType.methodType(Consumer.class);
} else {
methodHandle = lookup.findVirtual(klass, name, methodType);
invokedType = MethodType.methodType(Consumer.class, klass);
}
MethodHandle lambdaFactory = LambdaMetafactory.metafactory(lookup, "accept", invokedType, MethodType.methodType(void.class, Object.class), methodHandle, methodType).getTarget();
if (this.isStatic) { this.executor = (Consumer<Object>)lambdaFactory.invoke(); }
else { this.executor = (Consumer<Object>)lambdaFactory.invoke(object); }
} catch (Throwable throwable) {
throwable.printStackTrace();
}
}
public void call(Object event) {
this.executor.accept(event);
}
public Class<?> getTarget() {
return this.target;
}
public int getPriority() {
return this.priority;
}
public boolean isStatic() {
return this.isStatic;
}
static {
try {
isJava1dot8 = System.getProperty("java.version").startsWith("1.8");
if (isJava1dot8) {
lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(new Class[] { Class.class });
} else {
privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn", new Class[] { Class.class, MethodHandles.Lookup.class });
}
} catch (NoSuchMethodException e) {
e.printStackTrace();
}
}
public static interface Factory {
MethodHandles.Lookup create(Method param1Method, Class<?> param1Class) throws InvocationTargetException, IllegalAccessException;
}
}

