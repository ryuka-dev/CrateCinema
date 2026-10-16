package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.function.Function;
import java.util.logging.Logger;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.excellentcrates.api.animation.AnimationProvider;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;
import su.nightexpress.nightcore.config.FileConfig;

/**
 * Offers one of our shows to ExcellentCrates as an animation type. ExcellentCrates keeps its
 * profiles in {@code objects/animations/cratecinema_<show>/}; each file is a profile a crate can
 * select, keyed {@code cratecinema_<show>:<file name>}.
 */
final class CinemaAnimationProvider implements AnimationProvider<CinemaAnimationProfile> {

    static final String ID_PREFIX = "cratecinema_";
    private static final String NAME_PATH = "name";

    private final Identifier id;
    private final String showId;
    private final LiveConfig config;
    private final SessionManager sessions;
    private final Function<RewardPreviewAPI, ShowRequests> requests;
    private final Logger logger;

    CinemaAnimationProvider(String showId, LiveConfig config, SessionManager sessions,
                            Function<RewardPreviewAPI, ShowRequests> requests, Logger logger) {
        this.id = new Identifier(ID_PREFIX + showId);
        this.showId = showId;
        this.config = config;
        this.sessions = sessions;
        this.requests = requests;
        this.logger = logger;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public CinemaAnimationProfile readProfile(AdaptedKey key, FileConfig file) {
        String name = file.getString(NAME_PATH, this.defaultName());
        return new CinemaAnimationProfile(key, name, this.showId, this.config, this.sessions, this.requests, this.logger);
    }

    @Override
    public void writeDefaultProfile(AdaptedKey key, FileConfig file) {
        file.set(NAME_PATH, this.defaultName());
    }

    private String defaultName() {
        return "CrateCinema: " + this.showId;
    }
}
