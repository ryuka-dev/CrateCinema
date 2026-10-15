package io.github.ryukadev.cratecinema.engine;

import java.util.Map;

/**
 * Maps rarities to show tiers, as configured by the server owner. Tier 0 is the plainest show,
 * {@link #MAX} the grandest.
 */
public final class Tiers {

    public static final int MAX = 3;

    private final Map<String, Integer> tierByRarity;
    private final int defaultTier;

    public Tiers(Map<String, Integer> tierByRarity, int defaultTier) {
        this.tierByRarity = Map.copyOf(tierByRarity);
        this.defaultTier = clamp(defaultTier);
    }

    public int of(RewardView reward) {
        RarityView rarity = reward.rarity();
        if (rarity == null) {
            return this.defaultTier;
        }
        Integer tier = this.tierByRarity.get(rarity.id());
        return tier == null ? this.defaultTier : clamp(tier);
    }

    public static int clamp(int tier) {
        return Math.max(0, Math.min(MAX, tier));
    }
}
