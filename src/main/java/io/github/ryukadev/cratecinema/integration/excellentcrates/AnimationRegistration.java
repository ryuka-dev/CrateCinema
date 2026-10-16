package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;
import io.github.ryukadev.cratecinema.platform.BedrockPlayers;
import io.github.ryukadev.cratecinema.shows.ShowType;
import io.github.ryukadev.cratecinema.shows.SimpleShow;
import su.nightexpress.excellentcrates.api.CoreServices;
import su.nightexpress.excellentcrates.api.animation.AnimationAPI;
import su.nightexpress.excellentcrates.api.animation.AnimationProfile;
import su.nightexpress.excellentcrates.api.animation.AnimationRegistry;
import su.nightexpress.excellentcrates.api.engine.event.ExcellentCratesModulesRegisteredEvent;
import su.nightexpress.excellentcrates.api.rarity.RarityAPI;
import su.nightexpress.excellentcrates.api.rarity.registry.RarityResolver;

/**
 * Registers our shows as ExcellentCrates animation types, and lists their profiles for previews.
 *
 * <p>ExcellentCrates builds a new animation registry every time it is enabled and fires
 * {@link ExcellentCratesModulesRegisteredEvent} once its modules have registered their services,
 * before any of them starts and loads its data, so the profiles of types registered here load
 * with ExcellentCrates' own. We must be listening before ExcellentCrates is enabled, which is why
 * it loads after us. The registry is kept only until ExcellentCrates is disabled.
 */
public final class AnimationRegistration implements Listener, ShowCatalog {

    private static final String PLUGIN_NAME = "ExcellentCrates";
    private static final String DEFAULT_PROFILE = "default";

    private final Plugin plugin;
    private final Logger logger;
    private final SessionManager sessions;
    private final BedrockPlayers bedrock;

    private @Nullable AnimationRegistry registry;

    public AnimationRegistration(Plugin plugin, SessionManager sessions, BedrockPlayers bedrock) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.sessions = sessions;
        this.bedrock = bedrock;
    }

    public void start() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onModulesRegistered(ExcellentCratesModulesRegisteredEvent event) {
        // The event's service registry must not be kept; take what we need now.
        AnimationAPI animations = event.getServices().get(CoreServices.ANIMATION);
        if (animations == null) {
            this.logger.severe("ExcellentCrates has no animation service; shows are not available.");
            return;
        }
        RarityAPI rarityApi = event.getServices().get(CoreServices.RARITY);
        RarityResolver rarities = rarityApi == null ? null : rarityApi.getResolver();

        ShowRuntime runtime = new ShowRuntime(this.sessions, this.bedrock, new SimpleShow(),
            preview -> new ShowRequests(preview, rarities), new InterruptedOpenings(), this.logger);
        for (ShowType type : ShowType.ALL) {
            animations.getRegistry().registerProvider(new CinemaAnimationProvider(type, this.plugin, runtime));
        }
        this.registry = animations.getRegistry();
        this.logger.info("Registered " + ShowType.ALL.size() + " shows as ExcellentCrates animation types.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals(PLUGIN_NAME)) {
            this.registry = null;
        }
    }

    /**
     * Our profiles by preview name: the show id for a show's default profile ({@code orbit}),
     * otherwise {@code <show>:<profile>} ({@code orbit:fast}).
     */
    private TreeMap<String, CinemaAnimationProfile> profiles() {
        TreeMap<String, CinemaAnimationProfile> byName = new TreeMap<>();
        AnimationRegistry current = this.registry;
        if (current == null) {
            return byName;
        }
        for (AnimationProfile profile : current.getProfiles()) {
            if (profile instanceof CinemaAnimationProfile ours) {
                String showId = ours.getKey().namespace().substring(CinemaAnimationProvider.ID_PREFIX.length());
                String file = ours.getKey().value();
                byName.put(file.equals(DEFAULT_PROFILE) ? showId : showId + ":" + file, ours);
            }
        }
        return byName;
    }

    @Override
    public List<String> names() {
        return List.copyOf(this.profiles().keySet());
    }

    @Override
    public Optional<Show> get(String name) {
        return Optional.ofNullable(this.profiles().get(name)).map(CinemaAnimationProfile::show);
    }
}
