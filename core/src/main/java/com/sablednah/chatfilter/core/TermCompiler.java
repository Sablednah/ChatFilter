package com.sablednah.chatfilter.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns a word-list entry into the pattern that finds it.
 *
 * <p>Each letter becomes a small character class, which is where the "aggressive matching" of the
 * old plugin went: rather than rewriting {@code 3} to {@code e} in the message (and turning every
 * {@code 1} into an {@code l}, so {@code sh1t} slipped through as {@code shlt}), the term
 * {@code shit} accepts {@code [i1!|l]} in its third place. Every letter also takes a {@code +},
 * so {@code fuuuuck} is the same word as {@code fuck}.</p>
 *
 * <p>Vowels additionally accept {@code *} when wildcards are on — {@code f*ck}, {@code sh*t} — the
 * self-censoring people do when they want the word read without typing it. Consonants do not, or
 * {@code ****} would match every four-letter entry on the list.</p>
 */
public final class TermCompiler {

    /** What each letter may be written as, beyond itself. The old plugin's map, and then some. */
    private static final Map<Character, String> LEET = pairs(
            'a', "4@^",
            'b', "8",
            'c', "(<{[",
            'e', "3&",
            'g', "69",
            'h', "#",
            'i', "1!|l",
            'l', "1|!i",
            'o', "0",
            's', "5$z",
            't', "7+",
            'z', "2");

    /** Every symbol {@link #LEET} uses, so the {@link Normalizer} can treat them as letters. */
    public static final String LEET_SYMBOLS = "4@^8(<{[3&69#1!|057$+2*";

    private final boolean leet;
    private final boolean wildcards;

    public TermCompiler(boolean leet, boolean wildcards) {
        this.leet = leet;
        this.wildcards = wildcards;
    }

    /** A term matched anywhere — inside other words too. */
    public Pattern anywhere(String term) {
        return Pattern.compile(body(term));
    }

    /**
     * A term matched only as a whole word, optionally with one of {@code suffixes} on the end
     * ({@code boob} also catching {@code boobs}). The boundary is "not a letter or digit", not
     * {@code \b}, because {@code \b} thinks {@code @} ends a word and {@code @ss} starts one.
     */
    public Pattern word(String term, List<String> suffixes) {
        StringBuilder p = new StringBuilder("(?<![\\p{L}\\p{N}])").append(body(term));
        if (!suffixes.isEmpty()) {
            p.append("(?:");
            for (int k = 0; k < suffixes.size(); k++) {
                if (k > 0) p.append('|');
                p.append(body(suffixes.get(k)));
            }
            p.append(")?");
        }
        return Pattern.compile(p.append("(?![\\p{L}\\p{N}])").toString());
    }

    /** A term as typed, with none of the leniency — for allow-list entries. */
    public static Pattern literal(String term) {
        return Pattern.compile(Pattern.quote(term.toLowerCase(Locale.ROOT)));
    }

    private String body(String term) {
        String t = term.toLowerCase(Locale.ROOT).trim();
        StringBuilder p = new StringBuilder();
        int i = 0;
        while (i < t.length()) {
            char c = t.charAt(i);
            // A run of one letter in the term ("ass") needs at least that many in the message.
            int run = 1;
            while (i + run < t.length() && t.charAt(i + run) == c) run++;
            String cls = charClass(c);
            p.append(cls);
            if (Character.isLetter(c)) {
                p.append(run == 1 ? "+" : "{" + run + ",}");
            } else if (run > 1) {
                p.append("{").append(run).append("}");
            }
            i += run;
        }
        return p.toString();
    }

    private String charClass(char c) {
        if (Character.isWhitespace(c)) {
            // A space in a term ("blow job") matches any run of separators, or none.
            return "[\\s\\p{P}]*";
        }
        StringBuilder chars = new StringBuilder();
        chars.append(c);
        if (leet && LEET.containsKey(c)) {
            chars.append(LEET.get(c));
        }
        if (wildcards && "aeiouy".indexOf(c) >= 0) {
            chars.append('*');
        }
        if (chars.length() == 1) {
            return Pattern.quote(String.valueOf(c));
        }
        StringBuilder cls = new StringBuilder("[");
        for (int k = 0; k < chars.length(); k++) {
            char x = chars.charAt(k);
            if ("\\^-[]&".indexOf(x) >= 0) cls.append('\\');
            cls.append(x);
        }
        return cls.append(']').toString();
    }

    /** Pairs of (letter, alternatives). Built by hand because Map.ofEntries is Java 9. */
    private static Map<Character, String> pairs(Object... kv) {
        Map<Character, String> m = new java.util.HashMap<>();
        for (int k = 0; k < kv.length; k += 2) {
            m.put((Character) kv[k], (String) kv[k + 1]);
        }
        return java.util.Collections.unmodifiableMap(m);
    }
}
