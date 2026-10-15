# CrateCinema

In-world opening animations for [ExcellentCrates](https://modrinth.com/plugin/excellentcrates).
Keys, reward pools and the crate editor stay with ExcellentCrates; this plugin only adds the show.

> Status: early development, not released yet.

## Requirements

- Paper or Spigot 1.21.8 or newer
- Java 25 (required by ExcellentCrates 7.x)
- ExcellentCrates 7.1.0 or newer, with nightcore

## Building

```
./gradlew build
```

The plugin jar is written to `build/libs/`. A Java 25 toolchain is provisioned automatically if none is installed.

`./gradlew runServer` starts a local Paper 1.21.8 test server with ExcellentCrates and nightcore downloaded from Modrinth.

## License

GPL-3.0. See [LICENSE](LICENSE).
