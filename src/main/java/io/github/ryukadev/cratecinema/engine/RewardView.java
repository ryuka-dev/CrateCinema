package io.github.ryukadev.cratecinema.engine;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a show displays for one reward. Built by a crate-plugin adapter; the engine never sees
 * the crate plugin's own reward type.
 *
 * @param name   display name as a legacy-formatted string ({@code §} codes, hex colours allowed)
 * @param rarity the reward's rarity, or {@code null} if it has none
 * @param chance probability of this reward in the roll it came from, 0 to 1, or {@code NaN} if unknown
 */
public record RewardView(ItemStack icon, String name, @Nullable RarityView rarity, double chance) {

    public RewardView {
        icon = icon.clone();
    }

    @Override
    public ItemStack icon() {
        return this.icon.clone();
    }
}
