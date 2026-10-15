package io.github.ryukadev.cratecinema.integration.excellentcrates;

import su.nightexpress.excellentcrates.api.crate.pipeline.component.AnimationProfileComponent;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;

/**
 * Marks an opening as animated. ExcellentCrates hides the crate's hologram from the player while
 * an opening carrying this component runs, and shows it again when the opening completes. Its
 * own animation processor never takes over because ours has the higher priority.
 */
final class AnimationMarker implements AnimationProfileComponent {

    private AdaptedKey profileKey;

    AnimationMarker(AdaptedKey profileKey) {
        this.profileKey = profileKey;
    }

    @Override
    public AdaptedKey getProfileKey() {
        return this.profileKey;
    }

    @Override
    public void setProfileKey(AdaptedKey profileKey) {
        this.profileKey = profileKey;
    }
}
