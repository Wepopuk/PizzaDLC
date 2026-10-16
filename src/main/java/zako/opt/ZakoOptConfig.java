package zako.opt;

// Ported: values come from the PizzaDLC "ZakoOpt" module instead of config/zakoopt.json + system properties.
public final class ZakoOptConfig {
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static boolean guiAnimatedItems;
    private static boolean lazyClear;
    private static boolean noEntitySort;
    private static boolean bidiCache;
    private static boolean preparedTextCache;
    private static boolean itemLod;
    private static int itemLodDistance = 8;
    private static boolean entityLod;
    private static int entityLodDistance = 24;
    private static boolean particleLight;
    private static boolean particlePhysics;
    private static boolean particleLod;
    private static boolean parallelParticles;
    private static boolean spawnerCull;
    private static int spawnerDistance = 16;
    private static boolean spawnerTickCache;
    private static boolean spawnerReplay;
    private static boolean blockEntityCache;
    private static boolean lookupCaches;
    private static boolean microOpts;

    private ZakoOptConfig() {
    }

    public static void apply(Values v) {
        boolean on = v != null && v.enabled;
        guiAnimatedItems = on && v.guiAnimatedItems;
        lazyClear = on && v.lazyClear;
        noEntitySort = on && v.noEntitySort;
        bidiCache = on && v.bidiCache;
        preparedTextCache = on && v.preparedTextCache;
        itemLod = on && v.itemLod;
        entityLod = on && v.entityLod;
        particleLight = on && v.particleLight;
        particlePhysics = on && v.particlePhysics;
        particleLod = on && v.particleLod;
        parallelParticles = on && v.parallelParticles && CORES >= 4;
        spawnerCull = on && v.spawnerCull;
        spawnerTickCache = on && v.spawnerTickCache;
        spawnerReplay = on && v.spawnerReplay;
        blockEntityCache = on && v.blockEntityCache;
        lookupCaches = on && v.lookupCaches;
        microOpts = on && v.microOpts;
        if (v != null) {
            itemLodDistance = v.itemLodDistance;
            entityLodDistance = v.entityLodDistance;
            spawnerDistance = v.spawnerDistance;
        }
    }

    public static boolean guiAnimatedItems() {
        return guiAnimatedItems;
    }

    public static boolean lazyClear() {
        return lazyClear;
    }

    public static boolean noEntitySort() {
        return noEntitySort;
    }

    public static boolean bidiCache() {
        return bidiCache;
    }

    public static boolean preparedTextCache() {
        return preparedTextCache;
    }

    public static boolean itemLod() {
        return itemLod;
    }

    public static int itemLodDistance() {
        return itemLodDistance;
    }

    public static boolean entityLod() {
        return entityLod;
    }

    public static int entityLodDistance() {
        return entityLodDistance;
    }

    public static boolean particleLight() {
        return particleLight;
    }

    public static boolean particlePhysics() {
        return particlePhysics;
    }

    public static boolean particleLod() {
        return particleLod;
    }

    public static boolean parallelParticles() {
        return parallelParticles;
    }

    public static boolean spawnerCull() {
        return spawnerCull;
    }

    public static int spawnerDistance() {
        return spawnerDistance;
    }

    public static boolean spawnerTickCache() {
        return spawnerTickCache;
    }

    public static boolean spawnerReplay() {
        return spawnerReplay;
    }

    public static boolean blockEntityCache() {
        return blockEntityCache;
    }

    public static boolean lookupCaches() {
        return lookupCaches;
    }

    public static boolean signCache() {
        return lookupCaches;
    }

    public static boolean microOpts() {
        return microOpts;
    }

    public static boolean microOpts2() {
        return microOpts;
    }

    public static final class Values {
        public boolean enabled;
        public boolean guiAnimatedItems = true;
        public boolean lazyClear = true;
        public boolean noEntitySort = true;
        public boolean bidiCache = true;
        public boolean preparedTextCache = true;
        public boolean itemLod = true;
        public int itemLodDistance = 8;
        public boolean entityLod = true;
        public int entityLodDistance = 24;
        public boolean particleLight = true;
        public boolean particlePhysics = true;
        public boolean particleLod = true;
        public boolean parallelParticles = true;
        public boolean spawnerCull = true;
        public int spawnerDistance = 16;
        public boolean spawnerTickCache = true;
        public boolean spawnerReplay = true;
        public boolean blockEntityCache = true;
        public boolean lookupCaches = true;
        public boolean microOpts = true;
    }
}
