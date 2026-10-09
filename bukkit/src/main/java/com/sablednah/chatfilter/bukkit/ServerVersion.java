package com.sablednah.chatfilter.bukkit;

import org.bukkit.Bukkit;

/**
 * The running server's Minecraft version, from {@code Bukkit.getBukkitVersion()}:
 * {@code 1.20.4-R0.1-SNAPSHOT}, {@code 26.3-R0.1-SNAPSHOT}. Only hex colours care.
 */
public final class ServerVersion {

    private static final int MAJOR;
    private static final int MINOR;

    static {
        int major = 1, minor = 7;
        try {
            String v = Bukkit.getBukkitVersion();
            String[] parts = v.split("-")[0].split("\\.");
            major = Integer.parseInt(parts[0]);
            minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (RuntimeException e) {
            // No server (a unit test) or an unreadable version: assume the oldest, the safe side.
        }
        MAJOR = major;
        MINOR = minor;
    }

    private ServerVersion() {}

    public static boolean atLeast(int major, int minor) {
        return MAJOR > major || (MAJOR == major && MINOR >= minor);
    }

    public static String describe() {
        return MAJOR + "." + MINOR;
    }
}
