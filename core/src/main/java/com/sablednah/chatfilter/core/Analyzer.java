package com.sablednah.chatfilter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Finds everything the configured categories object to in one piece of text.
 *
 * <p>Pure: no state, no side effects, no Minecraft. The same text always gives the same hits, which
 * is what lets the vanilla mask, the chat event, a command and Standards' seam all ask about one
 * message without having to agree who asks first. Built once per config load and then shared.</p>
 */
public final class Analyzer {

    /** A compiled word-list category. */
    private static final class CompiledList {
        private final String name;
        private final List<Term> terms;
        private final List<Pattern> allowed;

        CompiledList(String name, List<Term> terms, List<Pattern> allowed) {
            this.name = name;
            this.terms = terms;
            this.allowed = allowed;
        }

        public String name() {
            return name;
        }

        public List<Term> terms() {
            return terms;
        }

        public List<Pattern> allowed() {
            return allowed;
        }
    }

    private static final class Term {
        private final String label;
        private final Pattern pattern;

        Term(String label, Pattern pattern) {
            this.label = label;
            this.pattern = pattern;
        }

        public String label() {
            return label;
        }

        public Pattern pattern() {
            return pattern;
        }
    }

    private final Normalizer normalizer;
    private final List<CompiledList> lists = new ArrayList<>();
    private final LinkSettings links;
    private final Pattern linkPattern;
    private final Pattern ipPattern;
    /** How a dot may be written, so a caught host can be put back together for the allow-list. */
    private final Pattern dotPattern;
    private final CapsSettings caps;
    private final List<String> problems = new ArrayList<>();

