package io.github.ryukadev.cratecinema.integration.floodgate;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import io.github.ryukadev.cratecinema.platform.BedrockPlayers;

/**
 * Bedrock detection through Floodgate. The only class that touches Floodgate; load it only when
 * the Floodgate plugin is enabled.
 */
public final class FloodgateBedrockPlayers implements BedrockPlayers {

    private final FloodgateApi api = FloodgateApi.getInstance();

    @Override
    public boolean isBedrock(Player player) {
        return this.api.isFloodgatePlayer(player.getUniqueId());
    }
}
