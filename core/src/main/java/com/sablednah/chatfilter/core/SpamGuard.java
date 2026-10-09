package com.sablednah.chatfilter.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Repeats and floods — the two spam checks that need memory.
 *
 * <p>Kept per player in memory only: a restart forgetting that somebody said "hi" twice is the
 * correct behaviour, not a loss. Times come in as arguments so the self-test can drive it.</p>
 */
public final class SpamGuard {

    private static final class History {
        String last;
        long lastAt;
        final Deque<Long> recent = new ArrayDeque<>();
    }

    private final Map<UUID, History> players = new HashMap<>();

    /**
     * Note a message and say what, if anything, is wrong with it. Call once per message that
     * would otherwise be delivered — a blocked message noted here would make the next attempt look
     * like a repeat of something nobody saw.
     *
     * @param repeatSeconds a message equal to the previous one within this long is a repeat; 0 off
     * @param floodCount    more than this many messages ...
     * @param floodSeconds  ... within this long is a flood; 0 off
     * @return the category caught ({@code repeat} or {@code flood}), or {@code null}
     */
    public synchronized String check(UUID player, String message, long now,
            int repeatSeconds, int floodCount, int floodSeconds) {
        History h = players.computeIfAbsent(player, k -> new History());
        String key = squash(message);
        String found = null;

        if (floodSeconds > 0 && floodCount > 0) {
            long cutoff = now - floodSeconds * 1000L;
            while (!h.recent.isEmpty() && h.recent.peekFirst() < cutoff) {
                h.recent.pollFirst();
            }
            if (h.recent.size() >= floodCount) {
                found = "flood";
            }
        }
        if (found == null && repeatSeconds > 0 && key.equals(h.last)
                && now - h.lastAt < repeatSeconds * 1000L) {
            found = "repeat";
        }
        return found;
    }

    /** Record a message that was actually delivered. */
    public synchronized void delivered(UUID player, String message, long now) {
        History h = players.computeIfAbsent(player, k -> new History());
        h.last = squash(message);
        h.lastAt = now;
        h.recent.addLast(now);
        while (h.recent.size() > 64) {
            h.recent.pollFirst();
        }
    }

    public synchronized void forget(UUID player) {
        players.remove(player);
    }

    /** "Hi!", "hi" and "HI !!" are the same message said three times. */
    private static String squash(String message) {
        StringBuilder b = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetterOrDigit(c)) b.append(c);
        }
        return b.toString().toLowerCase(Locale.ROOT);
    }
}
