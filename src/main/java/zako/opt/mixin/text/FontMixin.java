package zako.opt.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.text.PreparedTextCache;

@Mixin(value={Font.class})
public class FontMixin {
    @Inject(method={"prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"}, at={@At(value="HEAD")}, cancellable=true)
    private void zakoopt$cached(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background, CallbackInfoReturnable<Font.PreparedText> cir) {
        Font.PreparedText cached;
        if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text) && (cached = PreparedTextCache.get(PreparedTextCache.key(text, x, y, color, shadow, includeEmpty, background))) != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method={"prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"}, at={@At(value="RETURN")})
    private void zakoopt$store(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background, CallbackInfoReturnable<Font.PreparedText> cir) {
        PreparedTextCache.Key key;
        if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text) && !PreparedTextCache.known(key = PreparedTextCache.key(text, x, y, color, shadow, includeEmpty, background))) {
            PreparedTextCache.put(key, (Font.PreparedText)cir.getReturnValue());
        }
    }
}

