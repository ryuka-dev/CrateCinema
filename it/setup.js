// One-time world setup: place a chest and assign it to a crate.
// Usage: node setup.js [crateId] [offsetX]. Stores the position in the run directory (see positionFile).
const fs = require('fs');
const h = require('./harness');

(async () => {
  await h.rconConnect();
  await h.cmd('gamerule doDaylightCycle false');
  await h.cmd('time set day');
  const bot = await h.spawnBot('Bot1');
  await h.sleep(1000);
  await h.cmd('op Bot1');
  await h.cmd('gamemode survival Bot1');
  const crateId = process.argv[2] || h.CRATE_ID;
  const dx = Number(process.argv[3] || 0);
  // Offsets are from the main crate's standing spot, not from wherever the bot logged out (which
  // is the previous crate's spot); otherwise a negative offset lands on an earlier crate.
  const mainFile = h.positionFile('crate');
  const origin = crateId !== h.CRATE_ID && fs.existsSync(mainFile)
    ? (({ stand }) => new h.Vec3(stand.x, stand.y, stand.z))(JSON.parse(fs.readFileSync(mainFile, 'utf8')))
    : bot.entity.position.floored();
  const p = origin.offset(dx, 0, 0);
  const chest = p.offset(0, 0, 3);
  // Stand in front of the new crate so it is within reach.
  await h.cmd(`tp Bot1 ${p.x + 0.5} ${p.y} ${p.z + 0.5} 0 0`);
  await h.sleep(800);
  console.log(await h.cmd(`setblock ${chest.x} ${chest.y} ${chest.z} minecraft:air`));
  console.log(await h.cmd('clear Bot1'));
  console.log(await h.cmd('give Bot1 minecraft:chest 1'));
  await h.waitFor(() => bot.inventory.items().some(i => i.name === 'chest'), 10000);
  const chestItem = bot.inventory.items().find(i => i.name === 'chest');
  await bot.equip(chestItem, 'hand');
  await h.sleep(300);
  bot.chat(`/crate assign ${crateId}`);
  await h.sleep(1500);
  await h.waitFor(() => bot.inventory.items().some(i => i.name === 'chest'), 10000);
  const held = bot.inventory.items().find(i => i.name === 'chest');
  await bot.equip(held, 'hand');
  const below = bot.blockAt(chest.offset(0, -1, 0));
  await bot.lookAt(below.position.offset(0.5, 1, 0.5), true);
  await bot.placeBlock(below, new h.Vec3(0, 1, 0));
  await h.sleep(1000);
  console.log('placed:', bot.blockAt(chest).name);
  console.log('chat:', bot.chatLog.slice(-5));
  const out = h.positionFile(crateId === h.CRATE_ID ? 'crate' : crateId);
  fs.writeFileSync(out, JSON.stringify({ x: chest.x, y: chest.y, z: chest.z, stand: { x: p.x, y: p.y, z: p.z } }));
  console.log('crate at', chest, 'bot at', p);
  await h.quitBot(bot);
  await h.rconClose();
})().catch(e => { console.error(e); process.exit(1); });
