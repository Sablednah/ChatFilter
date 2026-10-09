package com.sablednah.chatfilter.bukkit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.sablednah.chatfilter.core.Outcome;

/**
 * Public chat.
 *
 * <h2>The silent mask, without a mask</h2>
 *
 * <p>On NeoForge a masked word rides inside the chat message as vanilla's filter mask. Bukkit has
 * no hook for that, but it does have the recipient set, which is per message and mutable. So: the
 * sender and holders of {@code chatfilter.see} stay recipients and get the real line, untouched;
 * everyone else is taken off it and sent a copy with {@code ####} in the same chat format. The
 * sender sees exactly what they typed and is told nothing, which is the point.</p>
 *
 * <p>Done in two halves. The recipients change at HIGHEST, after formatting plugins (which work at
 * NORMAL and below) have had their turn. The copies go out at MONITOR, using the format as it
 * finally stands, and only if nobody cancelled the event in between. The two halves run on the
 * same thread in the same dispatch, so a thread-local carries the pending copy across.</p>
 *
 * <p>A shadowed line is the same trick taken all the way: the sender is the only recipient left,
 * so they see their own line in its real format, and nobody else sees anything.</p>
 */
public final class ChatListener implements Listener {

    /** The copy waiting to go out at MONITOR, for the event currently being dispatched. */
    private static final class Pending {
        final AsyncPlayerChatEvent event;
        final Set<Player> hidden;
        final String censored;

        Pending(AsyncPlayerChatEvent event, Set<Player> hidden, String censored) {
            this.event = event;
            this.hidden = hidden;
            this.censored = censored;
        }
    }

    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<Pending>();

    private final FilterService service;

    public ChatListener(FilterService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        PENDING.remove();
        Player player = event.getPlayer();
        String raw = event.getMessage();
        Outcome outcome = service.handle(player, raw, "chat", true);
        switch (outcome.kind()) {
            case BLOCK:
                event.setCancelled(true);
                break;
            case SHADOW:
                try {
                    event.getRecipients().retainAll(Collections.singleton(player));
                } catch (UnsupportedOperationException e) {
                    // Another plugin built this event with a fixed audience. A plain block, then,
                    // plus the sender's own copy so they still see it "sent".
                    event.setCancelled(true);
                    player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), raw));
                }
                break;
            case REWRITE:
                event.setMessage(outcome.text());
                break;
            case MASK:
                Set<Player> hidden = new HashSet<Player>();
                for (Player viewer : event.getRecipients()) {
                    if (viewer != player && !viewer.hasPermission(FilterService.PERM_SEE)) {
                        hidden.add(viewer);
                    }
                }
                if (hidden.isEmpty()) break;
                try {
                    event.getRecipients().removeAll(hidden);
                    PENDING.set(new Pending(event, hidden, outcome.censoredText(raw)));
                } catch (UnsupportedOperationException e) {
                    // No per-viewer copy possible: everyone gets the censored text instead.
                    event.setMessage(outcome.censoredText(raw));
                }
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatDelivered(AsyncPlayerChatEvent event) {
        Pending pending = PENDING.get();
        PENDING.remove();
        if (pending == null || pending.event != event || event.isCancelled()) return;
        String line = String.format(event.getFormat(), event.getPlayer().getDisplayName(), pending.censored);
        for (Player viewer : pending.hidden) {
            viewer.sendMessage(line);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.forget(event.getPlayer());
    }
}
