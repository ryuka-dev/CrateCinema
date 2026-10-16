# Integration tests

Bots (mineflayer) and RCON drive real crate openings on the local test server and check the
outcome: keys taken, rewards granted, which display entities each player receives, and the
plugin's debug log.

## Running

1. `./gradlew runServer` once, accept the Minecraft EULA in `run/eula.txt` yourself, let the
   server finish starting, then `stop` it.
2. `cd it && npm install && npm run prepare-server`. This writes `run/server.properties` (offline
   mode, port 25590, RCON on 25591, bound to 127.0.0.1), the test crate (its ExcellentCrates
   animation set to `cratecinema_cinema_test:default`), its rewards and key. It also enables debug
   logging, turns off ExcellentCrates' interaction cooldown, and makes a normal opening with several
   keys ask how many to open (ExcellentCrates 7.2 only asks on a sneak opening by default).
3. `./gradlew runServer` again and wait for `Done`.
4. `npm run setup` (again after every `prepare-server`, which resets the crate files and so
   unlinks the blocks). A bot places and links the test crate block. For visual
   checks, `node setup.js showcase 3` places the `showcase` crate (eight rewards across all five
   rarities, animation `cratecinema_orbit:default`) three blocks to the right, and
   `node setup.js csgo_compare 6` places `csgo_compare` (the same rewards with ExcellentCrates' own
   CSGO animation) for comparison. Offsets are from the test crate.
5. `npm test` runs every scenario except `bukkitReload`. Run single scenarios with
   `node tests.js quit death`.

### Other Minecraft versions

`it/check-version.sh <server version> <bot version> [ExcellentCrates jar]` runs everything against
another Minecraft version, in its own `run-<version>/` directory. With a jar, that ExcellentCrates
build replaces the Modrinth release, in `run-<version>-<jar name>/`. The bots support `1.21.9`, `1.21.10`, `1.21.11` and
`26.1` (which also speaks 26.1.2's protocol). Newer servers need a real client: joining through
ViaBackwards does not work, because it mistranslates the bots' movement packets and the server kicks
them.

## Scenarios

| Name | What it checks |
|---|---|
| normal | The key is taken at the start, the opener sees the display, the reward arrives when the show ends, and the entity is removed |
| quit | Quitting mid-show ends it; the reward is there after rejoining |
| death | Dying mid-show ends it; the reward is held until respawn, then granted |
| teleport | A short teleport is ignored; a long one ends the show and grants at once |
| world | Changing world ends the show and grants |
| concurrent | Two players open the same crate; each receives only their own display entity |
| double | A second opening while watching is refused before any key is taken |
| fastopen | A sneak (fast) opening is left to ExcellentCrates |
| ecreload | `/ecrates reload` mid-show does not break the show or the hook |
| batch | A 3× opening: ExcellentCrates plays one show per reward, one after another, and grants all three after the last |
| batchInterrupt | Interrupting a round of a batch skips the rest and grants all of its rewards at once |
| orbit | On the showcase crate: the ring spawns, the crate hologram is hidden from the viewer and comes back afterwards, and the reward arrives at the end |
| profiles | Every show is registered as an ExcellentCrates animation type and has a default profile file |
| previews | Every show at tiers 0 and 3 through `/cratecinema preview`: each finishes, logs no errors and leaves no display entities on the server |
| configReload | Switching a crate's animation to another show takes effect after `/ecrates reload`; invalid values in a profile and in `config.yml` are reported; a second profile of a show (`orbit:fast`) is picked up |
| limits | With `max-concurrent-shows: 1`, a second opening gets the simple show |
| timeout | A show that would run past `max-show-seconds` is ended by the limit and still grants |
| audience | With `audience: nearby`, a player next to the crate sees someone else's show |
| particles | With `particle-density: 0`, the viewer receives no show particles |
| bukkitReload | Bukkit `/reload` mid-show (not in the default run; see below) |

Stopping the server and a hard kill mid-show need a server restart, so they are run by hand
with `node stopcheck.js open`, then stopping or killing the server, then restarting it and
running `node stopcheck.js after`. Both take a crate position name as a second argument
(default `crate`).

For a visual check with a real client, `node watch.js` puts a bot called `Watcher` next to the
crate for 20 minutes. It logs every change in the number of show entities it receives, so it
should stay at 0 while someone else opens the crate.

## Bedrock check (manual)

Needs a Bedrock client. Put Geyser-Spigot and Floodgate (from download.geysermc.org) and
ViaVersion into `run/plugins/`, set `auth-type: floodgate` in `plugins/Geyser-Spigot/config.yml`,
and restart. ViaVersion is needed because Geyser connects as the newest Java version. Join on
`127.0.0.1:19132` and open the test crate: the text-only simple show plays, and the reward arrives
when it ends. A Java player opening the same crate gets its own show.

## Known results

- `bukkitReload` fails. Paper rebuilds its command state before disabling plugins, so the `give`
  command that ExcellentCrates dispatches for the reward does nothing. On top of that, nightcore
  and ExcellentCrates fail to re-enable. Bukkit `/reload` is unsupported; a full restart is
  required.
