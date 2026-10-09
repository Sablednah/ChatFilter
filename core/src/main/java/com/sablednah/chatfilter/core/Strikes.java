package com.sablednah.chatfilter.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts offences so the response can escalate — the "less severe punishment than kicking" the old
 * page asked for, and then a firmer one if it keeps happening.
 *
 * <p>Strikes expire individually after the decay window, so somebody who slips once an hour never
 * reaches a threshold, while somebody who keeps at it does. In memory: a restart is a clean slate.</p>
 */
public final class Strikes {

    /**
     * One rung of the ladder: what happens when a player reaches {@code at} strikes.
     *
     * @param kind  {@code alert}, {@code tell} or {@code command}
     * @param value the message or command; {@code {player}} and {@code {strikes}} are filled in
     */
    public static final class Step {
        private final int at;
        private final String kind;
        private final String value;

        public Step(int at, String kind, String value) {
            this.at = at;
            this.kind = kind;
            this.value = value;
        }

        public int at() {
            return at;
        }

        public String kind() {
            return kind;
        }

        public String value() {
            return value;
        }
/**
         * Parse {@code "5 = command kick {player} Language"}. Returns null for anything that does
         * not parse, so one typo costs one rung rather than the whole ladder.
         */
        public static Step parse(String line) {
            int eq = line.indexOf('=');
            if (eq < 0) return null;
            try {
                int at = Integer.parseInt(line.substring(0, eq).trim());
                String rest = line.substring(eq + 1).trim();
                int sp = rest.indexOf(' ');
                String kind = (sp < 0 ? rest : rest.substring(0, sp)).toLowerCase(java.util.Locale.ROOT);
                String value = sp < 0 ? "" : rest.substring(sp + 1).trim();
                if (at < 1 || !(kind.equals("alert") || kind.equals("tell") || kind.equals("command"))) {
                    return null;
                }
                return new Step(at, kind, value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private final Map<UUID, Deque<Long>> players = new HashMap<>();

    /**
     * Add strikes and return the ladder steps crossed by doing so — each step fires once, on the
     * way up, rather than on every offence past it.
     */
    public synchronized List<Step> add(UUID player, int count, long now, long decayMillis,
            List<Step> ladder, int[] totalOut) {
        Deque<Long> times = players.computeIfAbsent(player, k -> new ArrayDeque<>());
        expire(times, now, decayMillis);
        int before = times.size();
        for (int i = 0; i < count; i++) {
            times.addLast(now);
        }
        int after = times.size();
        if (totalOut != null && totalOut.length > 0) totalOut[0] = after;
        List<Step> crossed = new ArrayList<>();
        for (Step s : ladder) {
            if (s.at() > before && s.at() <= after) {
                crossed.add(s);
            }
        }
        return crossed;
    }

    public synchronized int count(UUID player, long now, long decayMillis) {
        Deque<Long> times = players.get(player);
        if (times == null) return 0;
        expire(times, now, decayMillis);
        return times.size();
    }

    public synchronized void clear(UUID player) {
        players.remove(player);
    }

    private static void expire(Deque<Long> times, long now, long decayMillis) {
        if (decayMillis <= 0) return;
        while (!times.isEmpty() && times.peekFirst() <= now - decayMillis) {
            times.pollFirst();
        }
    }
}
