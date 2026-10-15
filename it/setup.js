// One-time world setup: place a chest and assign it to the test crate. Writes crate.json.
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
  const p = bot.entity.position.floored();
  const chest = p.offset(0, 0, 3);
  console.log(await h.cmd(`setblock ${chest.x} ${chest.y} ${chest.z} minecraft:air`));
  console.log(await h.cmd('clear Bot1'));
  console.log(await h.cmd('give Bot1 minecraft:chest 1'));
  await h.sleep(800);
  const chestItem = bot.inventory.items().find(i => i.name === 'chest');
  await bot.equip(chestItem, 'hand');
  await h.sleep(300);
  bot.chat(`/crate assign ${h.CRATE_ID}`);
  await h.sleep(1500);
  const held = bot.inventory.items().find(i => i.name === 'chest');
  await bot.equip(held, 'hand');
  const below = bot.blockAt(chest.offset(0, -1, 0));
  await bot.lookAt(below.position.offset(0.5, 1, 0.5), true);
  await bot.placeBlock(below, new h.Vec3(0, 1, 0));
  await h.sleep(1000);
  console.log('placed:', bot.blockAt(chest).name);
  console.log('chat:', bot.chatLog.slice(-5));
  fs.writeFileSync(__dirname + '/crate.json', JSON.stringify({ x: chest.x, y: chest.y, z: chest.z, stand: { x: p.x, y: p.y, z: p.z } }));
  console.log('crate at', chest, 'bot at', p);
  await h.quitBot(bot);
  await h.rconClose();
})().catch(e => { console.error(e); process.exit(1); });
