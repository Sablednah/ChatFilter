package com.sablednah.chatfilter.bukkit;

import java.io.InputStream;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import com.sablednah.chatfilter.core.BuildInfo;

/**
 * ChatFilter for Bukkit, Spigot and Paper, 1.7.2 to 26.3: the back-port of ChatFilter ReForged.
 *
 * <p>Everything that decides (normalising, matching, judging, strikes, spam) is the NeoForge mod's
 * {@code core}, copied verbatim. This package is the thin layer that hands it Bukkit's chat and
 * command events and does what it decides.</p>
 */
public final class ChatFilterPlugin extends JavaPlugin {

    private volatile Settings settings;
    private FilterService service;
    private Alerts alerts;

    @Override
    public void onEnable() {
        // The build stamp is the half of a bug report that matters: it says what actually RAN.
        getLogger().info("ChatFilter " + BuildInfo.describe() + " on " + getServer().getBukkitVersion());

        if (Migration.isLegacy(getDataFolder())) {
            InputStream defaults = getResource("config.yml");
            try {
                List<String> done = Migration.migrate(getDataFolder(), defaults, getLogger());
                getLogger().info("Migrated a ChatFilter 2.x config (originals kept as *-2.x.yml.bak):");
                for (String d : done) getLogger().info("  " + d);
                getLogger().info("Your old block/censor behaviour is kept. For the silent mode - others see ####, "
                        + "the sender sees what they typed - set profanity.action to MASK.");
            } catch (Exception e) {
                getLogger().severe("Could not migrate the 2.x config, starting from defaults: " + e);
            }
        }
        saveDefaultConfig();
        reloadSettings();

        alerts = new Alerts(getDataFolder(), getLogger());
        service = new FilterService(this);
        getServer().getPluginManager().registerEvents(new ChatListener(service), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        Commands commands = new Commands(this);
        getCommand("chatfilter").setExecutor(commands);
        getCommand("chatfilter").setTabCompleter(commands);

        if (Boolean.getBoolean("chatfilter.selftest")) {
            new SelfTest(this).run();
        }
    }

    /** Re-read config.yml and swap the compiled settings in whole. */
    public Settings reloadSettings() {
        reloadConfig();
        Settings next = Settings.load(getConfig());
        settings = next;
        for (String p : next.problems) {
            getLogger().warning("config: " + p);
        }
        return next;
    }

    public Settings settings() {
        return settings;
    }

    public FilterService service() {
        return service;
    }

    public Alerts alerts() {
        return alerts;
    }
}
