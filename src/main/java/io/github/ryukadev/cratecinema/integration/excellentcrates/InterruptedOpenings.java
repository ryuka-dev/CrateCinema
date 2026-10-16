package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Players whose opening is completing after its show was interrupted. ExcellentCrates plays a
 * batch one reward at a time and starts the next reward's session from inside the previous one's
 * completion callback; sessions started while this is set complete at once, so an interrupted
 * batch (quit, death, teleport, time limit) grants all of its rewards together instead of
 * playing on.
 *
 * <p>Main thread only.
 */
final class InterruptedOpenings {

    private final Set<UUID> completing = new HashSet<>();

    boolean isCompleting(UUID playerId) {
        return this.completing.contains(playerId);
    }

    /** Runs {@code completion} with the player marked as completing an interrupted opening. */
    void complete(UUID playerId, Runnable completion) {
        boolean marked = this.completing.add(playerId);
        try {
            completion.run();
        } finally {
            if (marked) {
                this.completing.remove(playerId);
            }
        }
    }
}
