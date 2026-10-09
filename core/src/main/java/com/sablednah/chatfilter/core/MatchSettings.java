package com.sablednah.chatfilter.core;

import java.util.List;

/**
 * How hard the matcher looks. See {@link Normalizer} and {@link TermCompiler} for what each does.
 *
 * @param joinMinimum shortest run of spelled-out letters ({@code s h i t}) to join; 0 turns it off
 * @param suffixes    endings a whole-word entry also catches ({@code boob} → {@code boobs})
 */
public final class MatchSettings {

    private final boolean stripAccents;
    private final boolean lookalikes;
    private final boolean stripColourCodes;
    private final int joinMinimum;
    private final boolean leet;
    private final boolean wildcards;
    private final List<String> suffixes;

    public MatchSettings(boolean stripAccents, boolean lookalikes, boolean stripColourCodes, int joinMinimum, boolean leet, boolean wildcards, List<String> suffixes) {
        this.stripAccents = stripAccents;
        this.lookalikes = lookalikes;
        this.stripColourCodes = stripColourCodes;
        this.joinMinimum = joinMinimum;
        this.leet = leet;
        this.wildcards = wildcards;
        this.suffixes = suffixes;
    }

    public boolean stripAccents() {
        return stripAccents;
    }

    public boolean lookalikes() {
        return lookalikes;
    }

    public boolean stripColourCodes() {
        return stripColourCodes;
    }

    public int joinMinimum() {
        return joinMinimum;
    }

    public boolean leet() {
        return leet;
    }

    public boolean wildcards() {
        return wildcards;
    }

    public List<String> suffixes() {
        return suffixes;
    }

}
