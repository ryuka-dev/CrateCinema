package io.github.ryukadev.cratecinema.config;

import io.github.ryukadev.cratecinema.engine.EngineSettings;
import io.github.ryukadev.cratecinema.engine.Tiers;

/**
 * Everything read from {@code config.yml}.
 *
 * @param busyMessage sent when a player tries to open a crate while still watching a show; may be empty
 */
public record PluginSettings(boolean debug, EngineSettings engine, String busyMessage) {

    static PluginSettings read(SettingsFile config) {
        Tiers tiers = new Tiers(config.intMap("tiers", 0, Tiers.MAX), config.integer("default-tier", 0, Tiers.MAX));
        EngineSettings engine = new EngineSettings(
            tiers,
            config.choice("audience", EngineSettings.Audience.class),
            config.decimal("nearby-radius", 1, 64),
            config.decimal("effects.sound-volume", 0, 4),
            config.decimal("effects.particle-density", 0, 4),
            config.integer("limits.max-concurrent-shows", 1, 10_000),
            config.integer("limits.max-show-seconds", 5, 300) * 20);
        return new PluginSettings(
            config.bool("debug"),
            engine,
            colorize(config.text("messages.already-watching")));
    }

    /** Translates '&' colour and format codes. */
    private static String colorize(String text) {
        return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
