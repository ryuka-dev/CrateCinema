package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.shows.TestShow;
import su.nightexpress.excellentcrates.api.CoreServices;
import su.nightexpress.excellentcrates.api.animation.AnimationAPI;
import su.nightexpress.excellentcrates.api.engine.event.ExcellentCratesModulesRegisteredEvent;
import su.nightexpress.excellentcrates.api.rarity.RarityAPI;
import su.nightexpress.excellentcrates.api.rarity.registry.RarityResolver;

/**
 * Registers our shows as ExcellentCrates animation types. ExcellentCrates builds a new animation
 * registry every time it is enabled and fires {@link ExcellentCratesModulesRegisteredEvent} once
 * its modules have registered their services, before any of them starts and loads its data, so
 * profiles of types registered here are loaded with ExcellentCrates' own. We must be listening
 * before ExcellentCrates is enabled, which is why it loads after us.
 */
public final class AnimationRegistration implements Listener {

    /** Shows offered to ExcellentCrates so far. */
    private static final List<String> SHOWS = List.of(TestShow.ID);

    private final Plugin plugin;
    private final Logger logger;
    private final LiveConfig config;
    private final SessionManager sessions;

    public AnimationRegistration(Plugin plugin, LiveConfig config, SessionManager sessions) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = config;
        this.sessions = sessions;
    }

    public void start() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onModulesRegistered(ExcellentCratesModulesRegisteredEvent event) {
        // The event's service registry must not be kept; take what we need now.
        AnimationAPI animations = event.getServices().get(CoreServices.ANIMATION);
        if (animations == null) {
            this.logger.severe("ExcellentCrates has no animation service; shows cannot be offered as animations.");
            return;
        }
        RarityAPI rarityApi = event.getServices().get(CoreServices.RARITY);
        RarityResolver rarities = rarityApi == null ? null : rarityApi.getResolver();

        for (String showId : SHOWS) {
            animations.getRegistry().registerProvider(new CinemaAnimationProvider(showId, this.config, this.sessions,
                preview -> new ShowRequests(preview, rarities), this.logger));
        }
        this.logger.info("Registered " + SHOWS.size() + " show(s) as ExcellentCrates animation types.");
    }
}
