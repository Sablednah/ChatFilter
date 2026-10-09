package com.sablednah.chatfilter.core;

/** How {@link Action#REPLACE} writes over a caught word. */
public enum CensorStyle {
    /** The whole word becomes the configured text — the old plugin's {@code censorText}, {@code !@$#}. */
    FIXED,
    /** Every character becomes the configured character, so the length survives: {@code ****}. */
    REPEAT,
    /** Cartoon swearing the length of the word: {@code @#$%}. */
    GRAWLIX
}
