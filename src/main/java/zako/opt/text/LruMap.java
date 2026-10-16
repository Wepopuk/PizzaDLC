package zako.opt.text;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LruMap<K, V>
extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LruMap(int maxSize) {
        super(256, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() > this.maxSize;
    }
}

