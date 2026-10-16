package io.github.ryukadev.cratecinema.integration.excellentcrates;

import io.github.ryukadev.cratecinema.engine.Show;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;
import su.nightexpress.excellentcrates.api.animation.AnimationProfile;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;

/**
 * One profile file of one of our shows, which a crate selects by its key. ExcellentCrates reads
 * the file again on {@code /ecrates reload}, which replaces this profile; shows already running
 * keep the settings they started with.
 */
final class CinemaAnimationProfile implements AnimationProfile {

    private final AdaptedKey key;
    private final String name;
    private final Show show;
    private final ShowRuntime runtime;

    CinemaAnimationProfile(AdaptedKey key, String name, Show show, ShowRuntime runtime) {
        this.key = key;
        this.name = name;
        this.show = show;
        this.runtime = runtime;
    }

    @Override
    public AdaptedKey getKey() {
        return this.key;
    }

    @Override
    public String getName() {
        return this.name;
    }

    Show show() {
        return this.show;
    }

    @Override
    public AnimationInstance createInstance(AnimationContext context, Runnable onComplete) {
        Show chosen = this.show;
        if (this.runtime.bedrock().isBedrock(context.player()) || !this.runtime.sessions().hasCapacity()) {
            chosen = this.runtime.fallback();
        }
        return new CinemaAnimationInstance(chosen, context, this.runtime, onComplete);
    }
}
