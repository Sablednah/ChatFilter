package com.sablednah.chatfilter.bukkit;

import java.util.List;
import java.util.Locale;

/**
 * Which part of a command line is somebody talking.
 *
 * <p>The NeoForge mod parses the command and finds its message argument. Bukkit's API has no
 * command tree to ask, so the configured entries say it instead: words to match, with {@code *}
 * for a word to skip. {@code msg *} matches {@code /msg Steve hello} and filters {@code hello},
 * never the name, so {@code /msg Assassin hi} works. An entry with no {@code *} ({@code r},
 * {@code f chat}) filters everything after it, as the old plugin did.</p>
 */
public final class CommandMatcher {

    private CommandMatcher() {}

    /** The matched entry and where the message starts. */
    public static final class Match {
        public final String label;
        public final int messageStart;

        Match(String label, int messageStart) {
            this.label = label;
            this.messageStart = messageStart;
        }
    }

    /**
     * @param line the command without its slash: {@code msg Steve hello}
     * @return the first entry that matches, or null
     */
    public static Match match(String line, List<List<String>> entries) {
        String[] typed = line.trim().split("\\s+");
        if (typed.length == 0 || typed[0].isEmpty()) return null;
        // "essentials:msg" and "minecraft:tell" are "msg" and "tell".
        typed[0] = typed[0].substring(typed[0].indexOf(':') + 1);
        for (List<String> entry : entries) {
            if (entry.size() >= typed.length) continue; // nothing left to be a message
            boolean ok = true;
            for (int i = 0; i < entry.size(); i++) {
                String want = entry.get(i);
                if (!want.equals("*") && !want.equals(typed[i].toLowerCase(Locale.ROOT))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                int at = endOfWords(line, entry.size());
                while (at < line.length() && Character.isWhitespace(line.charAt(at))) at++;
                return new Match(typed[0].toLowerCase(Locale.ROOT), at);
            }
        }
        return null;
    }

    private static int endOfWords(String s, int words) {
        int i = 0;
        for (int w = 0; w < words; w++) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            while (i < s.length() && !Character.isWhitespace(s.charAt(i))) i++;
        }
        return i;
    }
}
