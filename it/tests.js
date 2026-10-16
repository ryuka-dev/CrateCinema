// Integration tests. Usage: node tests.js [testName ...]  (no args = all except bukkitReload)
const fs = require('fs');
const path = require('path');
const h = require('./harness');

const crate = JSON.parse(fs.readFileSync(h.positionFile('crate'), 'utf8'));
const crateVec = new h.Vec3(crate.x, crate.y, crate.z);
const crateCenter = crateVec.offset(0.5, 0.5, 0.5);
const SHOW_MS = 3600; // TestShow: 72 ticks

const bots = {};
async function bot(name) {
  if (!bots[name] || !bots[name].entity) bots[name] = await h.spawnBot(name);
  return bots[name];
}

async function prep(name, keyCount = 1, offsetX = 0) {
  await h.cmd(`clear ${name}`);
  await h.cmd(`tp ${name} ${crate.stand.x + 0.5 + offsetX} ${crate.stand.y} ${crate.stand.z + 0.5} 0 0`);
  if (keyCount > 0) await h.cmd(`cratekey give ${name} ${h.KEY_ID} ${keyCount}`);
  await h.sleep(800);
}

async function open(b) {
  await b.lookAt(crateCenter, true);
  await b.activateBlock(b.blockAt(crateVec));
}

const SHOWCASE_ITEMS = ['dragon_egg', 'netherite_ingot', 'diamond', 'emerald', 'gold_ingot', 'iron_ingot', 'golden_apple', 'bread'];
/** Showcase rewards in the inventory. */
function count(b) {
  return SHOWCASE_ITEMS.reduce((sum, name) => sum + h.count(b, name), 0);
}

async function openBatchOfThree(b) {
  await open(b);
  await h.waitFor(() => !!b.currentWindow, 3000);
  await b.clickWindow(4, 0, 0);
}

/** The native_test crate placed by "node setup.js native_test -3", or null (and a note) if it is not. */
function nativeCrateOrSkip() {
  const file = h.positionFile('native_test');
  if (!fs.existsSync(file)) { console.log('skipped: run "node setup.js native_test -3" first'); return null; }
  const position = JSON.parse(fs.readFileSync(file, 'utf8'));
  const block = new h.Vec3(position.x, position.y, position.z);
  return { block, center: block.offset(0.5, 0.5, 0.5), stand: position.stand };
}

async function prepAt(name, target) {
  await h.cmd(`clear ${name}`);
  await h.cmd(`tp ${name} ${target.stand.x + 0.5} ${target.stand.y} ${target.stand.z + 0.5} 0 0`);
  await h.cmd(`cratekey give ${name} ${h.KEY_ID} 1`);
  await h.sleep(800);
}

async function openAt(b, target) {
  await b.lookAt(target.center, true);
  await b.activateBlock(b.blockAt(target.block));
}

const started = (log, name) => log.includes(`Started show 'cinema_test' for ${name}`);
const ended = (log, name, reason) => new RegExp(`Ended show 'cinema_test' for [0-9a-f-]+: ${reason}`).test(log);


// Bukkit /reload is unsupported (see README); not in the default run.
const MANUAL = ['bukkitReload'];

const PLUGIN_DIR = path.join(h.RUN_DIR, 'plugins', 'CrateCinema');

/**
 * Runs body with edited CrateCinema config files, then restores them and reloads.
 * edits: { 'config.yml': [[regex, replacement], ...], 'shows/orbit.yml': [...] }
 */
async function withConfig(edits, body) {
  const originals = {};
  try {
    for (const [file, changes] of Object.entries(edits)) {
      const full = path.join(PLUGIN_DIR, file);
      originals[full] = fs.readFileSync(full, 'utf8');
      let text = originals[full];
      for (const [pattern, replacement] of changes) {
        if (!pattern.test(text)) throw new Error(`pattern ${pattern} not found in ${file}`);
        text = text.replace(pattern, replacement);
      }
      fs.writeFileSync(full, text);
    }
    const reply = await h.cmd('cratecinema reload');
    if (!reply.includes('reloaded')) throw new Error('reload failed: ' + reply);
    await body();
  } finally {
    for (const [full, text] of Object.entries(originals)) fs.writeFileSync(full, text);
    await h.cmd('cratecinema reload');
  }
}

