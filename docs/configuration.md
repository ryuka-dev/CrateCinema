# Configuration

All files are in `plugins/CrateCinema/`. After editing any of them, run `/cratecinema reload`; there
is no need to restart. Shows that are already playing finish with the settings they started with.

A value that is missing or invalid is replaced by its default, and the console says which one.

## Binding crates: `bindings.yml`

A crate plays a show only if it is listed here. Everything else about the crate (keys, rewards,
costs, the editor) stays in ExcellentCrates.

```yaml
bindings:
  vote_crate: orbit
  legendary_crate: starfall
```

The key is the ExcellentCrates crate id, which is the crate's file name in
`plugins/ExcellentCrates/objects/crate/` without `.yml`. The value is a show:

| Show | What happens |
|---|---|
| `orbit` | Rewards from the pool circle above the crate, spin and wink out until the won reward is left, which charges and bursts. |
| `starfall` | A burning meteor strikes the crate and the reward rises from the impact. Decoy meteors land first at higher tiers. |
| `cards` | Five cards are shuffled; one steps forward, glows and flips over to show the reward. |
| `rift` | A portal tear swallows rewards from the pool and pushes the won reward out. |
| `simple` | Text, particles and sound only. Light, and visible to Bedrock players. |

Try any show at any tier with `/cratecinema preview <show> [tier]` while looking at a block. The
held item stands in for the reward. Nothing is rolled, taken or granted.

On startup and reload the console warns about bindings to crates that do not exist.

### Things that overlap a show

- **Crate particle effects.** An ExcellentCrates effect such as `helix` is drawn for every player
  around the crate and cannot be hidden for one player, so it runs through the show. The console
  warns about bound crates that have one. Turning it off for those crates (`effect.enabled: false`
  in the crate's file, or in the crate editor) gives the cleanest result.
- **Crate holograms** are hidden from the player watching a show and come back afterwards.

### Openings that never get a show

- Sneak-clicking a crate (ExcellentCrates' quick open) skips the show on purpose.
- When `limits.max-concurrent-shows` is reached, further openings get the normal ExcellentCrates
  opening.
- Bedrock players get what `bedrock.mode` says (below).

## Batch openings

When a player opens several at once, the show plays once, for the best reward in the batch (highest
tier, then the rarest). It is followed by a few seconds showing every reward of the batch together.
A batch of 20 takes about as long as a single opening.

## `config.yml`

### Tiers

```yaml
tiers:
  common: 0
  uncommon: 0
  rare: 1
  epic: 2
  legendary: 3
default-tier: 0
```

Each ExcellentCrates rarity id maps to a show tier from 0 (plainest) to 3 (grandest). Higher tiers
build up longer and end with more light, particles and sound. Rewards with no rarity, or a
rarity not listed, get `default-tier`.

### Who sees a show

```yaml
audience: viewer
nearby-radius: 16
```

- `viewer`: only the player opening the crate. Several players can open the same crate at the same
  time without their shows overlapping.
- `nearby`: also every player within `nearby-radius` blocks of the crate when the show starts.

### Effects

```yaml
effects:
  sound-volume: 1.0
  particle-density: 1.0
```

Multipliers for every show's sound volume and particle counts. `0` turns them off.

### Limits

```yaml
limits:
  max-concurrent-shows: 50
  max-show-seconds: 30
```

- `max-concurrent-shows`: further openings get the normal ExcellentCrates opening until a show
  ends.
- `max-show-seconds`: a safety net. A show still running after this long is ended and its reward
  granted. The default shows take 4 to 13 seconds.

### Bedrock players

```yaml
bedrock:
  mode: simple
```

Bedrock clients (through Geyser) cannot show the item and block displays the shows are made of.
With Floodgate installed, Bedrock players get:

- `simple`: the text-only `simple` show (default);
- `crate-plugin`: the normal ExcellentCrates opening, as if the crate were not bound.

Without Floodgate this setting has no effect.

Geyser connects to the server as the newest Java version. If the server runs an older version,
Bedrock players need ViaVersion installed to join at all; this is a Geyser requirement, not
specific to this plugin.

### Messages and debugging

```yaml
messages:
  already-watching: "&cYou are still watching a crate opening."
debug: false
```

`already-watching` is sent when a player tries to open a crate while their show is still playing.
That opening is refused before anything is taken. `debug: true` logs every show's start and end to
the console.

## Show settings: `shows/<show>.yml`

Each show has its own file. Values that differ by tier are lists of four, one per tier:
`[tier 0, tier 1, tier 2, tier 3]`. A single number applies to every tier. Times are in ticks
(20 ticks = 1 second). Every key is described in the file itself.

| File | Settings |
|---|---|
| `orbit.yml` | `radius`, `spin-ticks`, `spin-turns`, `charge-ticks`, `hold-ticks` |
| `starfall.yml` | `meteor-block`, `fall-ticks`, `decoys`, `hold-ticks` |
| `cards.yml` | `card-item`, `swaps`, `charge-ticks`, `flip-ticks`, `hold-ticks` |
| `rift.yml` | `tear-block`, `tear-glow`, `charge-ticks`, `hold-ticks` |

## Commands and permissions

| Command | Permission | |
|---|---|---|
| `/cratecinema preview <show> [tier]` | `cratecinema.admin` (op) | Play a show on the block you are looking at |
| `/cratecinema reload` | `cratecinema.admin` (op) | Reload every file |

## Notes

- Use a full restart after installing or updating the plugin. Bukkit's `/reload` is not supported:
  ExcellentCrates' reward commands do not run during it, so a show playing at that moment loses its
  reward.
- Shows hand every reward to ExcellentCrates when they end. If a player quits, changes world,
  teleports away or the server stops, the show ends at once and the reward is granted. If they
  die, it is granted once they respawn.
- Show entities are never saved with the world, so nothing is left behind after a crash.
