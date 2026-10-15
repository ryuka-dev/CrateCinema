package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.ExcellentCratesAPI;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineAPI;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelinePhase;
import su.nightexpress.excellentcrates.api.rarity.RarityAPI;
import su.nightexpress.excellentcrates.api.rarity.registry.RarityResolver;
import su.nightexpress.excellentcrates.api.reward.RewardsAPI;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;
import su.nightexpress.nightcore.bridge.key.KeyDomain;

/**
 * The only place that talks to ExcellentCrates.
 *
 * <p>ExcellentCrates builds a fresh opening pipeline every time it is enabled and publishes
 * {@link ExcellentCratesAPI} as a Bukkit service as the last step, so service registration is the
 * signal to hook in. When ExcellentCrates goes away, running shows are ended at once so that
 * their rewards are granted while it can still grant them.
 */
public final class ExcellentCratesIntegration implements Listener {

    private static final String PLUGIN_NAME = "ExcellentCrates";

    private final Plugin plugin;
    private final Logger logger;
    private final SessionManager sessions;
    private final Bindings bindings;
    private final String busyMessage;

    private @Nullable ExcellentCratesAPI attachedApi;
    private @Nullable Registration registration;

    public ExcellentCratesIntegration(Plugin plugin, SessionManager sessions, Bindings bindings, String busyMessage) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.sessions = sessions;
        this.bindings = bindings;
        this.busyMessage = busyMessage;
    }

    public void start() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        ExcellentCratesAPI api = this.plugin.getServer().getServicesManager().load(ExcellentCratesAPI.class);
        if (api == null) {
            this.logger.warning("ExcellentCrates has not published its API yet; waiting for it.");
            return;
        }
        this.attach(api);
    }

    /** Switches our pipeline entries off. Running shows are ended by the caller. */
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

        Optional<RewardPreviewAPI> preview = api.getRewardAPI().map(RewardsAPI::getView);
        if (preview.isEmpty()) {
            this.logger.severe("ExcellentCrates' rewards module is not available; shows are disabled.");
            return;
        }

        RarityResolver rarities = api.getRarityAPI().map(RarityAPI::getResolver).orElse(null);
        AdaptedKey markerKey = KeyDomain.of("cratecinema").makeSafe("show").orElseThrow();

        Registration newRegistration = new Registration();
        try {
            PipelineAPI pipeline = api.getCratesAPI().pipeline();
            pipeline.registerStage(PipelinePhase.PRE_ROLL, SessionGuardStage.ORDER,
                new SessionGuardStage(newRegistration, this.sessions, this.bindings, markerKey, this.busyMessage));
            pipeline.registerProcessor(new CinemaPipelineProcessor(newRegistration, this.bindings, this.sessions,
                new ShowRequests(preview.get(), rarities), this.logger));
        } catch (IllegalStateException exception) {
            newRegistration.deactivate();
            this.logger.severe("ExcellentCrates refused the registration (" + exception.getMessage()
                + "). Shows are disabled; crates keep their normal opening.");
            return;
        }

        this.attachedApi = api;
        this.registration = newRegistration;
        this.logger.info("Hooked into the ExcellentCrates opening pipeline.");
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
        if (this.registration == null) {
            return;
        }
        this.detach();
        this.sessions.shutdown();
        this.logger.info("ExcellentCrates went away; ended running shows and unhooked.");
    }
}
