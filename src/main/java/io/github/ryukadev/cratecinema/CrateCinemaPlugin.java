package io.github.ryukadev.cratecinema;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.engine.Diagnostics;
import io.github.ryukadev.cratecinema.engine.SessionListener;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;
import io.github.ryukadev.cratecinema.integration.excellentcrates.ExcellentCratesIntegration;
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
        Bindings bindings = Bindings.load(new File(this.getDataFolder(), BINDINGS_FILE), catalog, this.getLogger());

        Diagnostics diagnostics = new Diagnostics(this.getLogger(), this.getConfig().getBoolean("debug"));
        SessionManager sessionManager = new SessionManager(this, diagnostics);
        this.sessions = sessionManager;
        this.getServer().getPluginManager().registerEvents(new SessionListener(sessionManager), this);

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

    /** Translates '&' colour and format codes. */
    private static String colorize(String text) {
        return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
