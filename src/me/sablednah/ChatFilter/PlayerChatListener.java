package me.sablednah.ChatFilter;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(PlayerChatEvent chat) {
		
		if (chat.isCancelled()) { return; } // no need to do anything.
		
		Player p = chat.getPlayer();
		ChatColor BLUE = ChatColor.BLUE;
		ChatColor WHITE = ChatColor.WHITE;
		

		
		String message = chat.getMessage();
		String message_lower = message.toLowerCase();

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
					String broadcast = ChatFilter.eleven;
					for (int chatcntr = 0;chatcntr<16;chatcntr++){
						broadcast=broadcast.replaceAll("&"+Integer.toHexString(chatcntr),(ChatColor.getByChar(Integer.toHexString(chatcntr)))+"");
					}
					plugin.getServer().broadcastMessage(WHITE + broadcast);
				}
			}, 2L);
		}

		if (p.hasPermission("chatfilter.blockchat")) {   // player cant chat.
			String blockMessage = ChatFilter.blockMessage.replaceAll("%N", p.getName());
			for (int chatcntr = 0;chatcntr<16;chatcntr++){
				blockMessage=blockMessage.replaceAll("&"+Integer.toHexString(chatcntr),(ChatColor.getByChar(Integer.toHexString(chatcntr)))+"");
			}
			p.sendMessage(BLUE + "[ChatFilter] " + WHITE + blockMessage);
			chat.setCancelled(true);
			
			return;
		} 		

		if (p.hasPermission("chatfilter.canswear")) { return; } // player is allowed to be naughty

		boolean hasSwear = false;

		String cleanedMsg = message_lower.trim();
		cleanedMsg = Normalizer.normalize(cleanedMsg, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		
		if (ChatFilter.agressiveMatching) {
			cleanedMsg=cleanedMsg.replaceAll("3","e");
			cleanedMsg=cleanedMsg.replaceAll("0","o");
			cleanedMsg=cleanedMsg.replaceAll("4","a");
			cleanedMsg=cleanedMsg.replaceAll("7","t");
			cleanedMsg=cleanedMsg.replaceAll("1","l");
			cleanedMsg=cleanedMsg.replaceAll("@","a");
		}
		
		cleanedMsg=cleanedMsg.replaceAll("[^a-z]"," ");
		
		Iterator<Object> iter = ChatFilter.langProfanity.iterator();
		while (iter.hasNext()) {
			String swear;
			swear = (String) (iter.next());
			if(cleanedMsg.contains(swear)) {
				hasSwear=true;
				break;
			}
		}

		if (ChatFilter.debugMode) {
			System.out.print("[ChatFilter] Part match = " + hasSwear); 
		}

		if (!hasSwear) {
			//ChatFilter.profanityWordMatch
	
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
			
			String outMessage = ChatFilter.profanityMessage.replaceAll("%N", p.getName());
			for (int chatcntr = 0;chatcntr<16;chatcntr++){
				outMessage=outMessage.replaceAll("&"+Integer.toHexString(chatcntr),(ChatColor.getByChar(Integer.toHexString(chatcntr)))+"");
			}
			p.sendMessage(BLUE + "[ChatFilter] " + WHITE + outMessage);
			if (ChatFilter.censor) {
				String messageToSend = chat.getMessage();
				iter = ChatFilter.langProfanity.iterator();
				while (iter.hasNext()) {
					String swear;
					swear = (String) (iter.next());
					if(message_lower.contains(swear)) {
						messageToSend=messageToSend.replaceAll(("(?i)"+swear), Matcher.quoteReplacement(ChatFilter.censorText));
					}
				}
				
				
				String[] outwords = messageToSend.split(" ");
				for (int i = 0; i < outwords.length; i++) {
				//for (String wordToCheck : outwords) {
					
					iter = ChatFilter.profanityWordMatch.iterator();
					while (iter.hasNext()) {
						String swearWord;
						swearWord = (String) (iter.next());
						String testWord;
						testWord = outwords[i].toLowerCase();
						testWord = Normalizer.normalize(testWord, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
						testWord = testWord.replaceAll("[^a-z]"," ").trim();

						
						System.out.print("[ChatFilter] testWord: " + testWord);
						System.out.print("[ChatFilter] swearWord: " + swearWord);

						
						if(swearWord.equals(testWord)) {
							System.out.print("[ChatFilter] testWord: " + testWord + " = swearWord: " + swearWord);
							outwords[i] = ChatFilter.censorText;
						}
					}
				}
				
				messageToSend=StringUtils.join(outwords," ");
				System.out.print("[ChatFilter] cencored: " + messageToSend);
				
				chat.setMessage(messageToSend);
				
			} else {
				chat.setCancelled(true);
			}
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

