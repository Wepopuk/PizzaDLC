package zako.opt.text;

import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;

public final class StableText
implements FormattedCharSequence {
    private final FormattedCharSequence text;

    StableText(FormattedCharSequence text) {
        this.text = text;
    }

    public boolean accept(FormattedCharSink sink) {
        return this.text.accept(sink);
    }
}

