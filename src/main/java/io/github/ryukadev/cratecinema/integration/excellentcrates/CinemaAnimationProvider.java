package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import io.github.ryukadev.cratecinema.config.SettingsFile;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.shows.ShowType;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.excellentcrates.api.animation.AnimationProvider;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;
import su.nightexpress.nightcore.config.FileConfig;

/**
 * Offers one of our shows to ExcellentCrates as an animation type. ExcellentCrates keeps its
 * profiles in {@code objects/animations/cratecinema_<show>/}: each file holds one set of the
 * show's settings, and a crate selects it as {@code cratecinema_<show>:<file name>}.
 *
 * <p>The default profile starts as the commented copy in the plugin jar, or, when upgrading from
 * CrateCinema 1.x, as the server owner's old {@code shows/<show>.yml}, which is left in place but
 * no longer read.
 */
final class CinemaAnimationProvider implements AnimationProvider<CinemaAnimationProfile> {

    static final String ID_PREFIX = "cratecinema_";
    private static final String NAME_PATH = "name";

    private final ShowType type;
    private final Identifier id;
    private final Plugin plugin;
    private final ShowRuntime runtime;
    private final String defaultsText;
    private final YamlConfiguration defaults;

    CinemaAnimationProvider(ShowType type, Plugin plugin, ShowRuntime runtime) {
        this.type = type;
        this.id = new Identifier(ID_PREFIX + type.id());
        this.plugin = plugin;
        this.runtime = runtime;
        this.defaultsText = readResource(plugin, type.defaultsResource());
        this.defaults = new YamlConfiguration();
        try {
            this.defaults.loadFromString(this.defaultsText);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalStateException("Invalid " + type.defaultsResource() + " in the plugin jar", exception);
        }
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    String showId() {
        return this.type.id();
    }

    @Override
    public CinemaAnimationProfile readProfile(AdaptedKey key, FileConfig file) {
        String fileName = "animation " + key.asString();
        Show show;
        try {
            show = this.type.create(new SettingsFile(fileName, file, this.defaults, this.runtime.logger()));
        } catch (RuntimeException exception) {
            // Never let one bad file stop ExcellentCrates loading the rest.
            this.runtime.logger().log(Level.SEVERE, "Could not read " + fileName + "; using the default settings.", exception);
            show = this.type.create(new SettingsFile(fileName, this.defaults, this.defaults, this.runtime.logger()));
        }
        String name = file.getString(NAME_PATH, this.defaults.getString(NAME_PATH, this.type.id()));
        return new CinemaAnimationProfile(key, name, show, this.runtime);
    }

    @Override
    public void writeDefaultProfile(AdaptedKey key, FileConfig file) {
        Path legacy = this.plugin.getDataFolder().toPath().resolve(this.type.defaultsResource());
        try {
            if (Files.isRegularFile(legacy)) {
                file.loadFromString(Files.readString(legacy, StandardCharsets.UTF_8));
                if (!file.contains(NAME_PATH)) {
                    file.set(NAME_PATH, this.defaults.getString(NAME_PATH));
                }
                this.runtime.logger().info("Copied " + this.type.defaultsResource() + " from CrateCinema 1.x into the ExcellentCrates animation "
                    + key.asString() + ". The old file is no longer read; edit the animation's file instead.");
                return;
            }
        } catch (IOException | InvalidConfigurationException exception) {
            this.runtime.logger().log(Level.WARNING, "Could not copy " + legacy + "; starting " + key.asString() + " from the defaults.", exception);
        }
        try {
            file.loadFromString(this.defaultsText);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalStateException("Invalid " + this.type.defaultsResource() + " in the plugin jar", exception);
        }
    }

    private static String readResource(Plugin plugin, String path) {
        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + path + " in the plugin jar");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path + " from the plugin jar", exception);
        }
    }
}
