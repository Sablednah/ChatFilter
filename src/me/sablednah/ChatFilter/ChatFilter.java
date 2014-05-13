package me.sablednah.ChatFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatFilter extends JavaPlugin {

	public static ChatFilter			plugin;
	public static Logger				logger;
	public final PlayerChatListener		playerListener			= new PlayerChatListener(this);

	public static boolean				debugMode;
	public static boolean				showInConsole;
	public static boolean				kick;
	public static boolean				censor;
	public static boolean				agressiveMatching;

	private FileConfiguration			LangConfig				= null;
	private File						LangConfigurationFile	= null;

	public static List<String>			langProfanity;
	public static List<String>			profanityWordMatch;
	public static String				profanityMessage;
	public static List<String>			langTriggers;
	public static String				eleven;
	public static String				censorText;
	public static String				blockMessage;
	public static List<String>			commands;

	private ChatFilterCommandExecutor	myCommands;

	@Override
	public void onDisable() {
		logger.info("--- END OF LINE ---");
	}

	@Override
	public void onEnable() {
		logger = this.getLogger();

		plugin = this;

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);

		myCommands = new ChatFilterCommandExecutor(this);
		getCommand("ChatFilter").setExecutor(myCommands);

		loadConfiguration(true);

		logger.info("Online.");
	}

	/**
	 * Initialise config file
	 */
	@SuppressWarnings("unchecked")
	public void loadConfiguration(Boolean firstrun) {
		plugin.getConfig().options().copyDefaults(true);

		if (firstrun) {
			String headertext;
			headertext = "Default ChatFilter Config file\r\n\r\n";
			headertext += "debugMode: [true|false] Enable extra debug info in logs.\r\n";
			headertext += "kick: [true|false] Kick players after warning.\r\n";
			headertext += "showInConsole: [true|false] Show offending player and message in console.\r\n";
			headertext += "censor:  [true|false] Replace offending text instead of blocking message.\r\n";
			headertext += "aggressive: [true|false]  Attempts to match more words by looking for 3=e 0=o etc.\r\n";
			headertext += "\r\n";

			plugin.getConfig().options().header(headertext);
			plugin.getConfig().options().copyHeader(true);
		} else {
			plugin.reloadConfig();
		}

		ChatFilter.debugMode = plugin.getConfig().getBoolean("debugMode");
		ChatFilter.showInConsole = plugin.getConfig().getBoolean("showInConsole");
		ChatFilter.kick = plugin.getConfig().getBoolean("kick");
		ChatFilter.censor = plugin.getConfig().getBoolean("censor");
		ChatFilter.agressiveMatching = plugin.getConfig().getBoolean("aggressive");

		plugin.saveConfig();

		if (firstrun) {
			plugin.getLangConfig();
		} else {
			plugin.reloadLangConfig();
		}

		ChatFilter.langProfanity = (List<String>) plugin.getLangConfig().getList("profanity");
		ChatFilter.profanityWordMatch = (List<String>) plugin.getLangConfig().getList("profanityWordMatch");
		ChatFilter.profanityMessage = plugin.getLangConfig().getString("profanityMessage");
		ChatFilter.langTriggers = (List<String>) plugin.getLangConfig().getList("triggers");
		ChatFilter.eleven = plugin.getLangConfig().getString("triggerPhrase");
		ChatFilter.censorText = plugin.getLangConfig().getString("censorText");
		ChatFilter.blockMessage = plugin.getLangConfig().getString("blockMessage");
		ChatFilter.commands = (List<String>) plugin.getLangConfig().getList("commands");

		plugin.saveLangConfig();
	}

	public void reloadLangConfig() {
		if (LangConfigurationFile == null) {
			LangConfigurationFile = new File(getDataFolder(), "lang.yml");
		}
		LangConfig = YamlConfiguration.loadConfiguration(LangConfigurationFile);
		LangConfig.options().copyDefaults(true);

		// Look for defaults in the jar
		InputStream defConfigStream = getResource("lang.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			LangConfig.setDefaults(defConfig);
		}
	}

	public FileConfiguration getLangConfig() {
		if (LangConfig == null) {
			reloadLangConfig();
		}
		return LangConfig;
	}

	public void saveLangConfig() {
		if (LangConfig == null || LangConfigurationFile == null) {
			return;
		}
		try {
			LangConfig.save(LangConfigurationFile);
		} catch (IOException ex) {
			logger.severe("Could not save Lang config to " + LangConfigurationFile + " " + ex);
		}
	}
}
