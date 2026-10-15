// mode=open: open a crate and stop/crash is driven externally. mode=after: rejoin and report.
const fs = require('fs'); const h = require('./harness');
const c = JSON.parse(fs.readFileSync(__dirname + '/crate.json'));
const v = new h.Vec3(c.x, c.y, c.z);
(async () => {
  await h.rconConnect();
  const mode = process.argv[2];
  const b = await h.spawnBot('Bot1');
  if (mode === 'open') {
    await h.cmd('clear Bot1');
    await h.cmd(`tp Bot1 ${c.stand.x + 0.5} ${c.stand.y} ${c.stand.z + 0.5} 0 0`);
    await h.cmd(`cratekey give Bot1 ${h.KEY_ID} 1`);
    await h.sleep(800);
    await b.lookAt(v.offset(0.5, 0.5, 0.5), true);
    await b.activateBlock(b.blockAt(v));
    await h.sleep(1000);
    console.log(`OPENED keys=${h.keys(b)} rewards=${h.rewards(b)} displays=${h.displaysNear(b, v.offset(0.5, 0.5, 0.5))}`);
    console.log('READY_FOR_STOP');
    await h.sleep(60000); // stay connected; the server goes down under us
  } else {
    await h.sleep(1500);
    console.log(`AFTER keys=${h.keys(b)} rewards=${h.rewards(b)}`);
    console.log('item_display in loaded world:', await h.cmd('execute if entity @e[type=minecraft:item_display]'));
    await h.quitBot(b);
  }
  await h.rconClose();
})().catch(e => { console.log('bot ended:', e.message); process.exit(0); });
