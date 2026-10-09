package com.sablednah.chatfilter.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.sablednah.chatfilter.core.Action;
import com.sablednah.chatfilter.core.Analyzer;
import com.sablednah.chatfilter.core.CapsSettings;
import com.sablednah.chatfilter.core.CategoryPolicy;
import com.sablednah.chatfilter.core.CensorStyle;
import com.sablednah.chatfilter.core.Judge;
import com.sablednah.chatfilter.core.LinkSettings;
import com.sablednah.chatfilter.core.MatchSettings;
import com.sablednah.chatfilter.core.Responder;
import com.sablednah.chatfilter.core.Strikes;
import com.sablednah.chatfilter.core.WordListSettings;

/**
 * {@code config.yml}, read and compiled. Built whole and swapped in whole, because chat is judged
 * on Bukkit's async chat threads while a reload happens on the main one.
 *
 * <p>The sections and keys are the NeoForge mod's {@code chatfilter-common.toml}, so one set of docs
 * describes both. What differs: command entries say how many words to skip ({@code msg *}), because
 * Bukkit has no command tree to find the message in, and there is no {@code autoDetect}.</p>
 */
public final class Settings {

    public final Analyzer analyzer;
    public final Judge judge;
    public final List<List<String>> commands;
    public final List<Strikes.Step> ladder;
    public final List<Responder.Rule> responses;
    public final List<String> problems;

    public final boolean commandsEnabled;
    public final boolean strikesEnabled;
    public final long strikeDecayMillis;
    public final boolean alertConsole;
    public final boolean alertStaff;
    public final boolean responsesEnabled;
    public final int responseCooldown;
    public final int responseDelayTicks;
    public final int repeatSeconds;
    public final int floodMessages;
    public final int floodSeconds;

    public final String prefix;
    public final String msgNoChat;
    public final String msgAlert;
    public final String msgStrikeAlert;

