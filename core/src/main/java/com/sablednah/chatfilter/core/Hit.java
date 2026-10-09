package com.sablednah.chatfilter.core;

/**
 * One thing a category found.
 *
 * @param category which category ({@code profanity}, {@code links}, …)
 * @param span     what it covers in the original text; {@code null} for whole-message findings
 *                 such as caps or flooding
 * @param term     the list entry, pattern or rule that matched — for logs and {@code /chatfilter test}
 */
public final class Hit {

    private final String category;
    private final Span span;
    private final String term;

    public Hit(String category, Span span, String term) {
        this.category = category;
        this.span = span;
        this.term = term;
    }

    public String category() {
        return category;
    }

    public Span span() {
        return span;
    }

    public String term() {
        return term;
    }

    public boolean wholeMessage() {
        return span == null;
    }
}
