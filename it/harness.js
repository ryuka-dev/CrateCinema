// Shared helpers for CrateCinema integration tests against the local test server.
const fs = require('fs');
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const { Vec3 } = require('vec3');

const path = require('path');
// CC_RUN: the server's run directory (default run); CC_MC: the bot's Minecraft version (default 1.21.9).
const RUN_DIR = path.join(__dirname, '..', process.env.CC_RUN || 'run');
const LOG = path.join(RUN_DIR, 'logs', 'latest.log');
const VERSION = process.env.CC_MC || '1.21.9';
const CRATE_ID = 'cinema_test_crate';
const KEY_ID = 'cinema_key';
const REWARD_ITEMS = ['diamond', 'emerald', 'gold_ingot'];
const KEY_ITEM = 'tripwire_hook';

const sleep = ms => new Promise(r => setTimeout(r, ms));

let rcon;
async function rconConnect() {
  rcon = await Rcon.connect({ host: '127.0.0.1', port: 25591, password: 'cratecinema-local' });
}
async function cmd(c) {
  const out = await rcon.send(c);
  return out.replace(/\u00a7./g, '').trim();
}
async function rconClose() { if (rcon) await rcon.end(); rcon = null; }

function logMark() { return fs.statSync(LOG).size; }
function logSince(mark) {
  const buf = fs.readFileSync(LOG);
  return buf.subarray(mark).toString('utf8');
}

function spawnBot(name) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25590, username: name, version: VERSION, auth: 'offline' });
    bot.chatLog = [];
    bot.on('messagestr', m => bot.chatLog.push(m));
    const timer = setTimeout(() => reject(new Error(`${name} spawn timeout`)), 30000);
    bot.once('spawn', () => { clearTimeout(timer); resolve(bot); });
    bot.once('error', e => { clearTimeout(timer); reject(e); });
    bot.once('kicked', r => { clearTimeout(timer); reject(new Error(`${name} kicked: ${JSON.stringify(r)}`)); });
  });
}
function quitBot(bot) {
  return new Promise(resolve => { bot.once('end', () => resolve()); bot.quit(); });
}

function count(bot, name) {
  return bot.inventory.items().filter(i => i.name === name).reduce((s, i) => s + i.count, 0);
}
function rewards(bot) { return REWARD_ITEMS.reduce((s, n) => s + count(bot, n), 0); }
function keys(bot) { return count(bot, KEY_ITEM); }

/** item_display entities this bot's client knows about within `radius` of `pos`. */
function displaysNear(bot, pos, radius = 4) {
  return Object.values(bot.entities).filter(e => e.name === 'item_display' && e.position.distanceTo(pos) <= radius).length;
}

async function waitFor(pred, timeoutMs, stepMs = 100) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    if (await pred()) return true;
    await sleep(stepMs);
  }
  return await pred();
}

/** Where a placed crate's position is stored; positions belong to one world, so they live in its run directory. */
function positionFile(name) { return path.join(RUN_DIR, `cc-${name}.json`); }

const results = [];
function check(name, ok, detail = '') {
  results.push({ name, ok });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  -- ' + detail : ''}`);
}
function summary() {
  const failed = results.filter(r => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  return failed.length === 0;
}

module.exports = {
  sleep, rconConnect, cmd, rconClose, logMark, logSince, spawnBot, quitBot,
  count, rewards, keys, displaysNear, waitFor, check, summary, Vec3,
  CRATE_ID, KEY_ID, REWARD_ITEMS, KEY_ITEM, RUN_DIR, positionFile,
};
