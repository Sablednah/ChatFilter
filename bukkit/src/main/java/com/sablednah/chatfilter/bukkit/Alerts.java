package com.sablednah.chatfilter.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Who has turned staff alerts off, in {@code plugins/ChatFilter/alerts-off.yml}.
 *
 * <p>A file rather than per-player data: persistent data containers arrived in 1.14 and this has to
 * work on 1.7. It is a short list of the staff who said "not now", read once and written on change.</p>
 */
public final class Alerts {

    private final File file;
    private final Logger log;
    private final Set<UUID> off = new HashSet<UUID>();

    public Alerts(File dataFolder, Logger log) {
        this.file = new File(dataFolder, "alerts-off.yml");
        this.log = log;
        if (file.exists()) {
            for (String s : YamlConfiguration.loadConfiguration(file).getStringList("off")) {
                try {
                    off.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                    // a hand-edited line that is not a UUID: skip it
                }
            }
        }
    }

    /** Staff with the node who have not turned alerts off. */
    public synchronized boolean receives(Player player) {
        return player.hasPermission(FilterService.PERM_ALERTS) && !off.contains(player.getUniqueId());
    }

    public synchronized boolean isOff(Player player) {
        return off.contains(player.getUniqueId());
    }

    public synchronized void setOff(Player player, boolean value) {
        if (value) off.add(player.getUniqueId()); else off.remove(player.getUniqueId());
        YamlConfiguration y = new YamlConfiguration();
        List<String> ids = new ArrayList<String>();
        for (UUID u : off) ids.add(u.toString());
        y.set("off", ids);
        try {
            y.save(file);
        } catch (IOException e) {
            log.warning("Could not save " + file + ": " + e);
        }
    }
}
