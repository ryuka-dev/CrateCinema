package io.github.ryukadev.cratecinema.engine;

import org.bukkit.inventory.ItemStack;

/**
 * What a show displays for one reward. Built by a crate-plugin adapter; the engine never sees
 * the crate plugin's own reward type.
 */
public record RewardView(ItemStack icon) {

    public RewardView {
        icon = icon.clone();
    }

    @Override
    public ItemStack icon() {
        return this.icon.clone();
    }
}
