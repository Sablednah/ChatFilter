package com.sablednah.chatfilter.bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Brings a ChatFilter 2.x install forward.
 *
 * <p>The plugin keeps its name, so it inherits {@code plugins/ChatFilter/}: a {@code config.yml}
 * with {@code kick}/{@code censor}/{@code aggressiveMatching} and a {@code lang.yml} holding the
 * word lists. Both are read, their settings carried over into a new {@code config.yml}, and the
 * originals kept beside it as {@code *-2.x.yml.bak}. The old behaviour is kept rather than
 * "improved": a server that blocked swearing still blocks it, and the log says how to try the
 * silent mode instead.</p>
 */
public final class Migration {

    /** Commands the old default list held whose first argument is a player name, not words. */
    private static final List<String> NAME_FIRST = Arrays.asList("msg", "tell", "w", "whisper", "pm", "m", "t",
            "emsg", "etell", "ewhisper", "mail send");

    private Migration() {}

    /** Whether {@code config.yml} is a 2.x one. */
    public static boolean isLegacy(File folder) {
        File cfg = new File(folder, "config.yml");
        if (!cfg.exists()) return false;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(cfg);
        return !y.isConfigurationSection("matching")
                && (y.contains("censor") || y.contains("kick") || y.contains("showInConsole"));
    }

    /**
     * Migrate in place. {@code defaults} is the jar's config.yml, which becomes the new file with
     * the old values set into it.
     *
     * @return a line per thing carried over, for the log
     */
    public static List<String> migrate(File folder, InputStream defaults, Logger log) throws IOException {
        List<String> done = new ArrayList<String>();
        File cfgFile = new File(folder, "config.yml");
        File langFile = new File(folder, "lang.yml");
        YamlConfiguration old = YamlConfiguration.loadConfiguration(cfgFile);
        YamlConfiguration lang = langFile.exists() ? YamlConfiguration.loadConfiguration(langFile) : new YamlConfiguration();
        // loadFromString, not loadConfiguration(Reader): the Reader overload is newer than 1.7.2 and
        // the InputStream one is gone from later APIs. A string works on every line.
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(readAll(defaults));
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            throw new IOException("the jar's own config.yml does not parse: " + e.getMessage());
        }

        // The word lists lived in lang.yml; very early versions had them in config.yml.
        List<String> anywhere = firstList(lang, old, "profanity");
        if (anywhere != null) { cfg.set("profanity.anywhere", anywhere); done.add("profanity -> profanity.anywhere (" + anywhere.size() + ")"); }
        List<String> words = firstList(lang, old, "profanityWordMatch");
        if (words != null) { cfg.set("profanity.words", words); done.add("profanityWordMatch -> profanity.words (" + words.size() + ")"); }

        boolean censor = old.getBoolean("censor", false);
        cfg.set("profanity.action", censor ? "REPLACE" : "BLOCK");
        done.add("censor: " + censor + " -> profanity.action = " + (censor ? "REPLACE" : "BLOCK"));

        String msg = firstString(lang, old, "profanityMessage");
        if (msg != null) { cfg.set("profanity.message", msg); done.add("profanityMessage -> profanity.message"); }
        String block = firstString(lang, old, "blockMessage");
        if (block != null) { cfg.set("messages.noChat", block); done.add("blockMessage -> messages.noChat"); }
        String censorText = firstString(lang, old, "censorText");
        if (censorText != null) { cfg.set("censor.text", censorText); done.add("censorText -> censor.text"); }

        // The old code read "aggressive" while the shipped file said "aggressiveMatching"; honour both.
        boolean leet = old.getBoolean("aggressiveMatching", false) || old.getBoolean("aggressive", false);
        cfg.set("matching.leetspeak", leet);
        done.add("aggressiveMatching: " + leet + " -> matching.leetspeak");

        if (old.contains("showInConsole")) {
            cfg.set("alerts.console", old.getBoolean("showInConsole"));
            done.add("showInConsole -> alerts.console");
        }
        if (old.getBoolean("kick", false)) {
            List<String> ladder = new ArrayList<String>(Settings.list(cfg, "strikes.ladder"));
            ladder.add("1 = command kick {player} Mind your language");
            cfg.set("strikes.ladder", ladder);
            done.add("kick: true -> strikes.ladder '1 = command kick {player} ...'");
        }

        List<String> triggers = firstList(lang, old, "triggers");
        String phrase = firstString(lang, old, "triggerPhrase");
        if (triggers != null) {
            List<String> rules = new ArrayList<String>();
            if (!triggers.isEmpty() && phrase != null && !phrase.trim().isEmpty()) {
                rules.add(join(triggers, "|") + " = " + phrase);
            }
            cfg.set("responses.rules", rules);
            done.add("triggers/triggerPhrase -> responses.rules (" + rules.size() + ")");
        }

        List<String> commands = firstList(lang, old, "commands");
        if (commands != null) {
            List<String> mapped = new ArrayList<String>();
            for (String c : commands) {
                String e = c.trim().toLowerCase(Locale.ROOT);
                mapped.add(NAME_FIRST.contains(e) ? e + " *" : e);
            }
            cfg.set("commands.list", mapped);
            done.add("commands -> commands.list (" + mapped.size() + "; name-first commands now skip the name)");
        }

        if (!new File(folder, "config-2.x.yml.bak").exists()) {
            if (!cfgFile.renameTo(new File(folder, "config-2.x.yml.bak"))) log.warning("Could not back up config.yml");
        }
        if (langFile.exists() && !langFile.renameTo(new File(folder, "lang-2.x.yml.bak"))) {
            log.warning("Could not back up lang.yml");
        }
        cfg.save(cfgFile);
        return done;
    }

    private static List<String> firstList(YamlConfiguration a, YamlConfiguration b, String key) {
        if (a.isList(key)) return Settings.list(a, key);
        if (b.isList(key)) return Settings.list(b, key);
        return null;
    }

    private static String firstString(YamlConfiguration a, YamlConfiguration b, String key) {
        if (a.isString(key)) return a.getString(key);
        if (b.isString(key)) return b.getString(key);
        return null;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder s = new StringBuilder();
        for (String p : parts) {
            if (s.length() > 0) s.append(sep);
            s.append(p.trim());
        }
        return s.toString();
    }

    private static String readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return new String(out.toByteArray(), "UTF-8");
    }
}
