package com.sablednah.chatfilter.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keyword responses — the feature ChatFilter grew out of (an easter egg in MobHealth), and the
 * reason its default config has always answered "eleven".
 *
 * <p>The old one could not be switched off: Bukkit's copy-defaults put the triggers back on every
 * load, which one commenter found the hard way. These are an ordinary list, so emptying it works.</p>
 */
public final class Responder {

    /** One response: any of {@code triggers}, as a whole word, gets {@code reply}. */
    public static final class Rule {
        private final List<Pattern> triggers;
        private final String label;
        private final String reply;

        public Rule(List<Pattern> triggers, String label, String reply) {
            this.triggers = triggers;
            this.label = label;
            this.reply = reply;
        }

        public List<Pattern> triggers() {
            return triggers;
        }

        public String label() {
            return label;
        }

        public String reply() {
            return reply;
        }
/** Parse {@code "eleven|11 = That's ridiculous, it's not even funny."}; null if malformed. */
        public static Rule parse(String line) {
            int eq = line.indexOf('=');
            if (eq < 1) return null;
            String reply = line.substring(eq + 1).trim();
            if (reply.isEmpty()) return null;
            List<Pattern> triggers = new ArrayList<>();
            for (String t : line.substring(0, eq).split("\\|")) {
                String w = t.trim().toLowerCase(Locale.ROOT);
                if (w.isEmpty()) continue;
                triggers.add(Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(w) + "(?![\\p{L}\\p{N}])"));
            }
            return triggers.isEmpty() ? null : new Rule(triggers, line.substring(0, eq).trim(), reply);
        }
    }

    private final Map<String, Long> lastFired = new HashMap<>();

    /**
     * The replies this message earns, honouring each rule's cooldown. Whole-word and
     * case-insensitive, deliberately without the leetspeak leniency: {@code 11} must not fire on
     * coordinates like {@code 110}.
     */
    public synchronized List<String> respond(String message, List<Rule> rules, long now,
            int cooldownSeconds) {
        List<String> out = new ArrayList<>();
        String lower = message.toLowerCase(Locale.ROOT);
        for (Rule r : rules) {
            if (r.triggers().stream().noneMatch(p -> p.matcher(lower).find())) continue;
            Long last = lastFired.get(r.label());
            if (last != null && now - last < cooldownSeconds * 1000L) continue;
            lastFired.put(r.label(), now);
            out.add(r.reply());
        }
        return out;
    }
}
