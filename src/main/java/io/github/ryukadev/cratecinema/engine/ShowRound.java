package io.github.ryukadev.cratecinema.engine;

import java.util.List;

/**
 * One reveal: the reward that was already decided by the crate plugin, plus other rewards from
 * the same pool that a show may flash past before revealing it.
 */
public record ShowRound(RewardView target, List<RewardView> fillers) {

    public ShowRound {
        fillers = List.copyOf(fillers);
    }
}
