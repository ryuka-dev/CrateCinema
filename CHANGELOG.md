# Changelog

## 1.0.0

First release.

- Four in-world shows for ExcellentCrates openings: `orbit`, `starfall`, `cards` and `rift`, plus
  the text-only `simple`.
- Shows scale with the reward's rarity across four tiers: longer build-up, rarity-coloured glow,
  more particles and a bigger fanfare for rarer rewards.
- Only the player opening the crate sees their show by default, so several players can open the
  same crate at once. Showing it to nearby players is a setting.
- Batch openings play one show for the best reward, then show every reward of the batch
  together.
- The crate's hologram is hidden from the viewer during the show.
- Rewards are always granted by ExcellentCrates when the show ends. Quitting, changing world,
  teleporting away or a server stop end the show at once and grant the reward; after a death it is
  granted on respawn.
- Bedrock players (through Floodgate) get the text-only show, or the normal ExcellentCrates
  opening.
- Every show has its own settings file. `/cratecinema reload` applies changes without a restart,
  and `/cratecinema preview <show> [tier]` plays any show in-game.
- Limits on concurrent shows and on show length.

Requires Paper 1.21.9 or newer, Java 25, and ExcellentCrates 7.1.0 or newer.
