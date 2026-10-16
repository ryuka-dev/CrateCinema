package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.ExcellentCratesAPI;
import su.nightexpress.excellentcrates.api.animation.component.AnimationComponent;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.component.CrateComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelinePhase;
import su.nightexpress.excellentcrates.api.effect.crate.EffectComponent;

/**
 * Our part of ExcellentCrates' opening pipeline, and checks of how crates use our shows.
 *
 * <p>ExcellentCrates builds a fresh opening pipeline every time it is enabled and publishes
 * {@link ExcellentCratesAPI} as a Bukkit service as the last step, so service registration is the
 * signal to hook in. When ExcellentCrates goes away, running shows are ended at once so that
 * their rewards are granted while it can still grant them.
 */
public final class ExcellentCratesIntegration implements Listener {

    private static final String PLUGIN_NAME = "ExcellentCrates";
    /** CrateCinema 1.x bound crates to shows in this file. */
    private static final String LEGACY_BINDINGS = "bindings.yml";

    private final Plugin plugin;
    private final Logger logger;
    private final SessionManager sessions;
    private final LiveConfig config;

    private @Nullable ExcellentCratesAPI attachedApi;
    private @Nullable Registration registration;

    public ExcellentCratesIntegration(Plugin plugin, SessionManager sessions, LiveConfig config) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.sessions = sessions;
        this.config = config;
    }

    public void start() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        ExcellentCratesAPI api = this.plugin.getServer().getServicesManager().load(ExcellentCratesAPI.class);
        if (api != null) {
            this.attach(api);
        }
    }

    /** Switches our pipeline entry off. Running shows are ended by the caller. */
    public void stop() {
        this.detach();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == ExcellentCratesAPI.class
            && event.getProvider().getProvider() instanceof ExcellentCratesAPI api) {
            this.attach(api);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == ExcellentCratesAPI.class
            && event.getProvider().getProvider() == this.attachedApi) {
            this.detachAndEndShows();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals(PLUGIN_NAME)) {
            this.detachAndEndShows();
        }
    }

    private void attach(ExcellentCratesAPI api) {
        if (api == this.attachedApi) {
            return;
        }
        this.detach();

        Registration newRegistration = new Registration();
        try {
            api.getCratesAPI().pipeline().registerStage(PipelinePhase.PRE_ROLL, SessionGuardStage.ORDER,
                new SessionGuardStage(newRegistration, this.sessions, this.config));
        } catch (IllegalStateException exception) {
            newRegistration.deactivate();
            this.logger.severe("ExcellentCrates refused the registration (" + exception.getMessage()
                + "). Opening a crate while a preview is running will not be refused.");
        }

        this.attachedApi = api;
        this.registration = newRegistration;
        this.checkCrates();
        this.reportLegacyBindings();
    }

    /**
     * Warns about crates that play our shows with ExcellentCrates' particle effect on. Those
     * effects are shown to every player around the crate and overlap the show; this plugin cannot
     * hide them per player.
     */
    public void checkCrates() {
        ExcellentCratesAPI api = this.attachedApi;
        if (api == null) {
            return;
        }
        for (Crate crate : api.getCratesAPI().registry().crates()) {
            AnimationComponent animation = crate.getComponentOrNull(CrateComponentKeys.ANIMATION);
            if (animation == null || !animation.isEnabled()
                || !animation.getProfileKey().namespace().startsWith(CinemaAnimationProvider.ID_PREFIX)) {
                continue;
            }
            EffectComponent effect = crate.getComponentOrNull(CrateComponentKeys.EFFECT);
            if (effect != null && effect.isEnabled()) {
                this.logger.warning("Crate '" + crate.idString() + "' has the ExcellentCrates particle effect '" + effect.getProfileKey().asString()
                    + "' on. It is shown to everyone around the crate and overlaps the show; consider turning it off for this crate.");
            }
        }
    }

    /**
     * CrateCinema 1.x bound crates to shows in {@code bindings.yml}; crates now select a show as
     * their ExcellentCrates animation. Says what to set for each old binding. The file is left
     * alone, and ExcellentCrates' crate files are not changed.
     */
    private void reportLegacyBindings() {
        File file = new File(this.plugin.getDataFolder(), LEGACY_BINDINGS);
        if (!file.isFile()) {
            return;
        }
        ConfigurationSection bindings = YamlConfiguration.loadConfiguration(file).getConfigurationSection("bindings");
        if (bindings == null || bindings.getKeys(false).isEmpty()) {
            return;
        }
        this.logger.warning(LEGACY_BINDINGS + " is no longer used. A crate now plays a show when its ExcellentCrates animation is set to it"
            + " (crate editor, or 'animation' in the crate's file). For your old bindings:");
        for (String crateId : bindings.getKeys(false)) {
            this.logger.warning("  crate '" + crateId + "': animation enabled, profile "
                + CinemaAnimationProvider.ID_PREFIX + bindings.getString(crateId) + ":default");
        }
        this.logger.warning("Delete " + LEGACY_BINDINGS + " once that is done to stop this message.");
    }

    private void detach() {
        Registration current = this.registration;
        this.registration = null;
        this.attachedApi = null;
        if (current != null) {
            current.deactivate();
        }
    }

    private void detachAndEndShows() {
        if (this.attachedApi == null) {
            return;
        }
        this.detach();
        this.sessions.shutdown();
        this.logger.info("ExcellentCrates went away; ended running shows.");
    }
}
