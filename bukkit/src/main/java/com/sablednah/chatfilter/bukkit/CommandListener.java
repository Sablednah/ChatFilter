package com.sablednah.chatfilter.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.sablednah.chatfilter.core.Outcome;

/**
 * Chat that arrives as a command: the old plugin's best-loved feature, and the same event it used.
 *
 * <p>A command has no per-viewer copy (the plugin that owns {@code /msg} delivers it), so a masked
 * word is rewritten as {@code ####} instead, which reads the same.</p>
 */
public final class CommandListener implements Listener {

    private final ChatFilterPlugin plugin;

    public CommandListener(ChatFilterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Settings s = plugin.settings();
        if (!s.commandsEnabled) return;
        String full = event.getMessage();
        if (!full.startsWith("/")) return;
        String line = full.substring(1);
        CommandMatcher.Match m = CommandMatcher.match(line, s.commands);
        if (m == null || m.messageStart >= line.length()) return;

        Player player = event.getPlayer();
        String text = line.substring(m.messageStart);
        Outcome outcome = plugin.service().handle(player, text, "/" + m.label, false);
        switch (outcome.kind()) {
            case BLOCK:
            case SHADOW: // a command cannot pretend to have run: a shadow is a silent block here
                event.setCancelled(true);
                break;
            case REWRITE:
            case MASK:
                event.setMessage("/" + line.substring(0, m.messageStart) + outcome.censoredText(text));
                break;
            default:
                break;
        }
    }
}
