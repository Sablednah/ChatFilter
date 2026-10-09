package com.sablednah.chatfilter.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Writes over spans of a message. */
public final class Censor {

    private static final String GRAWLIX = "@#$%&!*";

    /** The character a masked word reads as, matching what the client draws for its own mask. */
    public static final char MASK_CHAR = '#';

    private Censor() {}

    /**
     * Replace each span. Spans are in original coordinates and may not overlap (the analyzer
     * merges them); applied right to left so earlier offsets stay valid when a replacement changes
     * the length.
     */
    public static String apply(String original, List<Span> spans, CensorStyle style, String text) {
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::start).reversed());
        StringBuilder out = new StringBuilder(original);
        int lastStart = Integer.MAX_VALUE;
        for (Span s : sorted) {
            if (s.end() > lastStart) continue; // overlapping leftovers: the later one already won
            int start = Math.max(0, s.start());
            int end = Math.min(out.length(), s.end());
            if (end <= start) continue;
            out.replace(start, end, replacement(original.substring(start, end), style, text));
            lastStart = start;
        }
        return out.toString();
    }

    /** The mask as text: every character of every span becomes {@link #MASK_CHAR}. */
    public static String mask(String original, List<Span> spans) {
        return apply(original, spans, CensorStyle.REPEAT, String.valueOf(MASK_CHAR));
    }

    /** Lower-case a message character by character, so every span stays where it was. */
    public static String lowercase(String original) {
        char[] c = original.toCharArray();
        for (int i = 0; i < c.length; i++) {
            c[i] = Character.toLowerCase(c[i]);
        }
        return new String(c);
    }

    private static String replacement(String word, CensorStyle style, String text) {
        switch (style) {
            case FIXED:
                return text;
            case REPEAT: {
                char c = text.isEmpty() ? '*' : text.charAt(0);
                return repeat(c, word.length());
            }
            default: {
                StringBuilder b = new StringBuilder(word.length());
                for (int i = 0; i < word.length(); i++) {
                    b.append(GRAWLIX.charAt(i % GRAWLIX.length()));
                }
                return b.toString();
            }
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) b.append(c);
        return b.toString();
    }
}
