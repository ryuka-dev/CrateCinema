# Changelog

## 2.0.0

The shows are now ExcellentCrates animations. Requires ExcellentCrates 7.2.0 or newer.

- A crate plays a show when its ExcellentCrates animation is set to it, for example
  `cratecinema_starfall:default`, in the crate editor or the crate's file. `bindings.yml` is no
  longer used.
- Each show's settings are ExcellentCrates animation profiles in
  `plugins/ExcellentCrates/objects/animations/cratecinema_<show>/`, reloaded with `/ecrates reload`.
  A show can have several profiles, for example a quicker `cratecinema_orbit:fast`, and
  `/cratecinema preview orbit:fast` previews them.
- Batch openings play one show per reward, as ExcellentCrates' own animations do, and grant every
  reward after the last. Interrupting one of them skips the rest and grants everything at once.
- Bedrock players always get the text-only `simple` show; `bedrock.mode` is removed.
- Openings past `limits.max-concurrent-shows` get the `simple` show instead of the normal opening.
- `/cratecinema reload` reloads `config.yml` only.

### Upgrading from 1.x

- On first start, each `plugins/CrateCinema/shows/<show>.yml` is copied into the show's default
  profile, so changed settings carry over. The old files are left in place but no longer read.
- The console lists, for each crate in the old `bindings.yml`, the animation profile to set. Set
  them, run `/ecrates reload`, then delete `bindings.yml`.

See the "Upgrading from 1.x" section of `docs/configuration.md`.

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
