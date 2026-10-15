package io.github.ryukadev.cratecinema.engine;

import org.bukkit.Color;

/**
 * A reward's rarity as shows see it.
 *
 * @param id    the crate plugin's rarity id, used to look up the tier
 * @param name  display name as a legacy-formatted string
 * @param color the rarity's colour, used for glows and particles
 */
public record RarityView(String id, String name, Color color) {
}
