#!/usr/bin/env node
// Three players and a conversation, for the ChatFilter test servers:
//
//   Swearer  - says things
//   Watcher  - an ordinary player: must see #### where Swearer swore
//   CFStaff  - an op (alerts, but no chatfilter.see): sees #### too, plus the staff alert
//
//   node chatfilter-bot.js <host> <port> <mcVersion>
//
// mineflayer speaks 1.8 to 26.1; 1.7.10 goes over node-minecraft-protocol directly. Prints every
// check, then one summary line ("bot: N/N passed" or the failures) for test.sh to collect.
'use strict';

const [host, portArg, version] = process.argv.slice(2);
if (!host || !portArg || !version) {
  console.error('usage: chatfilter-bot.js <host> <port> <mcVersion>');
  process.exit(2);
}
const port = Number(portArg);
const raw = version.startsWith('1.7');
const strip = (s) => String(s).replace(/§[0-9a-fk-orx]/gi, '');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function flatten(c) {
  if (c == null) return '';
  if (typeof c === 'string') return c;
  let out = c.text || '';
  if (c.translate) out += (c.with || []).map(flatten).join(' ');
  if (Array.isArray(c.extra)) out += c.extra.map(flatten).join('');
  return out;
}

function player(name) {
  const p = { name, lines: [], ready: null, say: null, quit: null };
  if (raw) {
    const mc = require('minecraft-protocol');
    const client = mc.createClient({ host, port, username: name, version: '1.7.10' });
    p.ready = new Promise((res, rej) => { client.once('login', res); client.once('error', rej); });
    client.on('chat', (pkt) => {
      let text;
      try { text = flatten(JSON.parse(pkt.message)); } catch (_) { text = pkt.message; }
      p.lines.push(strip(text));
    });
    p.say = (t) => client.write('chat', { message: t });
    p.quit = () => client.end();
  } else {
    const mineflayer = require('mineflayer');
    const bot = mineflayer.createBot({ host, port, version, username: name, auth: 'offline', hideErrors: true });
    p.ready = new Promise((res, rej) => { bot.once('spawn', res); bot.once('kicked', (r) => rej(new Error('kicked: ' + r))); bot.once('error', rej); });
    bot.on('message', (msg, position) => {
      if (position === 'game_info') return;
      // What a vanilla client SHOWS: the unsigned content when the server sent one. mineflayer
      // hands over the signed original and tucks the server's edit away in msg.unsigned, so a
      // lowercased shout looked untouched until this was read (Paper 1.19+, found 2026-10-09).
      p.lines.push(strip((msg.unsigned || msg).toString()));
    });
    p.say = (t) => bot.chat(t);
    p.quit = () => bot.quit();
  }
  return p;
}

const results = [];
const check = (name, ok, detail) => results.push({ name, ok: !!ok, detail });
// Lines a player received at or after a mark, so each step is judged on its own replies.
const since = (p, mark) => p.lines.slice(mark);
const has = (lines, s) => lines.some((l) => l.includes(s));

(async () => {
  const S = player('Swearer');
  const W = player('Watcher');
  const T = player('CFStaff');
  try {
    await Promise.all([S.ready, W.ready, T.ready]);
  } catch (e) {
    console.log('bot: could not join (' + (e.message || e) + ')');
    process.exit(0);
  }
  await sleep(2500);

  // Each step: Swearer says something, everyone listens for a moment.
  const step = async (text, wait = 1800) => {
    const marks = { S: S.lines.length, W: W.lines.length, T: T.lines.length };
    S.say(text);
    await sleep(wait);
    return { S: since(S, marks.S), W: since(W, marks.W), T: since(T, marks.T) };
  };

  let r = await step('hello there');
  check('a clean line reaches the Watcher untouched', has(r.W, 'hello there'));

  // A repeat is the same as your PREVIOUS message, so this has to come straight after it.
  r = await step('hello there');
  check('saying it twice is a repeat', has(r.S, 'said that already') && !has(r.W, 'hello there'), { S: r.S, W: r.W });

  r = await step('what the fuck');
  check('the Watcher sees the word masked', has(r.W, 'what the ####') && !has(r.W, 'fuck'), r.W);
  check('the Swearer sees what they typed', has(r.S, 'what the fuck'), r.S);
  check('the Swearer is told nothing', !has(r.S, 'Mind your language'), r.S);
  check('staff see it masked as well', has(r.T, 'what the ####') && !r.T.some((l) => l.includes('what the fuck') && !l.includes('[CF]')), r.T);
  check('staff get the alert', r.T.some((l) => l.includes('[CF]') && l.includes('Swearer')), r.T);

  r = await step('THIS IS VERY LOUD INDEED');
  check('shouting arrives lowercased', has(r.W, 'this is very loud indeed'), r.W);

  r = await step('join example.com today');
  check('a link is stopped', !has(r.W, 'example.com'), r.W);
  check('and the Swearer is told why', has(r.S, 'Links need a word'), r.S);

  r = await step('it goes up to eleven', 2500);
  const lineAt = r.W.findIndex((l) => l.includes('up to eleven'));
  const replyAt = r.W.findIndex((l) => l.includes("not even funny"));
  check('eleven gets its reply', replyAt >= 0, r.W);
  check('and the reply comes after the line', lineAt >= 0 && replyAt > lineAt, r.W);

  // /tell, not /msg: /msg is an unknown command on the 1.8.8 test server even with no plugins
  // touching it, while /tell exists on every line from 1.7 to 26.x.
  r = await step('/tell Watcher you shit');
  check('a /tell is masked for its recipient', has(r.W, 'you ####') && !has(r.W, 'shit'), { S: r.S, W: r.W });

  r = await step('banana split please');
  check('a shadowed line reaches nobody else', !has(r.W, 'banana'), r.W);
  check('but the Swearer sees it sent', has(r.S, 'banana split please'), r.S);

  for (const p of [S, W, T]) { try { p.quit(); } catch (_) { /* gone */ } }

  for (const c of results) {
    console.log((c.ok ? '  ok  ' : '  FAIL ') + c.name + (c.ok ? '' : '  ' + JSON.stringify(c.detail)));
  }
  const failed = results.filter((c) => !c.ok);
  console.log(failed.length ? `bot: ${results.length - failed.length}/${results.length}, FAILED: ${failed.map((c) => c.name).join('; ')}`
    : `bot: ${results.length}/${results.length} passed`);
  setTimeout(() => process.exit(0), 300);
})();

setTimeout(() => { console.log('bot: timed out'); process.exit(0); }, 100000);