    public Analyzer(MatchSettings match, List<WordListSettings> wordLists, LinkSettings links,
            CapsSettings caps) {
        this.normalizer = new Normalizer(match.stripAccents(), match.lookalikes(),
                match.stripColourCodes(), match.joinMinimum(),
                match.leet() ? TermCompiler.LEET_SYMBOLS : (match.wildcards() ? "*" : ""));
        TermCompiler compiler = new TermCompiler(match.leet(), match.wildcards());
        List<String> suffixes = match.suffixes().stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim()).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());

        for (WordListSettings list : wordLists) {
            if (!list.policy().enabled()) continue;
            List<Term> terms = new ArrayList<>();
            for (String t : list.anywhere()) {
                if (!t.trim().isEmpty()) terms.add(new Term(t.trim(), compiler.anywhere(t)));
            }
            for (String t : list.words()) {
                if (!t.trim().isEmpty()) terms.add(new Term(t.trim(), compiler.word(t, suffixes)));
            }
            for (String p : list.patterns()) {
                if (p.trim().isEmpty()) continue;
                try {
                    terms.add(new Term("/" + p + "/", Pattern.compile(p, Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE)));
                } catch (PatternSyntaxException e) {
                    // One bad pattern must not take the whole filter down with it — report it and
                    // carry on with the rest, which is what an owner editing a file live needs.
                    problems.add(list.name() + ": pattern '" + p + "' is not a valid regular expression ("
                            + e.getDescription() + ")");
                }
            }
            List<Pattern> allowed = list.allowed().stream()
                    .filter(a -> !a.trim().isEmpty()).map(TermCompiler::literal).collect(java.util.stream.Collectors.toList());
            lists.add(new CompiledList(list.name(), terms, allowed));
        }

        this.links = links;
        if (links.policy().enabled() && !links.tlds().isEmpty()) {
            String dot = links.spelledDots()
                    ? "(?:\\s*(?:\\.|\\(\\.\\)|\\[\\.\\]|\\{\\.\\}|\\(dot\\)|\\[dot\\]|\\{dot\\})\\s*|\\s+dot\\s+)"
                    : "\\.";
            String tlds = String.join("|", links.tlds().stream()
                    .map(t -> Pattern.quote(t.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "")))
                    .collect(java.util.stream.Collectors.toList()));
            this.dotPattern = Pattern.compile(dot);
            this.linkPattern = Pattern.compile(
                    "(?<![\\p{L}\\p{N}-])(?:[a-z][a-z0-9+.-]*://)?"
                    + "((?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?" + dot + ")+"
                    + "(?:" + tlds + "))"
                    + "(?![\\p{L}\\p{N}])(?::\\d{1,5})?(?:/[^\\s]*)?");
            this.ipPattern = links.ips()
                    ? Pattern.compile("(?<![\\p{N}.])((?:\\d{1,3}" + dot + "){3}\\d{1,3})(?![\\p{N}])(?::\\d{1,5})?")
                    : null;
        } else {
            this.linkPattern = null;
            this.ipPattern = null;
            this.dotPattern = null;
        }
        this.caps = caps;
    }

    /** Configuration mistakes found while compiling — bad regular expressions, mostly. */
    public List<String> problems() {
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(problems));
    }

    /** Every hit in {@code original}, in no particular order. */
    public List<Hit> analyze(String original) {
        List<Hit> hits = new ArrayList<>();
        if (original == null || original.isEmpty()) {
            return hits;
        }
        Normalized plain = normalizer.plain(original);
        Normalized joined = normalizer.joined(plain);

        for (CompiledList list : lists) {
            List<Hit> found = new ArrayList<>();
            scan(list, plain, found::add);
            if (joined != plain) {
                scan(list, joined, found::add);
            }
            hits.addAll(dedupe(found));
        }
        if (linkPattern != null) {
            links(plain, hits::add);
        }
        if (caps.policy().enabled() && shouting(original)) {
            hits.add(new Hit("caps", null, caps.maxPercent() + "% capitals"));
        }
        return hits;
    }

    private void scan(CompiledList list, Normalized view, Consumer<Hit> out) {
        List<Span> allowed = new ArrayList<>();
        for (Pattern a : list.allowed()) {
            Matcher m = a.matcher(view.text());
            while (m.find()) {
                allowed.add(view.original(m.start(), m.end()));
            }
        }
        for (Term term : list.terms()) {
            Matcher m = term.pattern().matcher(view.text());
            while (m.find()) {
                if (m.end() == m.start()) continue;
                Span span = view.original(m.start(), m.end());
                if (allowed.stream().noneMatch(a -> a.contains(span))) {
                    out.accept(new Hit(list.name(), span, term.label()));
                }
            }
        }
    }

    private void links(Normalized plain, Consumer<Hit> out) {
        String text = plain.text();
        List<Span> seen = new ArrayList<>();
        Matcher m = linkPattern.matcher(text);
        while (m.find()) {
            String host = dotPattern.matcher(m.group(1)).replaceAll(".");
            if (allowedDomain(host)) continue;
            Span span = plain.original(m.start(), m.end());
            seen.add(span);
            out.accept(new Hit("links", span, host));
        }
        if (ipPattern != null) {
            Matcher ip = ipPattern.matcher(text);
            while (ip.find()) {
                Span span = plain.original(ip.start(), ip.end());
                if (seen.stream().anyMatch(s -> s.overlaps(span))) continue;
                if (!validIp(ip.group(1))) continue;
                out.accept(new Hit("links", span, ip.group(1).replaceAll("[^0-9]+", ".")));
            }
        }
    }

    private boolean allowedDomain(String host) {
        String h = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        for (String allowed : links.allowedDomains()) {
            String a = allowed.toLowerCase(Locale.ROOT).trim().replaceFirst("^www\\.", "");
            if (a.isEmpty()) continue;
            if (h.equals(a) || h.endsWith("." + a)) return true;
        }
        return false;
    }

    /** {@code 300.1.1.1} is a version number or a typo, not an address. */
    private static boolean validIp(String raw) {
        String[] parts = raw.split("[^0-9]+");
        if (parts.length != 4) return false;
        for (String p : parts) {
            if (p.isEmpty() || Integer.parseInt(p) > 255) return false;
        }
        return true;
    }

    private boolean shouting(String original) {
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        return letters >= caps.minLetters() && upper * 100 > letters * caps.maxPercent();
    }

    /**
     * The plain and joined views often find the same word twice, and two entries can overlap
     * ({@code fuck} inside {@code motherfucker}). Overlaps merge into one hit, so a censor replaces a
     * word once rather than leaving a censored word inside a censored word.
     */
    private static List<Hit> dedupe(List<Hit> found) {
        found.sort((a, b) -> a.span().start() != b.span().start()
                ? Integer.compare(a.span().start(), b.span().start())
                : Integer.compare(b.span().end(), a.span().end()));
        List<Hit> out = new ArrayList<>();
        for (Hit h : found) {
            if (!out.isEmpty()) {
                Hit last = out.get(out.size() - 1);
                if (last.span().overlaps(h.span())) {
                    if (h.span().end() > last.span().end()) {
                        out.set(out.size() - 1, new Hit(last.category(),
                                new Span(last.span().start(), h.span().end()), last.term()));
                    }
                    continue;
                }
            }
            out.add(h);
        }
        return out;
    }
}
