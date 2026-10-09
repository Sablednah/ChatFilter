package com.sablednah.chatfilter.bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

import com.sablednah.chatfilter.core.Action;
import com.sablednah.chatfilter.core.Outcome;

/**
 * {@code -Dchatfilter.selftest=true} on the server's command line runs this at enable and logs a
 * pass/fail block, so every server line on the test rig says for itself that the jar works there.
 *
 * <p>The same two rules as the NeoForge self-test: call the real code, and test both directions,
 * so each disguise that must be caught sits beside an innocent word that must not be. The players'
 * side (what a second client actually sees) is the bot's job, not this.</p>
 */
public final class SelfTest {

    private final ChatFilterPlugin plugin;
    private final Logger log;
    private int passed;
    private final List<String> failed = new ArrayList<String>();

    public SelfTest(ChatFilterPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void run() {
        log.info("=== ChatFilter self-test on " + plugin.getServer().getBukkitVersion() + " ===");
        try {
            Settings s = plugin.settings();
            check("the shipped config compiles without complaint", s.problems.isEmpty());
            check("profanity masks by default", s.judge.policy("profanity").action() == Action.MASK);
            checkMatching(s);
            checkCommands(s);
            checkText();
            checkMigration();
        } catch (RuntimeException e) {
            failed.add("threw: " + e);
        }
        if (failed.isEmpty()) {
            log.info("=== ChatFilter self-test PASSED (" + passed + " checks) ===");
        } else {
            log.severe("=== ChatFilter self-test FAILED: " + failed.size() + " of " + (passed + failed.size()) + " ===");
            for (String f : failed) log.severe("  x " + f);
        }
    }

    private void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed.add(name);
        }
    }

    private static boolean caught(Settings s, String text, String category) {
        for (com.sablednah.chatfilter.core.Hit h : s.analyzer.analyze(text)) {
            if (h.category().equals(category)) return true;
        }
        return false;
    }

    private void checkMatching(Settings s) {
        String[] dirty = {"what the fuck", "FUCK", "fuuuuck", "f.u.c.k", "s h i t", "sh1t", "$h!t", "f*ck",
                "shït", "f​uck", "f&cuck", "motherfucker", "you ass", "asses", "@ss", "`fuck"};
        for (String d : dirty) check("catches " + d, caught(s, d, "profanity"));
        String[] clean = {"hello there", "this hit the spot", "class", "grass is green", "assassin",
                "Scunthorpe United", "shiitake", "pass the salt", "Mississippi", "classic"};
        for (String c : clean) check("leaves alone: " + c, !caught(s, c, "profanity"));
        Outcome o = s.judge.judge("what the fuck", s.analyzer.analyze("what the fuck"));
        check("masks exactly the word", o.kind() == Outcome.Kind.MASK && "what the ####".equals(o.censoredText("what the fuck")));
        check("catches a link", caught(s, "join example.com", "links"));
        check("allows minecraft.net", !caught(s, "see minecraft.net", "links"));
        check("leaves a version number alone", !caught(s, "updated to 1.21.11", "links"));
        Outcome shout = s.judge.judge("THIS IS VERY LOUD", s.analyzer.analyze("THIS IS VERY LOUD"));
        check("shouting is lowercased", shout.kind() == Outcome.Kind.REWRITE && "this is very loud".equals(shout.text()));
        check("the eleven response is shipped", s.responses.size() == 1);
    }

    private void checkCommands(Settings s) {
        CommandMatcher.Match m = CommandMatcher.match("msg Assassin you shit", s.commands);
        check("/msg skips the name", m != null && "you shit".equals("msg Assassin you shit".substring(m.messageStart)));
        m = CommandMatcher.match("essentials:msg Bob hi", s.commands);
        check("a namespaced /msg still matches", m != null && "hi".equals("essentials:msg Bob hi".substring(m.messageStart)));
        m = CommandMatcher.match("r well fuck", s.commands);
        check("/r has no name to skip", m != null && "well fuck".equals("r well fuck".substring(m.messageStart)));
        m = CommandMatcher.match("f chat hello all", s.commands);
        check("a two-word entry matches", m != null && "hello all".equals("f chat hello all".substring(m.messageStart)));
        check("'f chatter' is not 'f chat'", CommandMatcher.match("f chatter hello", s.commands) == null);
        check("/msg with only a name has no message", CommandMatcher.match("msg Bob", s.commands) == null);
        check("an unlisted command is left alone", CommandMatcher.match("give Bob diamond", s.commands) == null);
    }

    private void checkText() {
        check("& codes become colours", Text.colour("&cred").equals("§cred"));
        String alert = Text.fillLiteral("&7x &f{m}", "m", "&cnot a colour");
        check("player text in a template stays literal", alert.contains("&cnot a colour"));
        check("%N still names the player", Text.fill("Oi %N!", java.util.Collections.singletonMap("player", "Bob")).equals("Oi Bob!"));
    }

    /** A real 2.0.0 config and lang, migrated in a scratch folder. */
    private void checkMigration() {
        File dir = new File(plugin.getDataFolder(), "selftest-migration");
        try {
            deleteTree(dir);
            dir.mkdirs();
            write(new File(dir, "config.yml"), "debugMode: false\nkick: true\nshowInConsole: false\ncensor: true\naggressiveMatching: true\n");
            write(new File(dir, "lang.yml"), "profanity:\n- shit\n- arse\nprofanityWordMatch:\n- poo\n"
                    + "profanityMessage: \"Oi %N!\"\nblockMessage: \"No.\"\ncensorText: \"***\"\n"
                    + "triggers:\n- eleven\n- '11'\ntriggerPhrase: \"Ridiculous.\"\ncommands:\n- r\n- msg\n- f chat\n");
            check("a 2.x config is recognised", Migration.isLegacy(dir));
            Migration.migrate(dir, plugin.getResource("config.yml"), log);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(dir, "config.yml"));
            check("migration: word lists", Settings.list(y, "profanity.anywhere").contains("arse")
                    && Settings.list(y, "profanity.words").contains("poo"));
            check("migration: censor: true becomes REPLACE", "REPLACE".equals(y.getString("profanity.action")));
            check("migration: censorText", "***".equals(y.getString("censor.text")));
            check("migration: kick becomes a ladder step", Settings.list(y, "strikes.ladder").contains("1 = command kick {player} Mind your language"));
            check("migration: showInConsole", !y.getBoolean("alerts.console"));
            check("migration: triggers", Settings.list(y, "responses.rules").contains("eleven|11 = Ridiculous."));
            check("migration: msg now skips the name", Settings.list(y, "commands.list").contains("msg *")
                    && Settings.list(y, "commands.list").contains("f chat"));
            check("migration: originals kept", new File(dir, "config-2.x.yml.bak").exists() && new File(dir, "lang-2.x.yml.bak").exists());
            check("migration: the new config is not legacy", !Migration.isLegacy(dir));
            check("migration: the result compiles cleanly", Settings.load(y).problems.isEmpty());
        } catch (IOException e) {
            failed.add("migration threw: " + e);
        } finally {
            deleteTree(dir);
        }
    }

    private static void write(File f, String text) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }
}
