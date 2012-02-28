package me.sablednah.ChatFilter;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;


public class PlayerChatListener implements Listener  {
	public ChatFilter plugin;

	public PlayerChatListener(ChatFilter instance) {
		this.plugin=instance;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(PlayerChatEvent chat) {
		String message = chat.getMessage();
		String message_lower = message.toLowerCase();

		ChatColor BLUE = ChatColor.BLUE;
		ChatColor WHITE = ChatColor.WHITE;

		boolean hasTrigger=false;

		Iterator<Object> triggers = ChatFilter.langTriggers.iterator();
		while (triggers.hasNext()) {
			String trigger;
			trigger = (String) (triggers.next());
			if(message_lower.contains(trigger)) {
				hasTrigger=true;
				break;
			}
		}
		if (hasTrigger) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					ChatColor WHITE = ChatColor.WHITE;
					plugin.getServer().broadcastMessage(WHITE + ChatFilter.eleven);
				}
			}, 2L);
		}



		boolean hasSwear = false;

		Iterator<Object> iter = ChatFilter.langProfanity.iterator();
		while (iter.hasNext()) {
			String swear;
			swear = (String) (iter.next());
			if(message_lower.contains(swear)) {
				hasSwear=true;
				break;
			}
		}

		if (ChatFilter.debugMode) {
			System.out.print("[ChatFilter] Part match = " + hasSwear); 
		}

		if (!hasSwear) {
			//ChatFilter.profanityWordMatch

			String cleanedMsg = message_lower.trim();
			cleanedMsg = Normalizer.normalize(cleanedMsg, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
			cleanedMsg=cleanedMsg.replaceAll("[^a-z]"," ");
	
			if (ChatFilter.debugMode) {
				System.out.print("[ChatFilter] cleanedMsg = " + cleanedMsg); 
			}

			String[] words = cleanedMsg.split(" ");  
			Iterator<Object> iter2 = null;

			for (String word : words)  
			{  
				if (ChatFilter.debugMode) {
					System.out.print(word);
				}
				iter2 = ChatFilter.profanityWordMatch.iterator();
				while (iter2.hasNext()) {
					String swear;
					swear = (String) (iter2.next());
					if (ChatFilter.debugMode) {
						System.out.print("swear - " + swear + " | word - "+word);
					}
					if(swear.equals(word)) {
						hasSwear=true;
						break;
					}
				}
			}
		}


		if (hasSwear) {
			Player p = chat.getPlayer();
			String outMessage =ChatFilter.profanityMessage.replaceAll("%N", p.getName());
			p.sendMessage(BLUE + "[ChatFilter] " + WHITE + outMessage);
			chat.setCancelled(true);
			if (ChatFilter.kick) {
				p.kickPlayer(outMessage);
			}
			if (ChatFilter.showInConsole) {
				String consoleMsg = p.getName() + " said: " + message;
				System.out.print("[ChatFilter] " + consoleMsg);
			}
		}
	}
}

