package com.sablednah.chatfilter.bukkit;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Owner-written text to coloured strings: {@code &} codes, {@code &#rrggbb}, and placeholders.
 *
 * <p>Player text never goes through {@link #colour}, only the owner's templates do, so a player
 * cannot paint their message by typing {@code &c}. Where a template carries a player's words (a
 * staff alert), {@link #fillLiteral} puts them in after the colours are resolved.</p>
 */
public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    /** Hex colours need 1.16; before that the code is dropped rather than printed as garbage. */
    private static final boolean HEX_SUPPORTED = ServerVersion.atLeast(1, 16);

    private Text() {}

    /** {@code &} codes to colour; {@code &#rrggbb} too on 1.16+. */
    public static String colour(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String rep = "";
            if (HEX_SUPPORTED) {
                StringBuilder b = new StringBuilder("§x");
                for (char c : m.group(1).toCharArray()) b.append('§').append(c);
                rep = b.toString();
            }
            m.appendReplacement(out, Matcher.quoteReplacement(rep));
        }
        m.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    /** Fill {@code {key}} placeholders. {@code %N}, the old plugin's name placeholder, still works. */
    public static String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        if (values.containsKey("player")) {
            out = out.replace("%N", values.get("player"));
        }
        return out;
    }

    /**
     * A template with one placeholder holding text that must stay literal. The text inherits the
     * colour in force where it sits, which is what the owner meant by writing {@code &f{message}}.
     */
    public static String fillLiteral(String template, String key, String literal) {
        String marker = "{" + key + "}";
        int at = template.indexOf(marker);
        if (at < 0) return colour(template);
        String head = colour(template.substring(0, at));
        String tail = colour(template.substring(at + marker.length()));
        // Strip any section signs the player smuggled in, so the literal cannot recolour itself.
        String safe = literal.replace('§', '?');
        return head + safe + ChatColor.getLastColors(head) + tail;
    }

    /** To everyone online, and the console. */
    public static void broadcast(String line) {
        Bukkit.broadcastMessage(line);
    }
}
