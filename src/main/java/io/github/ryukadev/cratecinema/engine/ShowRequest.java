package io.github.ryukadev.cratecinema.engine;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A request to play a show for one viewer. Rounds are played one after another; a batch open
 * produces several rounds.
 *
 * @param anchor the point the show is centred on
 */
public record ShowRequest(Player viewer, Location anchor, List<ShowRound> rounds) {

    public ShowRequest {
        anchor = anchor.clone();
        rounds = List.copyOf(rounds);
        if (rounds.isEmpty()) {
            throw new IllegalArgumentException("A show request needs at least one round");
        }
    }

    @Override
    public Location anchor() {
        return this.anchor.clone();
    }
}
