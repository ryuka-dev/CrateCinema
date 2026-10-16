package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.function.Function;
import java.util.logging.Logger;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;
import su.nightexpress.excellentcrates.api.animation.AnimationProfile;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;

/**
 * One of our shows as an ExcellentCrates animation profile, which a crate selects by its key. The
 * show is looked up when an opening starts, so a configuration reload applies to the next one.
 */
final class CinemaAnimationProfile implements AnimationProfile {

    private final AdaptedKey key;
    private final String name;
    private final String showId;
    private final LiveConfig config;
    private final SessionManager sessions;
    private final Function<RewardPreviewAPI, ShowRequests> requests;
    private final Logger logger;

    CinemaAnimationProfile(AdaptedKey key, String name, String showId, LiveConfig config, SessionManager sessions,
                           Function<RewardPreviewAPI, ShowRequests> requests, Logger logger) {
        this.key = key;
        this.name = name;
        this.showId = showId;
        this.config = config;
        this.sessions = sessions;
        this.requests = requests;
        this.logger = logger;
    }

    @Override
    public AdaptedKey getKey() {
        return this.key;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public AnimationInstance createInstance(AnimationContext context, Runnable onComplete) {
        var show = this.config.catalog().get(this.showId).orElse(null);
        if (show == null) {
            this.logger.warning("Animation '" + this.key.asString() + "' plays show '" + this.showId
                + "', which is not loaded; granting the opening without a show.");
        }
        return new CinemaAnimationInstance(show, context, this.sessions,
            this.requests.apply(context.rewardPreview()), this.logger, onComplete);
    }
}