const tests = {
  async normal() {
    const b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    await open(b);
    h.check('normal: show starts', await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000));
    h.check('normal: key taken at start', await h.waitFor(() => h.keys(b) === 0, 1500), `keys=${h.keys(b)}`);
    h.check('normal: viewer sees the display entity', await h.waitFor(() => h.displaysNear(b, crateCenter) === 1, 1500), `displays=${h.displaysNear(b, crateCenter)}`);
    h.check('normal: no reward before the show ends', h.rewards(b) === 0, `rewards=${h.rewards(b)}`);
    h.check('normal: reward granted when show ends', await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000), `rewards=${h.rewards(b)}`);
    h.check('normal: ended as FINISHED', ended(h.logSince(mark), 'Bot1', 'FINISHED'));
    h.check('normal: display entity removed', await h.waitFor(() => h.displaysNear(b, crateCenter) === 0, 1500));
  },

  async quit() {
    let b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(1000);
    await h.quitBot(b); bots.Bot1 = null;
    await h.sleep(1000);
    h.check('quit: ended as QUIT', ended(h.logSince(mark), 'Bot1', 'QUIT'));
    b = await bot('Bot1');
    await h.sleep(1500);
    h.check('quit: reward kept after rejoin', h.rewards(b) === 1, `rewards=${h.rewards(b)}`);
    h.check('quit: key still consumed (one key, one reward)', h.keys(b) === 0, `keys=${h.keys(b)}`);
    const radius = h.displaysNear(b, crateCenter);
    h.check('quit: no display left at the crate', radius === 0, `displays=${radius}`);
  },

  async death() {
    const b = await bot('Bot1'); await prep('Bot1');
    await h.cmd('spawnpoint Bot1 200 -60 200');
    const mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(800);
    await h.cmd('kill Bot1');
    h.check('death: ended as DEATH', await h.waitFor(() => ended(h.logSince(mark), 'Bot1', 'DEATH'), 2000));
    h.check('death: reward held, then granted after respawn',
      await h.waitFor(() => h.logSince(mark).includes('Granting held reward for Bot1 after respawn'), 8000));
    h.check('death: reward in the new inventory', await h.waitFor(() => h.rewards(b) === 1, 3000), `rewards=${h.rewards(b)}`);
    await h.cmd(`spawnpoint Bot1 ${crate.stand.x} ${crate.stand.y} ${crate.stand.z}`);
  },

  async teleport() {
    const b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(500);
    await h.cmd('execute as Bot1 at Bot1 run tp @s ~1 ~ ~');
    await h.sleep(500);
    h.check('teleport: a short teleport does not end the show', !/Ended show/.test(h.logSince(mark)));
    await h.cmd('execute as Bot1 at Bot1 run tp @s ~60 ~ ~');
    h.check('teleport: a long teleport ends it as TELEPORT', await h.waitFor(() => ended(h.logSince(mark), 'Bot1', 'TELEPORT'), 1500));
    h.check('teleport: reward granted immediately', await h.waitFor(() => h.rewards(b) === 1, 1500), `rewards=${h.rewards(b)}`);
  },

  async world() {
    const b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(500);
    await h.cmd('execute in minecraft:the_nether run tp Bot1 0 100 0');
    h.check('world: changing world ends the show', await h.waitFor(() => /Ended show 'cinema_test' for [0-9a-f-]+: (TELEPORT|WORLD_CHANGE)/.test(h.logSince(mark)), 2000));
    h.check('world: reward granted', await h.waitFor(() => h.rewards(b) === 1, 3000), `rewards=${h.rewards(b)}`);
    await h.cmd(`execute in minecraft:overworld run tp Bot1 ${crate.stand.x + 0.5} ${crate.stand.y} ${crate.stand.z + 0.5}`);
    await h.sleep(1500);
  },

  async concurrent() {
    const b1 = await bot('Bot1'); const b2 = await bot('Bot2');
    await prep('Bot1', 1, -1); await prep('Bot2', 1, 1);
    const mark = h.logMark();
    await Promise.all([open(b1), open(b2)]);
    h.check('concurrent: both shows start', await h.waitFor(() => started(h.logSince(mark), 'Bot1') && started(h.logSince(mark), 'Bot2'), 2000));
    await h.sleep(800);
    const d1 = h.displaysNear(b1, crateCenter), d2 = h.displaysNear(b2, crateCenter);
    h.check('concurrent: each player sees only their own display', d1 === 1 && d2 === 1, `Bot1 sees ${d1}, Bot2 sees ${d2}`);
    h.check('concurrent: both rewarded', await h.waitFor(() => h.rewards(b1) === 1 && h.rewards(b2) === 1, SHOW_MS + 2000),
      `Bot1=${h.rewards(b1)} Bot2=${h.rewards(b2)}`);
  },

  async double() {
    const b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.cmd(`cratekey give Bot1 ${h.KEY_ID} 1`);
    await h.sleep(600);
    const chatBefore = b.chatLog.length;
    await open(b);
    await h.sleep(600);
    h.check('double: second opening refused while watching', b.chatLog.slice(chatBefore).some(m => m.includes('still watching')),
      JSON.stringify(b.chatLog.slice(chatBefore)));
    h.check('double: refused opening takes no key', h.keys(b) === 1, `keys=${h.keys(b)}`);
    h.check('double: only one reward', await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000) && h.keys(b) === 1,
      `rewards=${h.rewards(b)} keys=${h.keys(b)}`);
  },

  async fastopen() {
    const b = await bot('Bot1'); await prep('Bot1');
    const mark = h.logMark();
    b.setControlState('sneak', true);
    await h.sleep(300);
    await open(b);
    await h.sleep(300);
    b.setControlState('sneak', false);
    await h.sleep(1500);
    h.check('fastopen: no show for a sneak (fast) opening', !started(h.logSince(mark), 'Bot1'));
    h.check('fastopen: ExcellentCrates grants directly', h.rewards(b) === 1 && h.keys(b) === 0, `rewards=${h.rewards(b)} keys=${h.keys(b)}`);
  },

  async ecreload() {
    const b = await bot('Bot1'); await prep('Bot1');
    let mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(800);
    await h.cmd('ecrates reload');
    h.check('ecreload: show survives an ExcellentCrates reload', await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000)
      && ended(h.logSince(mark), 'Bot1', 'FINISHED'), `rewards=${h.rewards(b)}`);
    await prep('Bot1');
    mark = h.logMark();
    await open(b);
    h.check('ecreload: still hooked after the reload', await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000));
    await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000);
  },

  // Several keys make ExcellentCrates ask how many to open; slot 4 of its menu is "3".
  async batch() {
    const b = await bot('Bot1'); await prep('Bot1', 3);
    const mark = h.logMark();
    await openBatchOfThree(b);
    h.check('batch: one session with 3 rounds', await h.waitFor(() => h.logSince(mark).includes('for Bot1 (3 reward(s))'), 3000));
    h.check('batch: 3 keys taken up front', await h.waitFor(() => h.keys(b) === 0, 2000), `keys=${h.keys(b)}`);
    await h.sleep(5000);
    h.check('batch: no reward mid-batch (granted together at the end)', h.rewards(b) === 0, `rewards=${h.rewards(b)}`);
    h.check('batch: all 3 rewards after the last round', await h.waitFor(() => h.rewards(b) === 3, 9000), `rewards=${h.rewards(b)}`);
    h.check('batch: ended once as FINISHED', (h.logSince(mark).match(/Ended show/g) || []).length === 1);
  },

  async batchInterrupt() {
    const b = await bot('Bot1'); await prep('Bot1', 3);
    const mark = h.logMark();
    await openBatchOfThree(b);
    await h.waitFor(() => h.logSince(mark).includes('for Bot1 (3 reward(s))'), 3000);
    await h.sleep(4500); // inside round 2
    await h.cmd('execute as Bot1 at Bot1 run tp @s ~60 ~ ~');
    h.check('batchInterrupt: ended as TELEPORT', await h.waitFor(() => ended(h.logSince(mark), 'Bot1', 'TELEPORT'), 1500));
    h.check('batchInterrupt: all 3 rewards granted at once', await h.waitFor(() => h.rewards(b) === 3, 2000), `rewards=${h.rewards(b)}`);
  },

  // The orbit show on the showcase crate (node setup.js showcase 3). Skipped if that crate is not set up.
  async orbit() {
    const file = h.positionFile('showcase');
    if (!fs.existsSync(file)) { console.log('skipped: run "node setup.js showcase 3" first'); return; }
    const showcase = JSON.parse(fs.readFileSync(file, 'utf8'));
    const block = new h.Vec3(showcase.x, showcase.y, showcase.z);
    const center = block.offset(0.5, 0.5, 0.5);
    const b = await bot('Bot1');
    await h.cmd(`clear Bot1`);
    await h.cmd(`tp Bot1 ${showcase.stand.x + 0.5} ${showcase.stand.y} ${showcase.stand.z + 0.5} 0 0`);
    await h.cmd(`cratekey give Bot1 ${h.KEY_ID} 1`);
    await h.sleep(1500);
    // ExcellentCrates' hologram: text displays straight above this crate (needs packetevents).
    const hologramIds = () => Object.values(b.entities)
      .filter(e => e.name === 'text_display' && Math.abs(e.position.x - center.x) < 0.6 && Math.abs(e.position.z - center.z) < 0.6)
      .map(e => e.id);
    const hologram = hologramIds();
    const hologramShown = () => hologram.some(id => b.entities[id]);
    const mark = h.logMark();
    await b.lookAt(center, true);
    await b.activateBlock(b.blockAt(block));
    h.check('orbit: show starts', await h.waitFor(() => h.logSince(mark).includes("Started show 'orbit' for Bot1"), 2000));
    await h.sleep(1500);
    const ring = h.displaysNear(b, center, 4);
    h.check('orbit: ring of 6-8 rewards', ring >= 6 && ring <= 8, `item_display=${ring}`);
    h.check('orbit: crate hologram hidden from the viewer', hologram.length > 0 && !hologramShown(),
      `hologram entities=${hologram.length}, still shown=${hologramShown()}`);
    h.check('orbit: no reward during the show', h.rewards(b) === 0 && count(b) === 0);
    h.check('orbit: reward granted when it ends', await h.waitFor(() => count(b) === 1, 20000), `items=${count(b)}`);
    h.check('orbit: ended as FINISHED', /Ended show 'orbit' for [0-9a-f-]+: FINISHED/.test(h.logSince(mark)));
    h.check('orbit: everything removed', await h.waitFor(() => h.displaysNear(b, center, 4) === 0, 1500));
    h.check('orbit: hologram back afterwards', await h.waitFor(() => hologramIds().length === hologram.length, 3000),
      `hologram entities=${hologramIds().length}`);
  },

  // The test show as an ExcellentCrates animation type: the native_test crate selects
  // cratecinema_cinema_test:default in its own animation settings and is not in bindings.yml
  // (node setup.js native_test -3). Skipped if that crate is not set up.
  async native() {
    const nativeCrate = nativeCrateOrSkip();
    if (!nativeCrate) return;
    const whole = h.logSince(0);
    h.check('native: show registered as an animation type', whole.includes('Registered 1 show(s) as ExcellentCrates animation types'));
    h.check('native: ExcellentCrates wrote its profile file',
      fs.existsSync(path.join(h.RUN_DIR, 'plugins', 'ExcellentCrates', 'objects', 'animations', 'cratecinema_cinema_test', 'default.yml')));
    h.check('native: no missing-profile warning', !/No animation profile found|No instantiator found/.test(whole));

    const b = await bot('Bot1'); await prepAt('Bot1', nativeCrate);
    const mark = h.logMark();
    await openAt(b, nativeCrate);
    h.check('native: show starts', await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000));
    h.check('native: key taken at start', await h.waitFor(() => h.keys(b) === 0, 1500), `keys=${h.keys(b)}`);
    h.check('native: viewer sees the display entity', await h.waitFor(() => h.displaysNear(b, nativeCrate.center) === 1, 1500));
    h.check('native: no reward before the show ends', h.rewards(b) === 0, `rewards=${h.rewards(b)}`);
    h.check('native: reward granted when show ends', await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000), `rewards=${h.rewards(b)}`);
    h.check('native: ended as FINISHED', ended(h.logSince(mark), 'Bot1', 'FINISHED'));
    h.check('native: display entity removed', await h.waitFor(() => h.displaysNear(b, nativeCrate.center) === 0, 1500));
    await h.sleep(1000);
    h.check('native: exactly one reward', h.rewards(b) === 1, `rewards=${h.rewards(b)}`);
  },

  // Quitting mid-show: ExcellentCrates stops the session and our own listener ends the show; the
  // opening must complete exactly once.
  async nativeQuit() {
    const nativeCrate = nativeCrateOrSkip();
    if (!nativeCrate) return;
    let b = await bot('Bot1'); await prepAt('Bot1', nativeCrate);
    const mark = h.logMark();
    await openAt(b, nativeCrate);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(1000);
    await h.quitBot(b); bots.Bot1 = null;
    await h.sleep(1000);
    h.check('nativeQuit: show ended', /Ended show 'cinema_test' for [0-9a-f-]+: (QUIT|STOPPED)/.test(h.logSince(mark)));
    b = await bot('Bot1');
    await h.sleep(1500);
    h.check('nativeQuit: exactly one reward after rejoin', h.rewards(b) === 1, `rewards=${h.rewards(b)}`);
    h.check('nativeQuit: no display left', h.displaysNear(b, nativeCrate.center) === 0);
    // A new opening must not be refused as "still watching".
    await h.cmd(`cratekey give Bot1 ${h.KEY_ID} 1`);
    await h.sleep(800);
    const again = h.logMark();
    await openAt(b, nativeCrate);
    h.check('nativeQuit: can open again afterwards', await h.waitFor(() => started(h.logSince(again), 'Bot1'), 2000));
    await h.waitFor(() => h.rewards(b) === 2, SHOW_MS + 2000);
  },

  // Every show through /cratecinema preview at the lowest and highest tier: it must finish, leave
  // nothing behind and log no errors. Uses the showcase crate block as the target.
  async previews() {
    const file = h.positionFile('showcase');
    if (!fs.existsSync(file)) { console.log('skipped: run "node setup.js showcase 3" first'); return; }
    const showcase = JSON.parse(fs.readFileSync(file, 'utf8'));
    const center = new h.Vec3(showcase.x + 0.5, showcase.y + 0.5, showcase.z + 0.5);
    const b = await bot('Bot1');
    await h.cmd('op Bot1');
    await h.cmd('clear Bot1');
    await h.cmd(`tp Bot1 ${showcase.stand.x + 0.5} ${showcase.stand.y} ${showcase.stand.z + 0.5} 0 0`);
    await h.sleep(1500);
    await b.lookAt(center, true);
    const shown = () => Object.values(b.entities)
      .filter(e => ['item_display', 'block_display', 'text_display'].includes(e.name) && e.position.distanceTo(center) <= 12).length;
    const baseline = shown(); // crate holograms seen so far; they are packet-only, so the server never counts them
    const serverClean = async () => {
      for (const type of ['item_display', 'block_display', 'text_display']) {
        if (!(await h.cmd(`execute if entity @e[type=minecraft:${type}]`)).includes('failed')) return false;
      }
      return true;
    };
    for (const show of ['orbit', 'starfall', 'cards', 'rift']) {
      for (const tier of [0, 3]) {
        const mark = h.logMark();
        b.chat(`/cratecinema preview ${show} ${tier}`);
        const started = await h.waitFor(() => h.logSince(mark).includes(`Started show '${show}' for Bot1`), 2000);
        await h.sleep(1500);
        const during = shown();
        const finished = await h.waitFor(() => new RegExp(`Ended show '${show}' for [0-9a-f-]+: FINISHED`).test(h.logSince(mark)), 25000);
        await h.sleep(500);
        const clean = await serverClean();
        const errors = /ERROR|Exception/.test(h.logSince(mark));
        h.check(`previews: ${show} tier ${tier}`, started && during > baseline && finished && clean && !errors && h.rewards(b) === 0,
          `started=${started} entities=${during - baseline} finished=${finished} clean=${clean} errors=${errors}`);
      }
    }
  },


  async configReload() {
    const b = await bot('Bot1');
    await withConfig({ 'bindings.yml': [[/cinema_test_crate: cinema_test/, 'cinema_test_crate: simple']] }, async () => {
      await prep('Bot1');
      const mark = h.logMark();
      await open(b);
      h.check('configReload: new binding applies without a restart', await h.waitFor(() => h.logSince(mark).includes("Started show 'simple' for Bot1"), 2000));
      h.check('configReload: simple show grants', await h.waitFor(() => h.rewards(b) === 1, 10000), `rewards=${h.rewards(b)}`);
    });
    // An invalid value: warned about, default used, reload still applied.
    const mark = h.logMark();
    await withConfig({ 'shows/orbit.yml': [[/spin-ticks: .*/, 'spin-ticks: lots']] }, async () => {
      h.check('configReload: invalid value is reported', /orbit\.yml: 'spin-ticks' should be/.test(h.logSince(mark)));
    });
  },

  async limits() {
    const b1 = await bot('Bot1'); const b2 = await bot('Bot2');
    await withConfig({ 'config.yml': [[/max-concurrent-shows: \d+/, 'max-concurrent-shows: 1']] }, async () => {
      await prep('Bot1', 1, -1); await prep('Bot2', 1, 1);
      const mark = h.logMark();
      await open(b1);
      await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
      await open(b2);
      await h.sleep(1000);
      h.check('limits: second opening gets no show at the limit', !started(h.logSince(mark), 'Bot2'));
      h.check('limits: and is granted by ExcellentCrates at once', h.rewards(b2) === 1 && h.keys(b2) === 0, `rewards=${h.rewards(b2)} keys=${h.keys(b2)}`);
      h.check('limits: the first show still completes', await h.waitFor(() => h.rewards(b1) === 1, SHOW_MS + 2000));
    });
  },

  async timeout() {
    const file = h.positionFile('showcase');
    if (!fs.existsSync(file)) { console.log('skipped: run "node setup.js showcase 3" first'); return; }
    const showcase = JSON.parse(fs.readFileSync(file, 'utf8'));
    const block = new h.Vec3(showcase.x, showcase.y, showcase.z);
    const b = await bot('Bot1');
    await withConfig({
      'config.yml': [[/max-show-seconds: \d+/, 'max-show-seconds: 5']],
      'shows/orbit.yml': [[/hold-ticks: .*/, 'hold-ticks: 200']],
    }, async () => {
      await h.cmd('clear Bot1');
      await h.cmd(`tp Bot1 ${showcase.stand.x + 0.5} ${showcase.stand.y} ${showcase.stand.z + 0.5} 0 0`);
      await h.cmd(`cratekey give Bot1 ${h.KEY_ID} 1`);
      await h.sleep(1200);
      const mark = h.logMark();
      await b.lookAt(block.offset(0.5, 0.5, 0.5), true);
      await b.activateBlock(b.blockAt(block));
      const t0 = Date.now();
      const timedOut = await h.waitFor(() => /Ended show 'orbit' for [0-9a-f-]+: TIMEOUT/.test(h.logSince(mark)), 9000);
      h.check('timeout: an over-long show is ended by the limit', timedOut, `after ${((Date.now() - t0) / 1000).toFixed(1)}s`);
      h.check('timeout: and its reward is granted', await h.waitFor(() => count(b) === 1, 2000), `items=${count(b)}`);
    });
  },

  async audience() {
    const b1 = await bot('Bot1'); const b2 = await bot('Bot2');
    await withConfig({ 'config.yml': [[/audience: viewer/, 'audience: nearby']] }, async () => {
      await prep('Bot1', 1, -1); await prep('Bot2', 0, 1);
      const mark = h.logMark();
      await open(b1);
      await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
      await h.sleep(800);
      h.check('audience: a nearby player sees the show in nearby mode', h.displaysNear(b2, crateCenter) === 1, `Bot2 sees ${h.displaysNear(b2, crateCenter)}`);
      await h.waitFor(() => h.rewards(b1) === 1, SHOW_MS + 2000);
    });
  },

  async particles() {
    const b = await bot('Bot1');
    const countParticles = async (seconds) => {
      // Only the types orbit uses; the crates' own ExcellentCrates effect sends flame particles.
      const ours = ['end_rod', 'dust', 'firework', 'totem_of_undying'];
      let n = 0; const on = (particle) => { if (ours.includes(particle.name)) n++; };
      b.on('particle', on);
      await h.cmd(`execute as Bot1 at Bot1 run tp @s ${crate.stand.x + 0.5} ${crate.stand.y} ${crate.stand.z + 0.5} 0 0`);
      b.chat('/cratecinema preview orbit 3');
      await h.sleep(seconds * 1000);
      b.removeListener('particle', on);
      return n;
    };
    await h.cmd('op Bot1');
    await prep('Bot1', 0);
    await b.lookAt(crateCenter, true);
    const normal = await countParticles(14);
    let none = -1;
    await withConfig({ 'config.yml': [[/particle-density: [\d.]+/, 'particle-density: 0']] }, async () => {
      none = await countParticles(14);
    });
    h.check('particles: density 0 sends no show particles', normal > 0 && none === 0, `normal=${normal} with density 0=${none}`);
  },

  // Bukkit /reload: disables and re-enables every plugin. Not in the default run.
  async bukkitReload() {
    const b = await bot('Bot1'); await prep('Bot1');
    let mark = h.logMark();
    await open(b);
    await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000);
    await h.sleep(800);
    console.log(await h.cmd('bukkit:reload confirm'));
    h.check('reload: running show ended as SHUTDOWN', await h.waitFor(() => ended(h.logSince(mark), 'Bot1', 'SHUTDOWN'), 5000));
    h.check('reload: reward granted', await h.waitFor(() => h.rewards(b) === 1, 5000), `rewards=${h.rewards(b)}`);
    h.check('reload: hooked again after re-enable', await h.waitFor(() => (h.logSince(mark).match(/Hooked into the ExcellentCrates/g) || []).length >= 1, 15000));
    await h.sleep(2000);
    await prep('Bot1');
    mark = h.logMark();
    await open(b);
    h.check('reload: shows play after re-enable', await h.waitFor(() => started(h.logSince(mark), 'Bot1'), 2000));
    await h.waitFor(() => h.rewards(b) === 1, SHOW_MS + 2000);
  },
};

(async () => {
  await h.rconConnect();
  const wanted = process.argv.slice(2);
  const order = wanted.length ? wanted : Object.keys(tests).filter(t => !MANUAL.includes(t));
  for (const name of order) {
    console.log(`\n== ${name}`);
    try { await tests[name](); } catch (e) { h.check(`${name}: ran without error`, false, e.stack); }
  }
  const ok = h.summary();
  for (const b of Object.values(bots)) if (b && b.entity) await h.quitBot(b);
  await h.rconClose();
  process.exit(ok ? 0 : 1);
})();
