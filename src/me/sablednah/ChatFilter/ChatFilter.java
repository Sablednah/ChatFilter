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
	public static boolean kick;

	private FileConfiguration LangConfig = null;
	private File LangConfigurationFile = null;
	
	public static List<Object> langProfanity;
	public static String profanityMessage;
	public static List<Object> langTriggers;
	public static String eleven;

	private ChatFilterCommandExecutor myCommands;
	private String VersionNew;
	private String VersionCurrent;



	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		ChatFilter.logger.info(pdfFile.getName() + " : --- END OF LINE ---");
	}
	
	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		String myName=pdfFile.getName();
		VersionCurrent = getDescription().getVersion().substring(0, 3);

		logger.info("[" + myName + "] Version " + pdfFile.getVersion() + " starting.");

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);

		myCommands = new ChatFilterCommandExecutor(this);
		getCommand("ChatFilter").setExecutor(myCommands);
		
		loadConfiguration();

		/**
		 *  Schedule a version check every 6 hours for update notification .
		 */
		this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				try {
					VersionNew = getNewVersion(VersionCurrent);
					String VersionOld = getDescription().getVersion().substring(0, 3);
					if (Float.parseFloat(VersionNew) > Float.parseFloat(VersionOld)) {
						logger.warning(VersionNew + " is available. You're using " + VersionOld);
						logger.warning("http://dev.bukkit.org/server-mods/chatfilter/");
					}
				} catch (Exception e) {
					// ignore exceptions
				}
			}
		}, 0, 5184000);

		logger.info("[" + myName + "] Online.");
	}


	/**
	 * Initialise config file 
	 */
	public void loadConfiguration() {
		getConfig().options().copyDefaults(true);

		String headertext;
		headertext="Default ChatFilter Config file\r\n\r\n";
		headertext+="debugMode: [true|false] Enable extra debug info in logs.\r\n";
		headertext+="kick: [true|false] Kick players after warning.\r\n";
		headertext+="\r\n";

		getConfig().options().header(headertext);
		getConfig().options().copyHeader(true);

		debugMode = getConfig().getBoolean("debugMode");
		kick = getConfig().getBoolean("kick");

		saveConfig();

		getLangConfig();

		langProfanity = getLangConfig().getList("profanity");
		profanityMessage = getLangConfig().getString("profanityMessage");
		langTriggers = getLangConfig().getList("triggers");
		eleven = getLangConfig().getString("triggerPhrase");


		saveLangConfig();
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

