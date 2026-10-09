package com.sablednah.chatfilter.core;

/**
 * @param minLetters  lines with fewer letters than this are never caps — "OK", "GG", "LOL"
 * @param maxPercent  share of capital letters, out of all letters, above which a line is shouting
 */
public final class CapsSettings {

    private final CategoryPolicy policy;
    private final int minLetters;
    private final int maxPercent;

    public CapsSettings(CategoryPolicy policy, int minLetters, int maxPercent) {
        this.policy = policy;
        this.minLetters = minLetters;
        this.maxPercent = maxPercent;
    }

    public CategoryPolicy policy() {
        return policy;
    }

    public int minLetters() {
        return minLetters;
    }

    public int maxPercent() {
        return maxPercent;
    }

}
