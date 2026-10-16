package zako.opt.mixin.text;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.ZakoOptConfig;
import zako.opt.text.LruMap;
import zako.opt.text.PreparedTextCache;

@Mixin(value={ClientLanguage.class})
public class ClientLanguageMixin {
    @Unique
    private final Map<Component, FormattedCharSequence> zakoopt$visualOrder = new LruMap<Component, FormattedCharSequence>(1024);
    @Unique
    private final Map<Component, FormattedCharSequence> zakoopt$byIdentity = new IdentityHashMap<Component, FormattedCharSequence>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"getVisualOrder"}, at={@At(value="HEAD")}, cancellable=true)
    private void zakoopt$cached(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
        FormattedCharSequence cached;
        Component component;
        block9: {
            block8: {
                if (!(text instanceof Component)) break block8;
                component = (Component)text;
                if (ZakoOptConfig.bidiCache()) break block9;
            }
            return;
        }
        boolean identity = ZakoOptConfig.microOpts2();
        Map<Component, FormattedCharSequence> map = this.zakoopt$visualOrder;
        synchronized (map) {
            cached = identity ? this.zakoopt$byIdentity.get(component) : null;
            if (cached == null && (cached = this.zakoopt$visualOrder.get(component)) != null && identity) {
                this.zakoopt$remember(component, cached);
            }
        }
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"getVisualOrder"}, at={@At(value="RETURN")}, cancellable=true)
    private void zakoopt$store(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (text instanceof Component) {
            Component component = (Component)text;
            if (ZakoOptConfig.bidiCache()) {
                FormattedCharSequence stable = PreparedTextCache.markStable((FormattedCharSequence)cir.getReturnValue());
                Map<Component, FormattedCharSequence> map = this.zakoopt$visualOrder;
                synchronized (map) {
                    this.zakoopt$visualOrder.put(component, stable);
                    if (ZakoOptConfig.microOpts2()) {
                        this.zakoopt$remember(component, stable);
                    }
                }
                cir.setReturnValue(stable);
            }
        }
    }

    @Unique
    private void zakoopt$remember(Component component, FormattedCharSequence value) {
        if (this.zakoopt$byIdentity.size() >= 2048) {
            this.zakoopt$byIdentity.clear();
        }
        this.zakoopt$byIdentity.put(component, value);
    }
}

