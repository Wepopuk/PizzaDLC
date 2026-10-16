package zako.opt.block;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

public final class SpawnerReplay {
    public static final double DISTANCE = 6.0;
    private static final Map<Object, Recording> CACHE = new IdentityHashMap<Object, Recording>();
    private static long cachedTick = Long.MIN_VALUE;
    private static final PoseStack SCRATCH = new PoseStack();

    private SpawnerReplay() {
    }

    public static void submit(Object key, long tick, Vec3 camera, SubmitNodeCollector collector, Consumer<SubmitNodeCollector> body) {
        Recording recording;
        if (tick != cachedTick) {
            CACHE.clear();
            cachedTick = tick;
        }
        if ((recording = CACHE.get(key)) == null) {
            recording = new Recording(camera);
            CACHE.put(key, recording);
            body.accept((SubmitNodeCollector)SpawnerReplay.recorder(SubmitNodeCollector.class, collector, recording, null, -1));
        } else if (!recording.replayable) {
            body.accept(collector);
        } else {
            SpawnerReplay.replay(recording, camera, collector);
        }
    }

    private static Object recorder(Class<?> type, Object target, Recording recording, Method order, int orderIndex) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.getName().equals("equals") ? Boolean.valueOf(proxy == args[0]) : method.invoke(target, args);
            }
            Object result = SpawnerReplay.invoke(method, target, args);
            if (method.getReturnType() == OrderedSubmitNodeCollector.class && order == null) {
                return SpawnerReplay.recorder(OrderedSubmitNodeCollector.class, result, recording, method, (Integer)args[0]);
            }
            if (method.getReturnType() == Void.TYPE) {
                SpawnerReplay.record(recording, order, orderIndex, method, args);
            } else {
                recording.replayable = false;
            }
            return result;
        });
    }

    private static void record(Recording recording, Method order, int orderIndex, Method method, Object[] args) {
        Object[] objectArray;
        Object[] stored = args == null ? null : (Object[])args.clone();
        PoseStack.Pose pose = null;
        for (int i = 0; stored != null && i < stored.length; ++i) {
            Object object = stored[i];
            if (!(object instanceof PoseStack)) continue;
            PoseStack stack = (PoseStack)object;
            if (pose != null) {
                recording.replayable = false;
            }
            pose = stack.last().copy();
            stored[i] = SCRATCH;
        }
        List<Call> list = recording.calls;
        if (order == null) {
            objectArray = null;
        } else {
            Object[] objectArray2 = new Object[1];
            objectArray = objectArray2;
            objectArray2[0] = orderIndex;
        }
        list.add(new Call(order, objectArray, method, stored, pose));
    }

    private static void replay(Recording recording, Vec3 camera, SubmitNodeCollector collector) {
        float dx = (float)(recording.x - camera.x);
        float dy = (float)(recording.y - camera.y);
        float dz = (float)(recording.z - camera.z);
        for (Call call : recording.calls) {
            if (call.pose != null) {
                SCRATCH.last().set(call.pose);
                SCRATCH.last().pose().translateLocal(dx, dy, dz);
            }
            SubmitNodeCollector target = call.order == null ? collector : (SubmitNodeCollector)SpawnerReplay.invoke(call.order, collector, call.orderArgs);
            SpawnerReplay.invoke(call.method, target, call.args);
        }
    }

    private static Object invoke(Method method, Object target, Object[] args) {
        try {
            return method.invoke(target, args);
        }
        catch (InvocationTargetException e) {
            RuntimeException r;
            Throwable throwable = e.getCause();
            throw throwable instanceof RuntimeException ? (r = (RuntimeException)throwable) : new RuntimeException(e.getCause());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class Recording {
        final List<Call> calls = new ArrayList<Call>();
        final double x;
        final double y;
        final double z;
        boolean replayable = true;

        Recording(Vec3 camera) {
            this.x = camera.x;
            this.y = camera.y;
            this.z = camera.z;
        }
    }

    private record Call(Method order, Object[] orderArgs, Method method, Object[] args, PoseStack.Pose pose) {
    }
}

