package com.sablednah.chatfilter.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a pile of hits into one {@link Outcome}.
 *
 * <p>The firmest action wins ({@link Action} is ordered gentlest first). Below a stop, actions
 * combine: replaced words and a lowercased shout can both happen to one line. A mask cannot sit on
 * a line that is being rewritten anyway — the client's mask covers the <em>signed</em> text, which
 * a rewrite has already replaced — so in that case the masked words are rewritten as {@code ###}
 * instead, which reads the same.</p>
 */
public final class Judge {

    private final Map<String, CategoryPolicy> policies;
    private final CensorStyle style;
    private final String censorText;

    public Judge(Map<String, CategoryPolicy> policies, CensorStyle style, String censorText) {
        this.policies = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(policies));
        this.style = style;
        this.censorText = censorText;
    }

    public CategoryPolicy policy(String category) {
        return policies.getOrDefault(category, CategoryPolicy.OFF);
    }

    public Outcome judge(String original, List<Hit> hits) {
        if (hits.isEmpty()) {
            return Outcome.pass(hits);
        }
        // Firmest action per category, and which category holds the firmest overall.
        Map<String, Action> byCategory = new LinkedHashMap<>();
        for (Hit h : hits) {
            CategoryPolicy p = policy(h.category());
            if (!p.enabled()) continue;
            byCategory.merge(h.category(), p.action(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
        }
        String decider = null;
        Action firmest = Action.LOG;
        for (Map.Entry<String, Action> e : byCategory.entrySet()) {
            if (decider == null || e.getValue().compareTo(firmest) > 0) {
                decider = e.getKey();
                firmest = e.getValue();
            }
        }
        if (decider == null) {
            return Outcome.pass(hits);
        }
        if (firmest == Action.BLOCK) {
            return new Outcome(Outcome.Kind.BLOCK, null, java.util.Collections.emptyList(), decider, hits);
        }
        if (firmest == Action.SHADOW) {
            return new Outcome(Outcome.Kind.SHADOW, null, java.util.Collections.emptyList(), decider, hits);
        }

        List<Span> replace = new ArrayList<>();
        List<Span> mask = new ArrayList<>();
        boolean lowercase = false;
        for (Hit h : hits) {
            Action a = byCategory.get(h.category());
            if (a == null) continue;
            if (h.wholeMessage()) {
                // Only caps has a whole-message rewrite; for anything else REPLACE/MASK on a
                // whole-message finding has nothing to replace and is just a log entry.
                if (a == Action.REPLACE || a == Action.MASK) {
                    lowercase |= h.category().equals("caps");
                }
            } else if (a == Action.REPLACE) {
                replace.add(h.span());
            } else if (a == Action.MASK) {
                mask.add(h.span());
            }
        }

        if (replace.isEmpty() && !lowercase) {
            return mask.isEmpty()
                    ? new Outcome(Outcome.Kind.PASS, null, java.util.Collections.emptyList(), decider, hits)
                    : new Outcome(Outcome.Kind.MASK, null, java.util.Collections.unmodifiableList(new java.util.ArrayList<>(mask)), decider, hits);
        }
        String text = lowercase ? Censor.lowercase(original) : original;
        text = Censor.mask(text, mask);
        text = Censor.apply(text, replace, style, censorText);
        return new Outcome(Outcome.Kind.REWRITE, text, java.util.Collections.emptyList(), decider, hits);
    }
}
