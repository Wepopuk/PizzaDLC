package zako.opt.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import zako.opt.ZakoOptConfig;
import zako.opt.text.StableText;

public final class PreparedTextCache {
    private static final Font.PreparedText UNCACHEABLE = new Font.PreparedText(){

        public void visit(Font.GlyphVisitor visitor) {
        }

        public ScreenRectangle bounds() {
            return null;
        }
    };
    private static final Map<Key, Font.PreparedText> CACHE = new LinkedHashMap<Key, Font.PreparedText>(512, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Font.PreparedText> eldest) {
            return this.size() > 2048;
        }
    };
    private static final Set<FormattedCharSequence> STABLE = Collections.newSetFromMap(new WeakHashMap());

    private PreparedTextCache() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static FormattedCharSequence markStable(FormattedCharSequence text) {
        StableText stable = new StableText(text);
        Set<FormattedCharSequence> set = STABLE;
        synchronized (set) {
            STABLE.add(stable);
        }
        return stable;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isStable(FormattedCharSequence text) {
        if (ZakoOptConfig.microOpts2()) {
            return text instanceof StableText;
        }
        Set<FormattedCharSequence> set = STABLE;
        synchronized (set) {
            return STABLE.contains(text);
        }
    }

    public static boolean enabled() {
        return ZakoOptConfig.preparedTextCache();
    }

    public static Key key(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
        return new Key(text, x, y, color, shadow, includeEmpty, background);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Font.PreparedText get(Key key) {
        Map<Key, Font.PreparedText> map = CACHE;
        synchronized (map) {
            Font.PreparedText cached = CACHE.get(key);
            return cached == UNCACHEABLE ? null : cached;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean known(Key key) {
        Map<Key, Font.PreparedText> map = CACHE;
        synchronized (map) {
            return CACHE.containsKey(key);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void put(Key key, Font.PreparedText prepared) {
        boolean plain = key.text.accept((index, style, codePoint) -> !style.isObfuscated());
        Map<Key, Font.PreparedText> map = CACHE;
        synchronized (map) {
            CACHE.put(key, plain ? prepared : UNCACHEABLE);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear() {
        Map<Key, Font.PreparedText> map = CACHE;
        synchronized (map) {
            CACHE.clear();
        }
    }

    public record Key(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key)o;
            if (k.text != this.text) return false;
            if (k.x != this.x) return false;
            if (k.y != this.y) return false;
            if (k.color != this.color) return false;
            if (k.shadow != this.shadow) return false;
            if (k.includeEmpty != this.includeEmpty) return false;
            if (k.background != this.background) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int h = System.identityHashCode(this.text);
            h = 31 * h + Float.floatToIntBits(this.x);
            h = 31 * h + Float.floatToIntBits(this.y);
            h = 31 * h + this.color;
            h = 31 * h + this.background;
            return 31 * h + (this.shadow ? 1 : 0) + (this.includeEmpty ? 2 : 0);
        }
    }
}

