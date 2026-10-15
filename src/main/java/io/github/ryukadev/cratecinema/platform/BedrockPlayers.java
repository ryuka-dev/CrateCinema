package io.github.ryukadev.cratecinema.platform;

import org.bukkit.entity.Player;

/**
 * Tells Bedrock players apart from Java players. Bedrock clients (through Geyser) cannot show item
 * and block displays, so they need a different opening.
 */
@FunctionalInterface
public interface BedrockPlayers {

    /** Used when no Bedrock bridge is installed: everyone is a Java player. */
    BedrockPlayers NONE = player -> false;

    boolean isBedrock(Player player);
}
