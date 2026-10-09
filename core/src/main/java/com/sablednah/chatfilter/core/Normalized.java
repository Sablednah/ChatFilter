package com.sablednah.chatfilter.core;

/**
 * A view of a message prepared for matching, that still knows where every character came from.
 *
 * <p>The old plugin matched against a cleaned-up copy and then censored the <em>original</em> with a
 * second, cruder pass — so a word it had caught through an accent or a substitution was reported
 * and then left uncensored (the {@code molést} comment on the old project page is exactly that).
 * Here every character of the view carries the span of the original it was made from, so whatever
 * the matcher finds can be censored, masked or quoted precisely.</p>
 *
 * @param text  the view's characters
 * @param from  for each character of {@code text}, the first original index it came from
 * @param to    for each character of {@code text}, one past the last original index it came from
 */
public final class Normalized {

    private final String text;
    private final int[] from;
    private final int[] to;

    public Normalized(String text, int[] from, int[] to) {
        this.text = text;
        this.from = from;
        this.to = to;
    }

    public String text() {
        return text;
    }

    public int[] from() {
        return from;
    }

    public int[] to() {
        return to;
    }

    /** The original span covered by view characters {@code [start, end)}. */
    public Span original(int start, int end) {
        if (end <= start) {
            int at = start < from.length ? from[start] : (to.length == 0 ? 0 : to[to.length - 1]);
            return new Span(at, at);
        }
        return new Span(from[start], to[end - 1]);
    }

    public int length() {
        return text.length();
    }
}
