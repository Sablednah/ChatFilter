package com.sablednah.chatfilter.core;

/**
 * What a category does with a message it catches. Ordered from gentlest to firmest, which is also
 * the order they lose in when two categories disagree about one message.
 */
public enum Action {
    /** Record it — console, staff alerts, strikes — and change nothing. */
    LOG,
    /**
     * Hide the caught words from everyone but the sender, through the client's own chat filter:
     * the line stays signed vanilla chat, the words show as {@code ###} with a "filtered by the
     * server" hover, and the sender sees exactly what they typed. The silent mode the old plugin's
     * page asked for most.
     */
    MASK,
    /** Rewrite the caught words for everyone, the sender included. For caps, lowercase the line. */
    REPLACE,
    /**
     * Soft-mute the whole line: the sender sees it sent, nobody else sees it at all. Chat only — a
     * command cannot pretend to have run, so for commands this behaves as a silent {@link #BLOCK}.
     */
    SHADOW,
    /** Stop the message, and tell the sender if the category has a message. */
    BLOCK
}
