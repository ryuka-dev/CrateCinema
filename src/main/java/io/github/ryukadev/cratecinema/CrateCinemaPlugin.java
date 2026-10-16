package io.github.ryukadev.cratecinema;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import io.github.ryukadev.cratecinema.command.CinemaCommand;
import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.Diagnostics;
import io.github.ryukadev.cratecinema.engine.SessionListener;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.integration.excellentcrates.AnimationRegistration;
import io.github.ryukadev.cratecinema.integration.excellentcrates.ExcellentCratesIntegration;
import io.github.ryukadev.cratecinema.integration.floodgate.FloodgateBedrockPlayers;
import io.github.ryukadev.cratecinema.platform.BedrockPlayers;

public final class CrateCinemaPlugin extends JavaPlugin {

    private static final String FLOODGATE = "floodgate";

    private @Nullable LiveConfig config;
    private @Nullable SessionManager sessions;
    private @Nullable ExcellentCratesIntegration integration;

    @Override
    public void onEnable() {
        LiveConfig liveConfig = new LiveConfig(this);
        if (!liveConfig.load()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.config = liveConfig;

        Diagnostics diagnostics = new Diagnostics(this.getLogger(), () -> liveConfig.settings().debug());
        SessionManager sessionManager = new SessionManager(this, diagnostics, () -> liveConfig.settings().engine());
        this.sessions = sessionManager;
        this.getServer().getPluginManager().registerEvents(new SessionListener(sessionManager), this);

        BedrockPlayers bedrock = this.bedrockPlayers();
        AnimationRegistration animations;
        try {
            // Before ExcellentCrates is enabled: it registers our shows while it starts up.
            animations = new AnimationRegistration(this, sessionManager, bedrock);
            animations.start();
            ExcellentCratesIntegration excellentCrates = new ExcellentCratesIntegration(this, sessionManager, liveConfig);
            excellentCrates.start();
            this.integration = excellentCrates;
        } catch (LinkageError error) {
            this.getLogger().log(Level.SEVERE, "Cannot load ExcellentCrates classes. Is ExcellentCrates 7.2 or newer installed?", error);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CinemaCommand command = new CinemaCommand(animations, sessionManager, this::reload);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(CinemaCommand.LABEL, CinemaCommand.DESCRIPTION, command));
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
        this.config = null;
    }

    /** Reads the configuration again; running shows keep what they started with. */
    private boolean reload() {
        LiveConfig liveConfig = this.config;
        if (liveConfig == null || !liveConfig.load()) {
            return false;
        }
        if (this.integration != null) {
            this.integration.checkCrates();
        }
        return true;
    }

    /** Floodgate's view of who is on Bedrock, or nobody if Floodgate is not installed. */
    private BedrockPlayers bedrockPlayers() {
        if (!this.getServer().getPluginManager().isPluginEnabled(FLOODGATE)) {
            return BedrockPlayers.NONE;
        }
        try {
            BedrockPlayers floodgate = new FloodgateBedrockPlayers();
            this.getLogger().info("Floodgate found; Bedrock players get the text-only simple show.");
            return floodgate;
        } catch (LinkageError | RuntimeException error) {
            this.getLogger().log(Level.WARNING, "Floodgate is installed but its API could not be used; treating everyone as a Java player.", error);
            return BedrockPlayers.NONE;
        }
    }
}
