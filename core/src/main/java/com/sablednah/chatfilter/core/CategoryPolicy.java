package com.sablednah.chatfilter.core;

/**
 * How one category treats what it catches.
 *
 * @param enabled    whether the category runs at all
 * @param action     what happens to the message
 * @param tellSender whether the sender is sent {@code message} (blank always means say nothing)
 * @param message    what they are told; {@code {player}} and the old {@code %N} both work
 * @param strikes    strikes added per caught message; 0 for none
 * @param alert      whether staff with alerts on are told
 */
public final class CategoryPolicy {

    private final boolean enabled;
    private final Action action;
    private final boolean tellSender;
    private final String message;
    private final int strikes;
    private final boolean alert;

    public CategoryPolicy(boolean enabled, Action action, boolean tellSender, String message, int strikes, boolean alert) {
        this.enabled = enabled;
        this.action = action;
        this.tellSender = tellSender;
        this.message = message;
        this.strikes = strikes;
        this.alert = alert;
    }

    public boolean enabled() {
        return enabled;
    }

    public Action action() {
        return action;
    }

    public boolean tellSender() {
        return tellSender;
    }

    public String message() {
        return message;
    }

    public int strikes() {
        return strikes;
    }

    public boolean alert() {
        return alert;
    }

    public static final CategoryPolicy OFF = new CategoryPolicy(false, Action.LOG, false, "", 0, false);
}
