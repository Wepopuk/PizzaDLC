package zako.opt.entity;

import zako.opt.ZakoOptConfig;

public final class EntityLod {
    private EntityLod() {
    }

    public static boolean far(double distanceSq) {
        if (!ZakoOptConfig.entityLod()) {
            return false;
        }
        double d = ZakoOptConfig.entityLodDistance();
        return distanceSq > d * d;
    }
}

