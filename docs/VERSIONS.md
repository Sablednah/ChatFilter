# One plugin from 1.7.2 to 26.3

How the build is shaped and what was run. Written 2026-10-09.

## The shape

| Module | Compiled against | Carries |
|---|---|---|
| `core` | nothing | ChatFilter-ReForged's filtering engine, copied verbatim (Java 8) |
| `bukkit` | Bukkit **1.7.2-R0.3** (JitPack) | the plugin: settings, chat and command listeners, commands, migration |

MobHealth's Bukkit repo, which this follows, needs five `compat-*` adapters for APIs that arrived
later (action bars, boss bars, display entities). ChatFilter needs **none**:
`AsyncPlayerChatEvent` (with its mutable recipient set), `PlayerCommandPreprocessEvent`,
permissions, `FileConfiguration`, the scheduler and `ChatColor` were all in 1.7.2. Hex colours
(`&#rrggbb`) are plain `§x` strings, emitted only when the server reports 1.16+.

Two jars, because `api-version` is per jar:

| Jar | `api-version` | Servers |
|---|---|---|
| `+bukkit1.13-26.3` | 1.13 | 1.13 – 26.3 |
| `+bukkit1.7-1.12` | none | 1.7.2 – 1.12.2 |

There is no 1.7 / 1.8 split as in MobHealth, because nothing ChatFilter uses arrived in 1.8. Beta
1.7.3 was skipped on purpose.

## Measured

`apiCheck` compiles the `bukkit` sources against spigot-api 1.8.8, 1.12.2, 1.13.2, 1.16.5, 1.20.4,
1.21.11 and 26.3: **zero errors on all seven** (2026-10-09).

Then each line was run on the Vivo rig (Paper builds copied from MobHealth's rig). The self-test
has 55 checks (matching, commands, colours, a full 2.x migration). The bot runs three players and 15
checks: a clean line, mask (Watcher and staff see `####`, the sender sees the original and is told
nothing, staff get the alert), caps, link block plus message, the "eleven" reply arriving *after*
its line, `/tell` masked, shadow, and repeat.

| Server | Jar | Self-test | Bot |
|---|---|---|---|
| Paper 1.7.10 | 1.7-1.12 | 55/55 | 15/15 (raw protocol) |
| Paper 1.8.8 | 1.7-1.12 | 55/55 | 15/15 |
| Paper 1.12.2 | 1.7-1.12 | 55/55 | 15/15 |
| Paper 1.16.5 | 1.13-26.3 | 55/55 | 15/15 |
| Paper 1.20.1 | 1.13-26.3 | 55/55 | 15/15 |
| Paper 1.21.11 | 1.13-26.3 | 55/55 | 15/15 |
| Paper 26.1.2 | 1.13-26.3 | 55/55 | 15/15 |
| Paper 26.3 | 1.13-26.3 | 55/55 | — (no bot speaks 26.3) |

## Not yet done

- **Signed chat with real accounts.** The rig is offline-mode with `enforce-secure-profile=false`,
  so it cannot show how a 1.19+ server treats a *command* whose text ChatFilter rewrote when the
  client signed the original. Chat itself is fine, because Paper sends an edited line as unsigned
  content next to the signed original. Worth one check on an online-mode server before calling
  1.19+ `/msg` rewriting proven.
- Spigot (as opposed to Paper) has not been run; Paper is what the rig carries. The chat event
  contract is Bukkit's, but the delivery code differs between them.
- 26.2 not run; 26.3 has no bot.
