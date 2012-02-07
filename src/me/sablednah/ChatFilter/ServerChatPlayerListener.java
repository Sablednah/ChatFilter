package me.sablednah.ChatFilter;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;


public class ServerChatPlayerListener implements Listener  {
	public ChatFilter plugin;

	public ServerChatPlayerListener(ChatFilter instance) {
		this.plugin=instance;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(PlayerChatEvent chat) {
		String message = chat.getMessage();
		String message_lower = message.toLowerCase();

		ChatColor BLUE = ChatColor.BLUE;
		ChatColor WHITE = ChatColor.WHITE;

		Iterator<Object> triggers = ChatFilter.langTriggers.iterator();
		while (triggers.hasNext()) {
			String trigger;
			trigger = (String) (triggers.next());
			if(message_lower.contains(trigger)) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						ChatColor WHITE = ChatColor.WHITE;
						plugin.getServer().broadcastMessage(WHITE + ChatFilter.eleven);
					}
				}, 2L);
				break;
			}

			Iterator<Object> iter = ChatFilter.langProfanity.iterator();
			while (iter.hasNext()) {
				String swear;
				swear = (String) (iter.next());
				if(message_lower.contains(swear)) {
					Player p = chat.getPlayer();
					String outMessage;
					outMessage=ChatFilter.profanityMessage.replaceAll("%N", p.getName());

					p.sendMessage(BLUE + "[ChatFilter] " + WHITE + outMessage);
					chat.setCancelled(true);
					break;
				}               
			}
		}
	}
}
