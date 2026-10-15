package io.github.ryukadev.cratecinema;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.command.PreviewCommand;
import io.github.ryukadev.cratecinema.engine.Diagnostics;
import io.github.ryukadev.cratecinema.engine.SessionListener;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;
import io.github.ryukadev.cratecinema.engine.Tiers;
import io.github.ryukadev.cratecinema.integration.excellentcrates.ExcellentCratesIntegration;
import io.github.ryukadev.cratecinema.shows.OrbitShow;
import io.github.ryukadev.cratecinema.shows.TestShow;

public final class CrateCinemaPlugin extends JavaPlugin {

    private static final String BINDINGS_FILE = "bindings.yml";

    private @Nullable SessionManager sessions;
    private @Nullable ExcellentCratesIntegration integration;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        if (!new File(this.getDataFolder(), BINDINGS_FILE).exists()) {
            this.saveResource(BINDINGS_FILE, false);
        }

        ShowCatalog catalog = new ShowCatalog();
        catalog.register(new TestShow());
        catalog.register(new OrbitShow());
        Bindings bindings = Bindings.load(new File(this.getDataFolder(), BINDINGS_FILE), catalog, this.getLogger());

        Diagnostics diagnostics = new Diagnostics(this.getLogger(), this.getConfig().getBoolean("debug"));
        SessionManager sessionManager = new SessionManager(this, diagnostics, this.loadTiers());
        this.sessions = sessionManager;
        this.getServer().getPluginManager().registerEvents(new SessionListener(sessionManager), this);

        PluginCommand command = this.getCommand("cratecinema");
        if (command != null) {
            PreviewCommand preview = new PreviewCommand(catalog, sessionManager);
            command.setExecutor(preview);
            command.setTabCompleter(preview);
        }

        String busyMessage = colorize(this.getConfig().getString("messages.already-watching", ""));
        try {
            ExcellentCratesIntegration excellentCrates = new ExcellentCratesIntegration(this, sessionManager, bindings, busyMessage);
            excellentCrates.start();
            this.integration = excellentCrates;
        } catch (LinkageError error) {
            this.getLogger().log(Level.SEVERE, "Cannot load ExcellentCrates classes. Is ExcellentCrates 7.x installed?", error);
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.integration != null) {
            this.integration.stop();
            this.integration = null;
        }
        if (this.sessions != null) {
            this.sessions.shutdown();
            this.sessions = null;
        }
    }

    private Tiers loadTiers() {
        Map<String, Integer> tierByRarity = new HashMap<>();
        ConfigurationSection section = this.getConfig().getConfigurationSection("tiers");
        if (section != null) {
            for (String rarity : section.getKeys(false)) {
                tierByRarity.put(rarity, section.getInt(rarity));
            }
        }
        return new Tiers(tierByRarity, this.getConfig().getInt("default-tier", 0));
    }

    /** Translates '&' colour and format codes. */
    private static String colorize(String text) {
        return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
