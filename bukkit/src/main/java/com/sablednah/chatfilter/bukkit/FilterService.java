package com.sablednah.chatfilter.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.sablednah.chatfilter.core.Action;
import com.sablednah.chatfilter.core.CategoryPolicy;
import com.sablednah.chatfilter.core.Hit;
import com.sablednah.chatfilter.core.Outcome;
import com.sablednah.chatfilter.core.Responder;
import com.sablednah.chatfilter.core.SpamGuard;
import com.sablednah.chatfilter.core.Strikes;

/**
 * Where a message is judged, and where everything that follows from the judgement happens: once
 * per message. The NeoForge mod's service, with Bukkit's permissions and scheduler.
 *
 * <p>Called from Bukkit's async chat threads as well as the main thread. The settings are an
 * immutable snapshot, the core state classes lock themselves, and anything that touches the world
 * (a strike's command, a keyword reply) is scheduled onto the main thread.</p>
 */
public final class FilterService {

    public static final String PERM_CHAT = "chatfilter.chat";
    public static final String PERM_BYPASS = "chatfilter.bypass";
    public static final String PERM_SEE = "chatfilter.see";
    public static final String PERM_LINKS = "chatfilter.links";
    public static final String PERM_ALERTS = "chatfilter.alerts";
    public static final String PERM_ADMIN = "chatfilter.admin";

    /** The decider for a player without {@code chatfilter.chat}. */
    public static final String NO_CHAT = "chat";

    private final ChatFilterPlugin plugin;
    private final SpamGuard spam = new SpamGuard();
    private final Strikes strikes = new Strikes();
    private final Responder responder = new Responder();

