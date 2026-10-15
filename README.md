# CrateCinema

In-world opening animations for [ExcellentCrates](https://modrinth.com/plugin/excellentcrates).
Keys, reward pools and the crate editor stay with ExcellentCrates; this plugin only adds the show.

> Status: early development, not released yet.

## Requirements

- Paper 1.21.9 or newer (ExcellentCrates is dropping Spigot support)
- Java 25 (required by ExcellentCrates 7.x)
- ExcellentCrates 7.1.0 or newer, with nightcore

## Building

```
./gradlew build
```

The plugin jar is written to `build/libs/`. A Java 25 toolchain is provisioned automatically if none is installed.

`./gradlew runServer` starts a local Paper 1.21.9 test server with ExcellentCrates and nightcore downloaded from Modrinth.

Integration tests drive real openings with bots; see [it/README.md](it/README.md).

## Notes for server owners

- Restart the server after installing or updating the plugin. Bukkit `/reload` is not supported: ExcellentCrates' reward commands do not run during it, so a show that is running at that moment loses its reward. ExcellentCrates and nightcore do not survive it either.
- `/ecrates reload` is fine.

## License

GPL-3.0. See [LICENSE](LICENSE).
