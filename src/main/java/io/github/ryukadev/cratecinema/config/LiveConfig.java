package io.github.ryukadev.cratecinema.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;
import io.github.ryukadev.cratecinema.shows.CardsShow;
import io.github.ryukadev.cratecinema.shows.OrbitShow;
import io.github.ryukadev.cratecinema.shows.RiftShow;
import io.github.ryukadev.cratecinema.shows.SimpleShow;
import io.github.ryukadev.cratecinema.shows.StarfallShow;
import io.github.ryukadev.cratecinema.shows.TestShow;

/**
 * Owns the plugin's current configuration: settings, the shows built from their settings files,
 * and the bindings. Loading builds a complete new snapshot and swaps it in only if everything
 * loaded, so a broken file never leaves the plugin half-configured. Shows already running keep
 * what they started with.
 *
 * <p>Main thread only.
 */
public final class LiveConfig {

    private static final String BINDINGS_FILE = "bindings.yml";

    private record Snapshot(PluginSettings settings, ShowCatalog catalog, Bindings bindings) {
    }

    private final JavaPlugin plugin;
    private final Logger logger;
    private @Nullable Snapshot current;

    public LiveConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Reads every file again. On failure the previous configuration stays in place.
     *
     * @return whether the new configuration was applied
     */
    public boolean load() {
        try {
            this.plugin.saveDefaultConfig();
            this.plugin.reloadConfig();
            PluginSettings settings = PluginSettings.read(
                new SettingsFile("config.yml", this.plugin.getConfig(), this.plugin.getConfig().getDefaults(), this.logger));

            ShowCatalog catalog = new ShowCatalog();
            catalog.register(new TestShow());
            catalog.register(new SimpleShow());
            catalog.register(this.show(OrbitShow.ID, config -> new OrbitShow(OrbitShow.Settings.read(config))));
            catalog.register(this.show(StarfallShow.ID, config -> new StarfallShow(StarfallShow.Settings.read(config))));
            catalog.register(this.show(CardsShow.ID, config -> new CardsShow(CardsShow.Settings.read(config))));
            catalog.register(this.show(RiftShow.ID, config -> new RiftShow(RiftShow.Settings.read(config))));

            File bindingsFile = new File(this.plugin.getDataFolder(), BINDINGS_FILE);
            if (!bindingsFile.exists()) {
                this.plugin.saveResource(BINDINGS_FILE, false);
            }
            Bindings bindings = Bindings.load(bindingsFile, catalog, this.logger);

            this.current = new Snapshot(settings, catalog, bindings);
            return true;
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not load the configuration"
                + (this.current == null ? "." : "; keeping the previous one."), exception);
            return false;
        }
    }

    /** Builds a show from {@code shows/<id>.yml}, with the copy in the jar supplying defaults. */
    private Show show(String id, Function<SettingsFile, Show> factory) {
        String path = "shows/" + id + ".yml";
        File file = new File(this.plugin.getDataFolder(), path);
        if (!file.exists()) {
            this.plugin.saveResource(path, false);
        }
        YamlConfiguration values = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults;
        try (InputStream stream = this.plugin.getResource(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + path + " in the plugin jar");
            }
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path + " from the plugin jar", exception);
        }
        values.setDefaults(defaults);
        return factory.apply(new SettingsFile(path, values, defaults, this.logger));
    }

    public PluginSettings settings() {
        return this.snapshot().settings();
    }

    public ShowCatalog catalog() {
        return this.snapshot().catalog();
    }

    public Bindings bindings() {
        return this.snapshot().bindings();
    }

    /** The show ids server owners can bind or preview. */
    public List<String> showIds() {
        return this.catalog().ids().stream().sorted().toList();
    }

    private Snapshot snapshot() {
        Snapshot snapshot = this.current;
        if (snapshot == null) {
            throw new IllegalStateException("Configuration is not loaded");
        }
        return snapshot;
    }
}
