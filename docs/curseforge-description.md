# ChatFilter

**A chat filter for Bukkit, Spigot and Paper, 1.7.2 to 26.3.** It censors or stops swearing in chat and in chat-like
commands, sees through the usual disguises, blocks links and spam, and needs nothing installed on
the client. Unmodified clients get all of it.

Version 3 is a rebuild of this plugin, sharing its filtering engine with the NeoForge mod
ChatFilter ReForged. Most of what was asked for in these comments over the years is in it.
**Upgrading from 2.x:** swap the jar. Your config and word lists are carried over on first start,
the originals are kept as backups, and your old block or censor behaviour stays as it was.

---

## Silent by default

A caught word reaches other players as `####`. **The sender sees exactly what they typed and is
told nothing**, so there is nothing to argue with and nothing to work around. Staff with
`chatfilter.see` read the original.

Prefer the classic approach? Every category can instead:

- **REPLACE** the word for everyone (`!@$#`, `****` or cartoon swearing)
- **SHADOW** the whole line: a soft-mute where the sender sees it sent and nobody else sees it
- **BLOCK** it, with *"Oi {player}! Mind your language!"* if you like
- or just **LOG** it

## Hard to get around

Capitals, accents, leetspeak (`sh1t`, `$h!t`), stretched letters (`fuuuck`), spelled-out letters
(`f.u.c.k`, `s h i t`), self-censoring (`f*ck`), full-width and look-alike letters from other
alphabets, zero-width characters, and colour codes hidden mid-word. List the plain word once.

## Kind to innocent words

Part-word and whole-word lists, so `ass` catches neither *class* nor *grass*. There's an allowlist
for the Scunthorpe problem, and regular expressions for anything the lists can't say.
`/chatfilter test <text>` shows exactly what would be caught, by which entry, and what would happen.

## Chat that arrives as a command

`/msg`, `/tell`, `/w`, `/r`, `/me`, `/say`, `/mail send`, `/f chat`, and anything else you list.
`msg *` skips the name: **only the message is judged, never the player name**, so `/msg Assassin hi`
works.

## And the rest

- **Links and IP addresses**, including `example dot com`, with an allowlist for your own site and a
  permission for those who may post them
- **Spam**: shouting is lowercased, and repeats and floods are stopped
- **Strikes that expire one at a time**, and a ladder that can alert staff, warn the player, or run
  any command: `mute {player} 10m`, `kick {player}`
- **Staff alerts** with a per-person toggle, and a console log
- **Edit the lists in game**: `/chatfilter add profanity.words …`, saved to the config file
- **Keyword replies.** It still answers "eleven", and you can switch that off for good.

## Permissions

Plain Bukkit permissions, so any permissions plugin can grant them. The 2.x names (`chatfilter.canswear`, `canchat`, `reload`) still work.

| Node | Default | Means |
|---|---|---|
| `chatfilter.chat` | everyone | may chat at all |
| `chatfilter.bypass` | nobody | not filtered (ops included) |
| `chatfilter.see` | nobody | reads masked words as written |
| `chatfilter.links` | nobody | may post links |
| `chatfilter.alerts` | ops | receives staff alerts |
| `chatfilter.admin` | ops | uses `/chatfilter` |

## Configuration

`plugins/ChatFilter/config.yml`, every option commented. The defaults mask the classic word list
silently, block links and floods, quieten shouting, and filter the usual private-message commands.
The `severe` list ships empty: every server's is different, and nobody should get one they didn't
choose.

## Versions

Two files: **1.13 – 26.3** (`+bukkit1.13-26.3`) and **1.7.2 – 1.12.2** (`+bukkit1.7-1.12`). Tested
on Paper 1.7.10, 1.8.8, 1.12.2, 1.16.5, 1.20.1, 1.21.11, 26.1.2 and 26.3.
Open source (MIT): [GitHub](https://github.com/Sablednah/ChatFilter).
