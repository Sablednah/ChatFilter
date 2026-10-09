package com.sablednah.chatfilter.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Everyone online.
 *
 * <p>Not {@code Bukkit.getOnlinePlayers()}: it returned an array until 1.8 and a {@code Collection}
 * after, a different method descriptor, so one compiled call cannot run on both sides of that.
 * {@code World.getPlayers()} has returned a {@code List} the whole time. MobHealth's Bukkit repo
 * learned this first.</p>
 */
public final class Online {

    private Online() {}

    public static List<Player> players() {
        List<Player> out = new ArrayList<Player>();
        for (World w : Bukkit.getWorlds()) {
            out.addAll(w.getPlayers());
        }
        return out;
    }
}
