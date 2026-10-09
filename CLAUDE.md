# CLAUDE.md

Working notes. `README.md` says what the plugin does; `docs/VERSIONS.md` says how the build is
shaped and what was run. This file is only the conventions those two do not state.

## One branch, two jars

Shaped like MobHealth's Bukkit repo (`../MobHealth`, branch `main`): **no branch per Minecraft
version**. `main` builds both jars; `master` is the 2.0.0 plugin, frozen.

- **`bukkit` compiles against Bukkit 1.7.2-R0.3** and needs no `compat-*` adapters: chat and
  command events, permissions, YAML and the scheduler were all there in 1.7.2. If a feature ever
  needs a newer API, add an adapter the MobHealth way rather than raising the floor.
- `./gradlew apiCheck` recompiles the plugin against seven later Spigot API lines. Run it before
  pushing.
- **Never call `Bukkit.getOnlinePlayers()`** (array before 1.8, Collection after): use `Online.players()`.
- **Never load YAML with `loadConfiguration(Reader)`** (newer than 1.7.2) or `(InputStream)` (gone
  later): `loadFromString` works everywhere.
- **Java 8 bytecode**, built with **JDK 25** (`JAVA_HOME=../MobHealth-Forge/tools/jdk25`): the 26.x
  API class files need it for `apiCheck`.

## core/ is not ours

`core/` is ChatFilter-ReForged's `com.sablednah.chatfilter.core`, copied **verbatim**. Change it
there (where `./gradlew coreJava8` keeps it Java 8 clean), then `scripts/sync-core.sh`.
`scripts/sync-core.sh --check` before a release.

## How chat works here (it differs from NeoForge)

There is no vanilla filter mask on Bukkit. `ChatListener` gets the same result from the event's
recipient set: at HIGHEST, viewers who should not see the word are removed; at MONITOR, if nothing
cancelled the event, they are sent a `####` copy in the final chat format. SHADOW keeps only the
sender as a recipient. Commands (`CommandListener`) have no per-viewer copy, so a masked word there
is rewritten.

## Testing

`tools/vivo/` is the rig in `~/mc/chatfilter-bukkit/` on Vivo (`192.168.7.246`): one folder per
line, ports **25621-25628** (claimed in Vivo's `~/dev/README.md`), server jars copied from
MobHealth's rig. `test.sh [versions]` starts each line with `-Dchatfilter.selftest=true`, reads the
self-test, runs `tools/bot/chatfilter-bot.js` (three players: Swearer, Watcher, CFStaff op), and
stops it. `tools/bot/test-config.yml` is the shipped config plus a SHADOW rule for "banana";
regenerate it when `config.yml` changes.

Things that cost time:

- **mineflayer reports the SIGNED text, not what a client shows.** On 1.19+ Paper an edited line
  arrives as the signed original plus unsigned content; the vanilla client shows the latter. The bot
  reads `msg.unsigned || msg`. Without that, lowercasing looked broken on 1.20+.
- **`/msg` is an unknown command on the 1.8.8 test server**, plugins or not. The bot uses `/tell`.
- A repeat is "the same as your previous message", so the bot's repeat check follows the first line
  directly.
- Nothing speaks 26.3 yet (mineflayer and minecraft-data stop at 26.1): 26.3 is self-test and
  apiCheck only.

## Publishing

A published GitHub release fans its jars out to the legacy dev.bukkit.org project via
`.github/workflows/curseforge.yml` (copied from MobHealth). The `+suffix` in each jar name is
load-bearing: `scripts/curseforge-upload.sh` maps `+bukkit1.13-26.3` and `+bukkit1.7-1.12` to
CurseForge's Bukkit version names. Needs `CURSEFORGE_PROJECT_ID` (variable) and `CURSEFORGE_TOKEN`
(secret) on Sablednah/ChatFilter; never handle the token. Release workflows run from the **default**
branch, so `main` must be the default before the first release. On ChatFilter-ReForged the first
release event did not trigger the workflow at all; check `gh run list` and dispatch it by hand if so.
