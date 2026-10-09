package com.sablednah.chatfilter.core;

import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.Map;

/**
 * Turns what a player typed into something a word list can be matched against — without losing
 * track of where each character came from. See {@link Normalized}.
 *
 * <p>Two views are produced. The <b>plain</b> view undoes the disguises that keep a word's letters
 * in order: case, accents, full-width and look-alike letters (Cyrillic {@code а} for Latin
 * {@code a}), invisible characters, and {@code &}-colour codes, which Standards strips from player
 * text and so would otherwise split a word for free. Leetspeak is deliberately <em>not</em> undone
 * here: {@code 1} could be an {@code i} or an {@code l}, so the substitutions live in the term
 * patterns instead (see {@link TermCompiler}), where a character class can mean both at once.</p>
 *
 * <p>The <b>joined</b> view also closes up words spelled out letter by letter —
 * {@code s h i t}, {@code s.h.i.t} — which the old plugin was asked to catch and never did. Only
 * runs of <em>single</em> characters are joined, so {@code this hit} stays two words; joining every
 * separator would find a swear in half the sentences on the server.</p>
 */
public final class Normalizer {

    /**
     * Characters that look like Latin letters and are not decomposed by NFKD. Not exhaustive —
     * nothing is — but covers the ones that actually turn up, from Cyrillic and Greek keyboards and
     * from the "fancy text" generators people paste from.
     */
    private static final Map<Character, String> LOOKALIKES = lookalikes(
                // Cyrillic, then Greek, then Latin letters NFKD leaves alone
                'а', "a", 'в', "b", 'е', "e", 'ё', "e", 'к', "k", 'м', "m",
                'н', "h", 'о', "o", 'р', "p", 'с', "c", 'т', "t", 'у', "y",
                'х', "x", 'і', "i", 'ї', "i", 'ј', "j", 'ѕ', "s", 'ԁ', "d",
                'ԛ', "q", 'ԝ', "w", 'ь', "b", 'п', "n", 'г', "r", 'ц', "u",
                'α', "a", 'β', "b", 'ε', "e", 'η', "n", 'ι', "i", 'κ', "k",
                'ν', "v", 'ο', "o", 'ρ', "p", 'τ', "t", 'υ', "u", 'χ', "x",
                'ω', "w", 'μ', "u", 'ß', "ss", 'æ', "ae", 'œ', "oe", 'ø', "o",
                'đ', "d", 'ł', "l", 'ı', "i", 'ħ', "h", 'ŧ', "t", 'þ', "th",
                'ð', "d");

    /** Things that stand between spelled-out letters: {@code s.h.i.t}, {@code s-h-i-t}, {@code s h i t}. */
    private static final String SEPARATORS = " \t.,-_~'\"`+/\\:;=^";

    private final boolean stripAccents;
    private final boolean lookalikes;
    private final boolean stripColourCodes;
    private final int joinMinimum;
    private final String letterish;

    /**
     * @param stripAccents     fold accented letters to their base ({@code é} to {@code e})
     * @param lookalikes       fold look-alike letters from other scripts
     * @param stripColourCodes drop {@code &x} colour and format codes before matching
     * @param joinMinimum      shortest run of single letters to join; 0 disables joining
     * @param letterish        characters besides letters and digits that count as a letter when
     *                         deciding what a "single letter" is — the leetspeak symbols
     */
    public Normalizer(boolean stripAccents, boolean lookalikes, boolean stripColourCodes,
            int joinMinimum, String letterish) {
        this.stripAccents = stripAccents;
        this.lookalikes = lookalikes;
        this.stripColourCodes = stripColourCodes;
        this.joinMinimum = joinMinimum;
        this.letterish = letterish == null ? "" : letterish;
    }

    /** The plain view. */
    public Normalized plain(String original) {
        StringBuilder out = new StringBuilder(original.length());
        IntList from = new IntList(original.length());
        IntList to = new IntList(original.length());

        int i = 0;
        while (i < original.length()) {
            int cp = original.codePointAt(i);
            int width = Character.charCount(cp);
            int next = i + width;

            // &c, &l, &#rrggbb — what a colour code hides is two letters stuck back together.
            if (stripColourCodes && cp == '&' && next < original.length()) {
                int skip = colourCodeLength(original, next);
                if (skip > 0) {
                    i = next + skip;
                    continue;
                }
            }
            if (invisible(cp)) {
                i = next;
                continue;
            }

            String piece = fold(new String(Character.toChars(cp)));
            for (int k = 0; k < piece.length(); k++) {
                out.append(piece.charAt(k));
                from.add(i);
                to.add(next);
            }
            i = next;
        }
        return new Normalized(out.toString(), from.toArray(), to.toArray());
    }

