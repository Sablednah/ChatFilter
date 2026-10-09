package com.sablednah.chatfilter.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.sablednah.chatfilter.core.BuildInfo;
import com.sablednah.chatfilter.core.CategoryPolicy;
import com.sablednah.chatfilter.core.Hit;
import com.sablednah.chatfilter.core.Outcome;

/**
 * {@code /chatfilter}: reload, test, add, remove, list, strikes, alerts. The same commands as the
 * NeoForge mod, gated by {@code chatfilter.admin} (and {@code chatfilter.alerts} for alerts).
 */
public final class Commands implements CommandExecutor, TabCompleter {

    /** Editable lists, by the name staff type, to their config path. */
    private static final Map<String, String> LISTS = new LinkedHashMap<String, String>();
    static {
        for (String cat : new String[] {"profanity", "severe"}) {
            for (String kind : new String[] {"anywhere", "words", "patterns", "allowed"}) {
                LISTS.put(cat + "." + kind, cat + "." + kind);
            }
        }
        LISTS.put("links.allowed", "links.allowedDomains");
        LISTS.put("commands", "commands.list");
        LISTS.put("responses", "responses.rules");
    }

    private static final List<String> SUBS = Arrays.asList("reload", "test", "add", "remove", "list", "strikes", "alerts");

    private final ChatFilterPlugin plugin;

    public Commands(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("alerts")) return alerts(sender, args);
        if (!sub.isEmpty() && !sender.hasPermission(FilterService.PERM_ADMIN)) {
            say(sender, "&7That one is for staff.");
            return true;
        }
        if (sub.equals("reload")) return reload(sender);
        if (sub.equals("test") && args.length > 1) return test(sender, rest(args, 1));
        if ((sub.equals("add") || sub.equals("remove")) && args.length > 2) return edit(sender, sub.equals("add"), args[1], rest(args, 2));
        if (sub.equals("list") && args.length > 1) return show(sender, args[1]);
        if (sub.equals("strikes") && args.length > 1) return strikes(sender, args);
        say(sender, "&7ChatFilter " + BuildInfo.describe());
        if (sender.hasPermission(FilterService.PERM_ADMIN)) {
            say(sender, "&7/" + label + " reload | test <text> | add/remove <list> <entry> | list <list> | strikes <player> [clear] | alerts [on|off|toggle]");
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        Settings s = plugin.reloadSettings();
        if (s.problems.isEmpty()) {
            say(sender, "&7Reloaded. No complaints.");
        } else {
            say(sender, "&eReloaded, with " + s.problems.size() + " thing(s) skipped:");
            for (String p : s.problems) say(sender, "&7  " + p);
        }
        return true;
    }

    private boolean test(CommandSender sender, String text) {
        Settings s = plugin.settings();
        List<Hit> hits = s.analyzer.analyze(text);
        Outcome o = s.judge.judge(text, hits);
        if (hits.isEmpty()) {
            say(sender, "&7Nothing caught. That would go through untouched.");
            return true;
        }
        for (Hit h : hits) {
            CategoryPolicy p = s.judge.policy(h.category());
            String where = h.wholeMessage() ? "the whole line" : "'" + text.substring(h.span().start(), h.span().end()) + "'";
            sender.sendMessage(Text.fillLiteral("&7" + h.category() + " &8(" + (p.enabled() ? p.action() : "off")
                    + ")&7: {w}", "w", where + " via " + h.term()));
        }
        String label;
        switch (o.kind()) {
            case MASK: label = "&7Result: masked. Others see: &f{t}"; break;
            case REWRITE: label = "&7Result: rewritten to: &f{t}"; break;
            case SHADOW: label = "&7Result: shadowed. The sender sees it sent; nobody else sees it."; break;
            case BLOCK: label = "&7Result: blocked."; break;
            default: label = "&7Result: logged only."; break;
        }
        sender.sendMessage(Text.fillLiteral(label, "t", o.censoredText(text)));
        say(sender, "&8(Spam checks and permissions are not part of a test.)");
        return true;
    }

    private boolean edit(CommandSender sender, boolean add, String name, String entry) {
        String path = LISTS.get(name.toLowerCase(Locale.ROOT));
        if (path == null) {
            say(sender, "&7There is no list called '" + name + "'. Try: " + join(LISTS.keySet()));
            return true;
        }
        List<String> current = new ArrayList<String>(Settings.list(plugin.getConfig(), path));
        boolean present = false;
        for (String e : current) present |= e.equalsIgnoreCase(entry);
        if (add == present) {
            say(sender, add ? "&7'" + entry + "' is already on " + name + "." : "&7'" + entry + "' was not on " + name + " to begin with.");
            return true;
        }
        if (add) {
            current.add(entry);
        } else {
            for (int i = current.size() - 1; i >= 0; i--) {
                if (current.get(i).equalsIgnoreCase(entry)) current.remove(i);
            }
        }
        plugin.getConfig().set(path, current);
        plugin.saveConfig();
        Settings s = plugin.reloadSettings();
        say(sender, "&7" + (add ? "Added '" + entry + "' to " : "Removed '" + entry + "' from ") + name + ". Saved to config.yml.");
        for (String p : s.problems) say(sender, "&e  " + p);
        return true;
    }

    private boolean show(CommandSender sender, String name) {
        String path = LISTS.get(name.toLowerCase(Locale.ROOT));
        if (path == null) {
            say(sender, "&7There is no list called '" + name + "'. Try: " + join(LISTS.keySet()));
            return true;
        }
        List<String> entries = Settings.list(plugin.getConfig(), path);
        say(sender, entries.isEmpty() ? "&7" + name + " is empty."
                : "&7" + name + " (" + entries.size() + "): &f" + join(entries).replace(", ", "&7, &f"));
        return true;
    }

    private boolean strikes(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            say(sender, "&7Nobody called " + args[1] + " is online.");
            return true;
        }
        if (args.length > 2 && args[2].equalsIgnoreCase("clear")) {
            plugin.service().clearStrikes(target.getUniqueId());
            say(sender, "&7" + target.getName() + "'s slate is clean.");
            return true;
        }
        int n = plugin.service().strikes(target.getUniqueId());
        say(sender, n == 0 ? "&7" + target.getName() + " has no strikes."
                : "&7" + target.getName() + " has " + n + " strike" + (n == 1 ? "" : "s") + ".");
        return true;
    }

    /** Tri-state, like every switch in this family: on and off for a script, bare for a human. */
    private boolean alerts(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission(FilterService.PERM_ALERTS)) {
            say(sender, "&7Alerts are for staff in game.");
            return true;
        }
        Player p = (Player) sender;
        String arg = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
        boolean on = arg.equals("on") || (!arg.equals("off") && plugin.alerts().isOff(p));
        plugin.alerts().setOff(p, !on);
        say(sender, on ? "&7Alerts on. You will hear about it." : "&7Alerts off. Ignorance, restored.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && Arrays.asList("add", "remove", "list").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (String s : LISTS.keySet()) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("alerts")) {
            for (String s : new String[] {"on", "off", "toggle"}) if (s.startsWith(args[1])) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("strikes")) {
            return null; // Bukkit's default: online player names
        }
        return out;
    }

    private static String rest(String[] args, int from) {
        StringBuilder b = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) b.append(' ');
            b.append(args[i]);
        }
        return b.toString();
    }

    private static String join(Iterable<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (b.length() > 0) b.append(", ");
            b.append(p);
        }
        return b.toString();
    }

    private static void say(CommandSender sender, String text) {
        sender.sendMessage(Text.colour(text));
    }
}
