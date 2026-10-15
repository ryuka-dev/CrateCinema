# CrateCinema

Cinematic, in-world opening shows for [ExcellentCrates](https://modrinth.com/plugin/excellentcrates).
Keys, reward pools and the crate editor stay with ExcellentCrates; this plugin only adds the show.

<p>
  <img src="docs/media/starfall.gif" width="49%" alt="Starfall: a meteor strikes the crate">
  <img src="docs/media/cards.gif" width="49%" alt="Cards: one of five shuffled cards flips over">
</p>

## Features

- **Four shows**: `orbit` (a ring of rewards that spins and narrows down to the winner), `starfall`
  (a meteor strikes the crate), `cards` (shuffled cards, one flips over) and `rift` (a portal tear
  swallows the pool and pushes the reward out). There is also a text-only `simple` show.
- **Rarity tiers**: rarer rewards build up longer, glow in their rarity colour and end bigger.

  <img src="docs/media/starfall-tiers.gif" width="80%" alt="Starfall at all four tiers, from common to legendary">

- **Private by default**: only the player opening the crate sees their show, so a crowd can open
  the same crate at once. Showing it to nearby players is one setting.
- **Batch openings stay short**: one show for the best reward, then the whole batch at a glance.
- **Safe**: ExcellentCrates grants every reward. If a player quits, teleports away or the server
  stops mid-show, the reward is granted at once. Nothing is left in the world after a crash.
- **Bedrock aware**: with Floodgate, Bedrock players get a text-only show they can actually see.
- **Configurable**: one file per show, `/cratecinema reload`, and `/cratecinema preview` to try
  any show at any tier.

## Requirements

- Paper 1.21.9 or newer (ExcellentCrates is dropping Spigot support)
- Java 25 (required by ExcellentCrates 7.x)
- ExcellentCrates 7.1.0 or newer, with nightcore

## Getting started

1. Install ExcellentCrates and nightcore, then this plugin, and restart.
2. Bind a crate in `plugins/CrateCinema/bindings.yml`, for example `my_crate: starfall`.
3. Run `/cratecinema reload`.

Shows: `orbit`, `starfall`, `cards`, `rift` and the text-only `simple`. Preview any of them in-game
with `/cratecinema preview <show> [tier]`. Everything else is in
[docs/configuration.md](docs/configuration.md).

## Building

```
./gradlew build
```

The plugin jar is written to `build/libs/`. A Java 25 toolchain is provisioned automatically if none is installed.

`./gradlew runServer` starts a local Paper 1.21.9 test server with ExcellentCrates and nightcore downloaded from Modrinth.

Integration tests drive real openings with bots; see [it/README.md](it/README.md).

## License

GPL-3.0. See [LICENSE](LICENSE).