    private Settings(FileConfiguration c) {
        List<String> problems = new ArrayList<String>();
        MatchSettings match = new MatchSettings(
                c.getBoolean("matching.stripAccents", true),
                c.getBoolean("matching.lookalikes", true),
                c.getBoolean("matching.stripColourCodes", true),
                clamp(c.getInt("matching.joinSpelledOut", 3), 0, 20),
                c.getBoolean("matching.leetspeak", true),
                c.getBoolean("matching.wildcards", true),
                list(c, "matching.wordSuffixes"));

        Map<String, CategoryPolicy> policies = new LinkedHashMap<String, CategoryPolicy>();
        for (String cat : Arrays.asList("profanity", "severe", "links", "caps", "repeat", "flood")) {
            policies.put(cat, policy(c, cat, problems));
        }
        List<WordListSettings> lists = Arrays.asList(
                words(c, "profanity", policies.get("profanity")),
                words(c, "severe", policies.get("severe")));
        LinkSettings links = new LinkSettings(policies.get("links"), c.getBoolean("links.ips", true),
                c.getBoolean("links.spelledDots", true), list(c, "links.tlds"), list(c, "links.allowedDomains"));
        CapsSettings caps = new CapsSettings(policies.get("caps"), clamp(c.getInt("caps.minLetters", 8), 1, 256),
                clamp(c.getInt("caps.maxPercent", 70), 1, 100));

        this.analyzer = new Analyzer(match, lists, links, caps);
        problems.addAll(analyzer.problems());
        this.judge = new Judge(policies, enumOr(CensorStyle.class, c.getString("censor.style"), CensorStyle.FIXED,
                "censor.style", problems), c.getString("censor.text", "!@$#"));

        List<List<String>> cmds = new ArrayList<List<String>>();
        for (String entry : list(c, "commands.list")) {
            String e = entry.trim().replaceFirst("^/", "").toLowerCase(Locale.ROOT);
            if (!e.isEmpty()) cmds.add(Arrays.asList(e.split("\\s+")));
        }
        this.commands = Collections.unmodifiableList(cmds);

        List<Strikes.Step> steps = new ArrayList<Strikes.Step>();
        for (String line : list(c, "strikes.ladder")) {
            Strikes.Step step = Strikes.Step.parse(line);
            if (step == null) {
                problems.add("strikes: cannot read ladder entry '" + line
                        + "' - expected 'N = alert', 'N = tell <message>' or 'N = command <command>'");
            } else {
                steps.add(step);
            }
        }
        this.ladder = Collections.unmodifiableList(steps);

        List<Responder.Rule> rules = new ArrayList<Responder.Rule>();
        for (String line : list(c, "responses.rules")) {
            Responder.Rule rule = Responder.Rule.parse(line);
            if (rule == null) {
                problems.add("responses: cannot read '" + line + "' - expected 'trigger|trigger = reply'");
            } else {
                rules.add(rule);
            }
        }
        this.responses = Collections.unmodifiableList(rules);

        this.commandsEnabled = c.getBoolean("commands.enabled", true);
        this.strikesEnabled = c.getBoolean("strikes.enabled", true);
        this.strikeDecayMillis = clamp(c.getInt("strikes.decayMinutes", 30), 1, 10080) * 60000L;
        this.alertConsole = c.getBoolean("alerts.console", true);
        this.alertStaff = c.getBoolean("alerts.staff", true);
        this.responsesEnabled = c.getBoolean("responses.enabled", true);
        this.responseCooldown = clamp(c.getInt("responses.cooldownSeconds", 30), 0, 86400);
        this.responseDelayTicks = clamp(c.getInt("responses.delayTicks", 10), 1, 200);
        this.repeatSeconds = clamp(c.getInt("repeat.seconds", 30), 0, 3600);
        this.floodMessages = clamp(c.getInt("flood.messages", 5), 1, 100);
        this.floodSeconds = clamp(c.getInt("flood.seconds", 4), 0, 600);

        this.prefix = c.getString("messages.prefix", "&9[ChatFilter] &f");
        this.msgNoChat = c.getString("messages.noChat", "I'm sorry {player}, I can't let you do that.");
        this.msgAlert = c.getString("messages.alert",
                "&8[&cCF&8] &7{player} &8({channel}, {category}: {term})&7: &f{message}");
        this.msgStrikeAlert = c.getString("messages.strikeAlert", "&8[&cCF&8] &7{player} &fhas {strikes} strikes.");
        this.problems = Collections.unmodifiableList(problems);
    }

    public static Settings load(FileConfiguration c) {
        return new Settings(c);
    }

    private static CategoryPolicy policy(FileConfiguration c, String cat, List<String> problems) {
        ConfigurationSection s = c.getConfigurationSection(cat);
        if (s == null) {
            return CategoryPolicy.OFF;
        }
        return new CategoryPolicy(s.getBoolean("enabled", true),
                enumOr(Action.class, s.getString("action"), Action.LOG, cat + ".action", problems),
                s.getBoolean("tellSender", true), s.getString("message", ""),
                clamp(s.getInt("strikes", 0), 0, 100), s.getBoolean("alertStaff", true));
    }

    private static WordListSettings words(FileConfiguration c, String cat, CategoryPolicy policy) {
        return new WordListSettings(cat, policy, list(c, cat + ".anywhere"), list(c, cat + ".words"),
                list(c, cat + ".patterns"), list(c, cat + ".allowed"));
    }

    /** A list, every element as a string: YAML will happily hand back {@code 11} as an integer. */
    static List<String> list(FileConfiguration c, String path) {
        List<?> raw = c.getList(path);
        if (raw == null) return Collections.emptyList();
        List<String> out = new ArrayList<String>(raw.size());
        for (Object o : raw) {
            if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String value, E fallback, String path,
            List<String> problems) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            problems.add(path + ": '" + value + "' is not one of " + Arrays.toString(type.getEnumConstants())
                    + "; using " + fallback);
            return fallback;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
