# CrateCinema

In-world opening animations for [ExcellentCrates](https://modrinth.com/plugin/excellentcrates).
Keys, reward pools and the crate editor stay with ExcellentCrates; this plugin only adds the show.

> Status: early development, not released yet.

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
