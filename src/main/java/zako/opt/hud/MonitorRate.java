package zako.opt.hud;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Monitor;
import org.slf4j.LoggerFactory;

public final class MonitorRate {
    public static volatile boolean refreshAnimated = true;
    private static long intervalNs = 6944444L;
    private static long nextCheck;

    private MonitorRate() {
    }

    public static long intervalNs(long now) {
        if (now >= nextCheck) {
            Monitor monitor = Minecraft.getInstance().getWindow().findBestMonitor();
            int hz = monitor != null ? monitor.currentMode().getRefreshRate() : 144;
            long interval = 1000000000L / (long)Math.max(30, hz);
            if (interval != intervalNs) {
                LoggerFactory.getLogger((String)"zakoopt").info("Monitor refresh rate {} Hz", (Object)hz);
            }
            intervalNs = interval;
            nextCheck = now + 1000000000L;
        }
        return intervalNs;
    }
}

