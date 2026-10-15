// Stands next to the crate for a while and logs every change in the show entities it receives.
const fs = require('fs'); const h = require('./harness');
const c = JSON.parse(fs.readFileSync(h.positionFile('crate')));
const center = new h.Vec3(c.x + 0.5, c.y + 0.5, c.z + 0.5);
(async () => {
  await h.rconConnect();
  const b = await h.spawnBot('Watcher');
  await h.cmd(`tp Watcher ${c.stand.x + 2.5} ${c.stand.y} ${c.stand.z + 0.5} 0 0`);
  await h.rconClose();
  let last = -1;
  const end = Date.now() + 20 * 60 * 1000;
  while (Date.now() < end) {
    const n = h.displaysNear(b, center, 6);
    if (n !== last) { console.log(new Date().toISOString().slice(11, 19), 'Watcher receives item_display entities:', n); last = n; }
    await h.sleep(100);
  }
  await h.quitBot(b);
})().catch(e => { console.error(e); process.exit(1); });
