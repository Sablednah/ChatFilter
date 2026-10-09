package com.sablednah.chatfilter.core;

/** A half-open range {@code [start, end)} of characters in the original message. */
public final class Span {

    private final int start;
    private final int end;

    public Span(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public boolean contains(Span other) {
        return start <= other.start && other.end <= end;
    }

    public boolean overlaps(Span other) {
        return start < other.end && other.start < end;
    }

    public int length() {
        return end - start;
    }
}
