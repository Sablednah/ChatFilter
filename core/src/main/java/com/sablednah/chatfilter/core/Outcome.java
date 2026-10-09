package com.sablednah.chatfilter.core;

import java.util.List;

/**
 * The single decision about one message, after every category has had its say.
 *
 * @param kind     what to do
 * @param text     for {@link Kind#REWRITE}, the new text everybody sees
 * @param mask     for {@link Kind#MASK}, the spans to hide from everyone but the sender
 * @param decider  the category whose action won — whose message the sender may be told
 * @param hits     everything that was found, whether or not it changed anything
 */
public final class Outcome {

    private final Kind kind;
    private final String text;
    private final List<Span> mask;
    private final String decider;
    private final List<Hit> hits;

    public Outcome(Kind kind, String text, List<Span> mask, String decider, List<Hit> hits) {
        this.kind = kind;
        this.text = text;
        this.mask = mask;
        this.decider = decider;
        this.hits = hits;
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    public List<Span> mask() {
        return mask;
    }

    public String decider() {
        return decider;
    }

    public List<Hit> hits() {
        return hits;
    }

    public enum Kind {
        /** Nothing to change. There may still be {@link Action#LOG} hits to report. */
        PASS,
        /** Hide {@link #mask} from other viewers; the text itself is untouched. */
        MASK,
        /** Everybody sees {@link #text} instead. */
        REWRITE,
        /** The sender sees it sent; nobody else sees it. */
        SHADOW,
        /** Nobody sees it. */
        BLOCK
    }

    public static Outcome pass(List<Hit> hits) {
        return new Outcome(Kind.PASS, null, java.util.Collections.<Span>emptyList(), null, hits);
    }

    public boolean changes() {
        return kind != Kind.PASS;
    }

    public boolean stops() {
        return kind == Kind.BLOCK || kind == Kind.SHADOW;
    }

    /** What a viewer who does not see the original reads, for paths that deliver text rather than masks. */
    public String censoredText(String original) {
        switch (kind) {
            case MASK:
                return Censor.mask(original, mask);
            case REWRITE:
                return text;
            default:
                return original;
        }
    }
}
