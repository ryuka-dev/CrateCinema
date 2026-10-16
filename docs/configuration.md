# Configuration

CrateCinema's shows are ExcellentCrates animations. A crate plays a show when its ExcellentCrates
animation is set to it, and each show's settings are ExcellentCrates animation profiles. The few
settings that apply to every show are in `plugins/CrateCinema/config.yml`.

| What | Where | Apply changes with |
|---|---|---|
| Which show a crate plays | The crate's animation in ExcellentCrates | `/ecrates reload` |
| A show's settings | `plugins/ExcellentCrates/objects/animations/cratecinema_<show>/` | `/ecrates reload` |
| Tiers, audience, effects, limits, messages | `plugins/CrateCinema/config.yml` | `/cratecinema reload` |

There is no need to restart. Shows that are already playing finish with the settings they started
with. A value that is missing or invalid is replaced by its default, and the console says which one.

## Choosing a crate's show

Set the crate's animation to one of the shows, in the ExcellentCrates crate editor or in the crate's
file in `plugins/ExcellentCrates/objects/crate/`:

```yaml
animation:
  enabled: true
  profile: cratecinema_starfall:default
```

Everything else about the crate (keys, rewards, costs) stays in ExcellentCrates as before.

| Show | Profile | What happens |
|---|---|---|
| Orbit | `cratecinema_orbit:default` | Rewards from the pool circle above the crate, spin and wink out until the won reward is left, which charges and bursts. |
| Starfall | `cratecinema_starfall:default` | A burning meteor strikes the crate and the reward rises from the impact. Decoy meteors land first at higher tiers. |
| Cards | `cratecinema_cards:default` | Five cards are shuffled; one steps forward, glows and flips over to show the reward. |
| Rift | `cratecinema_rift:default` | A portal tear swallows rewards from the pool and pushes the won reward out. |
| Simple | `cratecinema_simple:default` | Text, particles and sound only. Light, and visible to Bedrock players. |

Try any show at any tier with `/cratecinema preview <show> [tier]` while looking at a block, for
example `/cratecinema preview starfall 3`. The held item stands in for the reward. Nothing is
rolled, taken or granted.

### Things that overlap a show

- **Crate particle effects.** An ExcellentCrates effect such as `helix` is drawn for every player
  around the crate and cannot be hidden for one player, so it runs through the show. On startup the
  console warns about crates that play a show and have one. Turning it off for those crates
  (`effect.enabled: false` in the crate's file, or in the crate editor) gives the cleanest result.
- **Crate holograms** are hidden from the player watching a show and come back afterwards.

### Openings that never get a show

- Sneak-clicking a crate (ExcellentCrates' quick open) skips the show on purpose.

## Batch openings

When a player opens several at once, ExcellentCrates plays the show once per reward, one after
another, and grants all the rewards after the last. If one of those shows is interrupted (the player
quits, dies, teleports away or changes world), the rest are skipped and every reward is granted at
once.

Since ExcellentCrates 7.2, choosing how many to open is only offered on a sneak-click by default,
and a sneak-click skips the show. To let players open several at once with a show, set
`trigger_in_fast_mode_only: false` in `plugins/ExcellentCrates/config/crates.batch.yml`.

## Show settings: animation profiles

Each show keeps its settings in `plugins/ExcellentCrates/objects/animations/cratecinema_<show>/`.
Every file there is a profile: `default.yml` is `cratecinema_<show>:default`. Copy it under another
name for a variant, for example a quicker `fast.yml`, run `/ecrates reload`, and set a crate to
`cratecinema_orbit:fast`. Previews take variants too: `/cratecinema preview orbit:fast`.

Values that differ by tier are lists of four, one per tier: `[tier 0, tier 1, tier 2, tier 3]`. A
single number applies to every tier. Times are in ticks (20 ticks = 1 second). Every key is
described in the file itself. The files ExcellentCrates writes put each list item on its own line;
both ways of writing a list work.

| Show | Settings |
|---|---|
| `orbit` | `radius`, `spin-ticks`, `spin-turns`, `charge-ticks`, `hold-ticks` |
| `starfall` | `meteor-block`, `fall-ticks`, `decoys`, `hold-ticks` |
| `cards` | `card-item`, `swaps`, `charge-ticks`, `flip-ticks`, `hold-ticks` |
| `rift` | `tear-block`, `tear-glow`, `charge-ticks`, `hold-ticks` |

Every profile also has `name`, which is how ExcellentCrates lists it in its editor. `simple` has no
other settings.

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

- `max-concurrent-shows`: further openings get the `simple` show until a show ends.
- `max-show-seconds`: a safety net. A show still running after this long is ended and its reward
  granted. The default shows take 4 to 13 seconds.

### Messages and debugging

```yaml
messages:
  already-watching: "&cYou are still watching a crate opening."
debug: false
```

`already-watching` is sent when a player tries to open a crate while their show (or a preview) is
still playing. That opening is refused before anything is taken. `debug: true` logs every show's
start and end to the console.

## Bedrock players

Bedrock clients (through Geyser) cannot show the item and block displays the shows are made of.
With Floodgate installed, Bedrock players always get the `simple` show instead of the crate's show.

Geyser connects to the server as the newest Java version. If the server runs an older version,
Bedrock players need ViaVersion installed to join at all; this is a Geyser requirement, not
specific to this plugin.

## Commands and permissions

| Command | Permission | |
|---|---|---|
| `/cratecinema preview <show> [tier]` | `cratecinema.admin` (op) | Play a show on the block you are looking at |
| `/cratecinema reload` | `cratecinema.admin` (op) | Reload `config.yml` |
| `/ecrates reload` | ExcellentCrates' own | Reload crates and the shows' profiles |

## Upgrading from 1.x

2.0 needs ExcellentCrates 7.2.0 or newer.

- **Bindings.** `bindings.yml` is no longer read. On startup the console lists, for each crate in
  it, the animation profile to set instead. Set them (see [Choosing a crate's show](#choosing-a-crates-show)),
  run `/ecrates reload`, then delete `bindings.yml` to stop the message. CrateCinema does not change
  ExcellentCrates' crate files itself.
- **Show settings.** The first time 2.0 starts, each show's `plugins/CrateCinema/shows/<show>.yml`
  is copied into its default profile, so your changes carry over. The old files stay but are no
  longer read; edit the profiles instead.
- **`bedrock.mode`** is gone: Bedrock players always get the `simple` show. The old setting is
  ignored.
- **Openings past `max-concurrent-shows`** get the `simple` show instead of ExcellentCrates' normal
  opening.
- **`/cratecinema reload`** only reloads `config.yml`; use `/ecrates reload` for the shows.

## Notes

- Use a full restart after installing or updating the plugin. Bukkit's `/reload` is not supported:
  ExcellentCrates' reward commands do not run during it, so a show playing at that moment loses its
  reward.
- Shows hand every reward to ExcellentCrates when they end. If a player quits, changes world,
  teleports away or the server stops, the show ends at once and the reward is granted. If they
  die, it is granted once they respawn.
- Show entities are never saved with the world, so nothing is left behind after a crash.