    /**
     * The joined view, built from the plain one: runs of at least {@link #joinMinimum} single
     * letters with only separators between them become one word. Everything else is copied through.
     *
     * @return the plain view unchanged when there was nothing to join
     */
    public Normalized joined(Normalized plain) {
        if (joinMinimum <= 0) {
            return plain;
        }
        String s = plain.text();
        StringBuilder out = new StringBuilder(s.length());
        IntList from = new IntList(s.length());
        IntList to = new IntList(s.length());
        boolean changed = false;

        int i = 0;
        while (i < s.length()) {
            int runEnd = singleLetterRun(s, i);
            if (runEnd > i) {
                // Copy the letters, skip what was between them.
                for (int k = i; k < runEnd; k++) {
                    if (isLetter(s.charAt(k))) {
                        out.append(s.charAt(k));
                        from.add(plain.from()[k]);
                        to.add(plain.to()[k]);
                    }
                }
                changed = true;
                i = runEnd;
            } else {
                out.append(s.charAt(i));
                from.add(plain.from()[i]);
                to.add(plain.to()[i]);
                i++;
            }
        }
        return changed ? new Normalized(out.toString(), from.toArray(), to.toArray()) : plain;
    }

    /**
     * If a run of spelled-out letters starts at {@code start}, where it ends; otherwise
     * {@code start}. A run is letter, separator(s), letter, … with every letter standing alone.
     */
    private int singleLetterRun(String s, int start) {
        if (!isLetter(s.charAt(start)) || (start > 0 && isLetter(s.charAt(start - 1)))) {
            return start;
        }
        int letters = 0;
        int i = start;
        int lastLetterEnd = start;
        while (i < s.length() && isLetter(s.charAt(i))) {
            // This letter must stand alone: the next character is a separator or the end.
            if (i + 1 < s.length() && isLetter(s.charAt(i + 1))) {
                break;
            }
            letters++;
            lastLetterEnd = i + 1;
            int j = i + 1;
            int gap = 0;
            while (j < s.length() && SEPARATORS.indexOf(s.charAt(j)) >= 0 && gap < 3) {
                j++;
                gap++;
            }
            if (gap == 0 || j >= s.length() || !isLetter(s.charAt(j))) {
                break;
            }
            i = j;
        }
        return letters >= joinMinimum ? lastLetterEnd : start;
    }

    private boolean isLetter(char c) {
        return Character.isLetterOrDigit(c) || letterish.indexOf(c) >= 0;
    }

    private String fold(String piece) {
        String s = piece;
        if (stripAccents) {
            // NFKD also folds full-width and the mathematical "fancy text" alphabets, which is
            // most of what a copy-pasted disguise is made of.
            s = java.text.Normalizer.normalize(s, Form.NFKD);
            StringBuilder b = new StringBuilder(s.length());
            for (int k = 0; k < s.length(); k++) {
                char c = s.charAt(k);
                if (Character.getType(c) != Character.NON_SPACING_MARK) {
                    b.append(c);
                }
            }
            s = b.toString();
        }
        s = s.toLowerCase(Locale.ROOT);
        if (lookalikes && s.length() == 1) {
            String mapped = LOOKALIKES.get(s.charAt(0));
            if (mapped != null) {
                return mapped;
            }
        }
        return s;
    }

    /** Zero-width and formatting characters: the classic way to split a word invisibly. */
    private static boolean invisible(int cp) {
        int type = Character.getType(cp);
        return type == Character.FORMAT          // ZWSP, ZWJ, ZWNJ, soft hyphen, BOM, bidi marks
                || type == Character.CONTROL
                || cp == 0x034F                  // combining grapheme joiner
                || (cp >= 0xFE00 && cp <= 0xFE0F); // variation selectors
    }

    /** Length of a colour code after its {@code &}, or 0 if this {@code &} does not start one. */
    private static int colourCodeLength(String s, int at) {
        char c = Character.toLowerCase(s.charAt(at));
        if ("0123456789abcdefklmnor".indexOf(c) >= 0) {
            return 1;
        }
        if (c == '#' && at + 7 <= s.length()) {
            for (int k = at + 1; k < at + 7; k++) {
                if (Character.digit(s.charAt(k), 16) < 0) {
                    return 0;
                }
            }
            return 7;
        }
        return 0;
    }

    /** A growable int array — the mapping arrays are built one character at a time. */
    private static final class IntList {
        private int[] data;
        private int size;

        IntList(int capacity) {
            data = new int[Math.max(capacity, 4)];
        }

        void add(int v) {
            if (size == data.length) {
                data = java.util.Arrays.copyOf(data, size * 2);
            }
            data[size++] = v;
        }

        int[] toArray() {
            return java.util.Arrays.copyOf(data, size);
        }
    }

    /** Pairs of (look-alike, replacement). Built by hand because Map.ofEntries is Java 9. */
    private static Map<Character, String> lookalikes(Object... pairs) {
        Map<Character, String> m = new java.util.HashMap<>();
        for (int k = 0; k < pairs.length; k += 2) {
            m.put((Character) pairs[k], (String) pairs[k + 1]);
        }
        return java.util.Collections.unmodifiableMap(m);
    }
}
