package me.sablednah.ChatFilter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


public class ChatFilterCommandExecutor implements CommandExecutor {
	public ChatFilter plugin;

	public ChatFilterCommandExecutor(ChatFilter instance) {
		this.plugin=instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("ChatFilter")){
			if (args.length > 0 && args[0].toLowerCase().equals("reload")) {
				
				plugin.reloadConfig();
				ChatFilter.debugMode = plugin.getConfig().getBoolean("debugMode");
				ChatFilter.showInConsole = plugin.getConfig().getBoolean("showInConsole");
				ChatFilter.kick = plugin.getConfig().getBoolean("kick");
				ChatFilter.censor=plugin.getConfig().getBoolean("censor");

				plugin.reloadLangConfig();
				ChatFilter.langProfanity = plugin.getLangConfig().getList("profanity");
				ChatFilter.profanityWordMatch = plugin.getLangConfig().getList("profanityWordMatch");
				ChatFilter.profanityMessage = plugin.getLangConfig().getString("profanityMessage");
				ChatFilter.langTriggers = plugin.getLangConfig().getList("triggers");
				ChatFilter.eleven = plugin.getLangConfig().getString("triggerPhrase");
				ChatFilter.censorText=plugin.getLangConfig().getString("censorText");
				
				System.out.print("[ChatFilter] Reloaded.");
				
				return true;
			}
		}

		return false; 
	}
}
