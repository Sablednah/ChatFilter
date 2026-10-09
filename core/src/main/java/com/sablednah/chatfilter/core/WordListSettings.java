package com.sablednah.chatfilter.core;

import java.util.List;

/**
 * One word-list category, as configured.
 *
 * @param anywhere entries matched inside other words too — the old {@code profanity} list
 * @param words    entries matched only as whole words — the old {@code profanityWordMatch}
 * @param patterns regular expressions, matched against the lower-cased, accent-folded text
 * @param allowed  words that are never caught, even when an entry above is inside them —
 *                 {@code scunthorpe}, {@code shiitake}
 */
public final class WordListSettings {

    private final String name;
    private final CategoryPolicy policy;
    private final List<String> anywhere;
    private final List<String> words;
    private final List<String> patterns;
    private final List<String> allowed;

    public WordListSettings(String name, CategoryPolicy policy, List<String> anywhere, List<String> words, List<String> patterns, List<String> allowed) {
        this.name = name;
        this.policy = policy;
        this.anywhere = anywhere;
        this.words = words;
        this.patterns = patterns;
        this.allowed = allowed;
    }

    public String name() {
        return name;
    }

    public CategoryPolicy policy() {
        return policy;
    }

    public List<String> anywhere() {
        return anywhere;
    }

    public List<String> words() {
        return words;
    }

    public List<String> patterns() {
        return patterns;
    }

    public List<String> allowed() {
        return allowed;
    }

}
