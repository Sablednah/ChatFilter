# ChatFilter

A chat filter for Bukkit, Spigot and Paper servers, **1.7.2 to 26.3**. It censors or stops swearing
in chat and in `/msg`-style commands, sees through the usual disguises, blocks links and spam, and
needs nothing on the client.

Version 3 is a rebuild of the 2012 plugin, sharing its filtering engine with the NeoForge mod
[ChatFilter ReForged](https://github.com/Sablednah/ChatFilter-ReForged). The 2.x plugin is on the
`master` branch.

## What it does

- **Masks swearing silently.** Other players see a caught word as `####`; the sender sees what they
  typed and is told nothing. Staff with `chatfilter.see` read the original.
- **Or replaces, shadows, blocks or just logs, per category.** `REPLACE` rewrites the word (the old
  `!@$#`), `SHADOW` is a soft-mute (the sender sees their line sent, nobody else sees it), `BLOCK`
  stops it, with *"Oi Steve! Mind your language!"* if you want it.
- **Sees through disguises:** leetspeak (`sh1t`, `$h!t`), stretched letters (`fuuuck`), spelled-out
  letters (`f.u.c.k`, `s h i t`), self-censoring (`f*ck`), accents, look-alike letters from other
  alphabets, zero-width characters and colour codes hidden mid-word.
- **Leaves innocent words alone:** whole-word lists (`ass` catches neither *class* nor *grass*) and
  an allowlist for the Scunthorpe problem.
- **Filters chat that arrives as a command:** `/msg`, `/tell`, `/w`, `/r`, `/me`, `/say`,
  `/mail send`, `/f chat` and any you add. `msg *` skips the name, so `/msg Assassin hi` works.
- **Links and IP addresses** (with an allowlist for your own site), **shouting**, **repeats** and
  **floods**.
- **Strikes** that expire one at a time, and a ladder that can alert staff, warn, or run any command.
- **Staff alerts**, the console log, `/chatfilter test`, and in-game list editing.
- **Keyword replies.** It still answers "eleven", and you can switch that off for good.

## Upgrading from 2.x

Drop the new jar in place of the old one. On first start your `config.yml` and `lang.yml` are
carried over into the new `config.yml` and kept as `config-2.x.yml.bak` / `lang-2.x.yml.bak`. Your
old behaviour is kept: if you blocked swearing, it still blocks. Set `profanity.action: MASK` for
the silent mode. The old permission nodes (`chatfilter.canswear`, `canchat`, `reload`) still work.

## Which jar

| Server | Jar |
|---|---|
| 1.13 – 26.3 (Spigot, Paper) | `chatfilter-3.0.0+bukkit1.13-26.3.jar` |
| 1.7.2 – 1.12.2 | `chatfilter-3.0.0+bukkit1.7-1.12.jar` |

## Commands and permissions

| Command | Permission (default) | Does |
|---|---|---|
| `/chatfilter reload` | `chatfilter.admin` (op) | re-read config.yml, report mistakes |
| `/chatfilter test <text>` | `chatfilter.admin` (op) | what would be caught, by what, and the result |
| `/chatfilter add\|remove <list> <entry>` | `chatfilter.admin` (op) | edit a list; saved to config.yml |
| `/chatfilter list <list>` | `chatfilter.admin` (op) | show a list |
| `/chatfilter strikes <player> [clear]` | `chatfilter.admin` (op) | see or clear strikes |
| `/chatfilter alerts [on\|off\|toggle]` | `chatfilter.alerts` (op) | your own staff alerts |

| Permission | Default | Means |
|---|---|---|
| `chatfilter.chat` | everyone | may chat at all |
| `chatfilter.bypass` | nobody | not filtered, ops included |
| `chatfilter.see` | nobody | sees masked words as written |
| `chatfilter.links` | nobody | may post links |

## Licence

MIT.
