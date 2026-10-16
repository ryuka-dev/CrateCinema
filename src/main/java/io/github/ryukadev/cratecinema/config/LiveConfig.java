package io.github.ryukadev.cratecinema.config;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

/**
 * Owns the plugin's current settings from {@code config.yml}. Loading reads the whole file and
 * swaps the result in only if it loaded, so a broken file never leaves the plugin half-configured.
 * Shows already running keep what they started with.
 *
 * <p>The shows' own settings are ExcellentCrates animation profiles, which ExcellentCrates loads
 * and reloads.
 *
 * <p>Main thread only.
 */
public final class LiveConfig {

    private final JavaPlugin plugin;
    private final Logger logger;
    private @Nullable PluginSettings current;

    public LiveConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Reads {@code config.yml} again. On failure the previous settings stay in place.
     *
     * @return whether the new settings were applied
     */
    public boolean load() {
        try {
            this.plugin.saveDefaultConfig();
            this.plugin.reloadConfig();
            this.current = PluginSettings.read(
                new SettingsFile("config.yml", this.plugin.getConfig(), this.plugin.getConfig().getDefaults(), this.logger));
            return true;
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not load the configuration"
                + (this.current == null ? "." : "; keeping the previous one."), exception);
            return false;
        }
    }

    public PluginSettings settings() {
        PluginSettings settings = this.current;
        if (settings == null) {
            throw new IllegalStateException("Configuration is not loaded");
        }
        return settings;
    }
}
