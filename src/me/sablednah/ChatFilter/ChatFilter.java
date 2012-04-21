package me.sablednah.ChatFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatFilter extends JavaPlugin {

	public static ChatFilter plugin;
	public final static Logger logger = Logger.getLogger("Minecraft");
	public final PlayerChatListener playerListener = new PlayerChatListener(this);

	public static boolean debugMode;
	public static boolean showInConsole;
	public static boolean kick;
	public static boolean censor;

	private FileConfiguration LangConfig = null;
	private File LangConfigurationFile = null;

	public static List<Object> langProfanity;
	public static List<Object> profanityWordMatch;
	public static String profanityMessage;
	public static List<Object> langTriggers;
	public static String eleven;
	public static String censorText;

	private ChatFilterCommandExecutor myCommands;
	public static PluginDescriptionFile pdfFile;
	public static String myName;	
	private String VersionNew;
	private String VersionCurrent;



	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		ChatFilter.logger.info(pdfFile.getName() + " : --- END OF LINE ---");
	}

	@Override
	public void onEnable() {
		plugin = this;
		pdfFile = plugin.getDescription();
		myName = pdfFile.getName();
		VersionCurrent = pdfFile.getVersion();

		logger.info("[" + myName + "] Version " + pdfFile.getVersion() + " starting.");

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);

		myCommands = new ChatFilterCommandExecutor(this);
		getCommand("ChatFilter").setExecutor(myCommands);

		loadConfiguration(true);

		/**
		 *  Schedule a version check every 6 hours for update notification .
		 */
		this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				try {
					VersionNew = getNewVersion(VersionCurrent);
					String VersionOld = getDescription().getVersion();
					if (!VersionNew.contains(VersionOld)) {
						logger.warning(VersionNew + " is available. You're using " + VersionOld);
						logger.warning("http://dev.bukkit.org/server-mods/chatfilter/");
					}
				} catch (Exception e) {
					// ignore exceptions
				}
			}
		}, 0, 432000);

		logger.info("[" + myName + "] Online.");
	}


	/**
	 * Initialise config file 
	 */
	public void loadConfiguration(Boolean firstrun) {
		plugin.getConfig().options().copyDefaults(true);

		if (firstrun) {
			String headertext;
			headertext="Default ChatFilter Config file\r\n\r\n";
			headertext+="debugMode: [true|false] Enable extra debug info in logs.\r\n";
			headertext+="kick: [true|false] Kick players after warning.\r\n";
			headertext+="showInConsole: [true|false] Show offending player and message in console.\r\n";
			headertext+="\r\n";

			plugin.getConfig().options().header(headertext);
			plugin.getConfig().options().copyHeader(true);
		} else {
			plugin.reloadConfig();
		}

		ChatFilter.debugMode = plugin.getConfig().getBoolean("debugMode");
		ChatFilter.showInConsole = plugin.getConfig().getBoolean("showInConsole");
		ChatFilter.kick = plugin.getConfig().getBoolean("kick");
		ChatFilter.censor=plugin.getConfig().getBoolean("censor");

		plugin.saveConfig();

		if (firstrun) {
			plugin.getLangConfig();
		} else {
			plugin.reloadLangConfig();
		}
		
		ChatFilter.langProfanity = plugin.getLangConfig().getList("profanity");
		ChatFilter.profanityWordMatch = plugin.getLangConfig().getList("profanityWordMatch");
		ChatFilter.profanityMessage = plugin.getLangConfig().getString("profanityMessage");
		ChatFilter.langTriggers = plugin.getLangConfig().getList("triggers");
		ChatFilter.eleven = plugin.getLangConfig().getString("triggerPhrase");
		ChatFilter.censorText=plugin.getLangConfig().getString("censorText");

		plugin.saveLangConfig();
	}

	/**
	 * Get latest version of plugin from remote server.
	 * 
	 * @param VersionCurrent  String of current version to compare (returned in cases such as update server is unavailable).
	 * @return returns Latest version as String
	 * @throws Exception
	 */
	public String getNewVersion(String VersionCurrent) throws Exception {
		String urlStr = "http://sablekisska.co.uk/asp/chatversion.asp";
		try {

			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			HttpURLConnection.setFollowRedirects(true);
			conn.setRequestProperty ( "User-agent", "[ChatFilter Plugin] "+VersionCurrent);
			String inStr = null;
			inStr = convertStreamToString(conn.getInputStream());
			return inStr;

		}
		catch (Exception localException) {}
		return VersionCurrent;
	}

	/**
	 * Converts InputStream to String
	 * 
	 * One-line 'hack' to convert InputStreams to strings.
	 * 
	 * @param	is  The InputStream to convert
	 * @return	returns a String version of 'is'
	 */
	public String convertStreamToString(InputStream is) { 
		return new Scanner(is).useDelimiter("\\A").next();
	}




	public void reloadLangConfig() {
		if (LangConfigurationFile  == null) {
			LangConfigurationFile  = new File(getDataFolder(), "lang.yml");
		}
		LangConfig  = YamlConfiguration.loadConfiguration(LangConfigurationFile);
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