    public FilterService(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    /** What would happen, with no side effects. For {@code /chatfilter test}. */
    public Outcome peek(String text) {
        Settings s = plugin.settings();
        return s.judge.judge(text, s.analyzer.analyze(text));
    }

    /**
     * Judge a message and act on it.
     *
     * @param channel        where it was said, for logs and alerts: {@code chat}, {@code /msg}, …
     * @param conversational whether keyword replies may fire: public chat only
     */
    public Outcome handle(Player player, String text, String channel, boolean conversational) {
        Settings s = plugin.settings();
        if (!player.hasPermission(PERM_CHAT)) {
            tell(player, s, s.msgNoChat);
            return new Outcome(Outcome.Kind.BLOCK, null, Collections.<com.sablednah.chatfilter.core.Span>emptyList(),
                    NO_CHAT, Collections.<Hit>emptyList());
        }
        long now = System.currentTimeMillis();
        if (player.hasPermission(PERM_BYPASS)) {
            if (conversational) respond(player, text, s, now);
            return Outcome.pass(Collections.<Hit>emptyList());
        }

        List<Hit> hits = new ArrayList<Hit>(s.analyzer.analyze(text));
        if (!hits.isEmpty() && player.hasPermission(PERM_LINKS)) {
            List<Hit> kept = new ArrayList<Hit>();
            for (Hit h : hits) {
                if (!h.category().equals("links")) kept.add(h);
            }
            hits = kept;
        }
        String caught = spam.check(player.getUniqueId(), text, now,
                s.judge.policy("repeat").enabled() ? s.repeatSeconds : 0, s.floodMessages,
                s.judge.policy("flood").enabled() ? s.floodSeconds : 0);
        if (caught != null) {
            hits.add(new Hit(caught, null, caught));
        }

        Outcome outcome = s.judge.judge(text, hits);
        report(player, text, channel, outcome, s, now);
        if (!outcome.stops()) {
            spam.delivered(player.getUniqueId(), text, now);
            if (conversational) respond(player, text, s, now);
        }
        return outcome;
    }

    public void forget(Player player) {
        spam.forget(player.getUniqueId());
    }

    public int strikes(UUID player) {
        return strikes.count(player, System.currentTimeMillis(), plugin.settings().strikeDecayMillis);
    }

    public void clearStrikes(UUID player) {
        strikes.clear(player);
    }

    // ------------------------------------------------------------------ internals

    private void report(Player player, String text, String channel, Outcome outcome, Settings s, long now) {
        List<Hit> counted = new ArrayList<Hit>();
        for (Hit h : outcome.hits()) {
            if (s.judge.policy(h.category()).enabled()) counted.add(h);
        }
        if (counted.isEmpty()) return;

        Set<String> categories = new LinkedHashSet<String>();
        Set<String> terms = new LinkedHashSet<String>();
        for (Hit h : counted) {
            categories.add(h.category());
            terms.add(h.term());
        }
        String cats = join(categories, "+");
        String who = player.getName();

        if (s.alertConsole) {
            plugin.getLogger().info(who + " (" + channel + ", " + cats + " -> " + outcome.kind() + "): " + text);
        }

        boolean alert = false;
        for (String c : categories) alert |= s.judge.policy(c).alert();
        if (alert && s.alertStaff) {
            Map<String, String> v = new HashMap<String, String>();
            v.put("player", who);
            v.put("channel", channel);
            v.put("category", cats);
            v.put("term", join(terms, ", "));
            final String line = Text.fillLiteral(Text.fill(s.msgAlert, v), "message", text);
            final UUID sender = player.getUniqueId();
            onMain(new Runnable() {
                @Override
                public void run() {
                    for (Player staff : Online.players()) {
                        if (!staff.getUniqueId().equals(sender) && plugin.alerts().receives(staff)) {
                            staff.sendMessage(line);
                        }
                    }
                }
            });
        }

        // The sender, if the winning category says so, and never for the silent actions.
        if (outcome.decider() != null && (outcome.kind() == Outcome.Kind.BLOCK
                || outcome.kind() == Outcome.Kind.REWRITE)) {
            CategoryPolicy p = s.judge.policy(outcome.decider());
            if (p.tellSender() && (p.action() == Action.BLOCK || p.action() == Action.REPLACE)) {
                tell(player, s, p.message());
            }
        }

        if (s.strikesEnabled) {
            int add = 0;
            for (String c : categories) add += s.judge.policy(c).strikes();
            if (add > 0) {
                int[] total = new int[1];
                List<Strikes.Step> crossed = strikes.add(player.getUniqueId(), add, now, s.strikeDecayMillis,
                        s.ladder, total);
                for (Strikes.Step step : crossed) {
                    runStep(player, step, total[0], s);
                }
            }
        }
    }

    private void runStep(Player player, Strikes.Step step, final int total, Settings s) {
        final Map<String, String> v = new HashMap<String, String>();
        v.put("player", player.getName());
        v.put("strikes", String.valueOf(total));
        final String name = player.getName();
        if ("alert".equals(step.kind())) {
            final String line = Text.colour(Text.fill(s.msgStrikeAlert, v));
            onMain(new Runnable() {
                @Override
                public void run() {
                    for (Player staff : Online.players()) {
                        if (plugin.alerts().receives(staff)) staff.sendMessage(line);
                    }
                    plugin.getLogger().info(name + " has " + total + " strikes");
                }
            });
        } else if ("tell".equals(step.kind())) {
            tell(player, s, Text.fill(step.value(), v));
        } else if ("command".equals(step.kind())) {
            final String command = Text.fill(step.value(), v);
            // On the main thread, and never in the middle of the chat event it is reacting to.
            onMain(new Runnable() {
                @Override
                public void run() {
                    plugin.getLogger().info("strike " + total + " for " + name + ": /" + command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            });
        }
    }

    private void respond(Player player, String text, Settings s, long now) {
        if (!s.responsesEnabled || s.responses.isEmpty()) return;
        final List<String> replies = responder.respond(text, s.responses, now, s.responseCooldown);
        if (replies.isEmpty()) return;
        final Map<String, String> v = Collections.singletonMap("player", player.getName());
        // A few ticks later, so the reply lands after the line it answers rather than racing it.
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                for (String reply : replies) {
                    Text.broadcast(Text.colour(Text.fill(reply, v)));
                }
            }
        }, s.responseDelayTicks);
    }

    private void tell(final Player player, Settings s, String template) {
        if (template == null || template.trim().isEmpty()) return;
        player.sendMessage(Text.colour(s.prefix + Text.fill(template,
                Collections.singletonMap("player", player.getName()))));
    }

    private void onMain(Runnable r) {
        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    private static String join(Iterable<String> parts, String sep) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (b.length() > 0) b.append(sep);
            b.append(p);
        }
        return b.toString();
    }
}
