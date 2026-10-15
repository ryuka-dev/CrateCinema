package io.github.ryukadev.cratecinema.engine;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

/**
 * One viewer's show: plays the rounds in order. Driven by {@link SessionManager}.
 */
final class Session {

    private final Show show;
    private final ShowContext context;
    private final List<ShowRound> rounds;
    private final Completion completion;
    private final Logger logger;

    private int roundIndex;
    private @Nullable ShowInstance current;

    Session(Show show, ShowContext context, List<ShowRound> rounds, Completion completion, Logger logger) {
        this.show = show;
        this.context = context;
        this.rounds = rounds;
        this.completion = completion;
        this.logger = logger;
    }

    void start() {
        this.current = this.show.create(this.context, this.rounds.get(0));
    }

    /** @return {@code false} once every round has finished */
    boolean tick() {
        ShowInstance instance = this.current;
        if (instance == null) {
            return false;
        }
        if (instance.tick()) {
            return true;
        }

        this.closeCurrent();
        this.roundIndex++;
        if (this.roundIndex >= this.rounds.size()) {
            return false;
        }
        this.current = this.show.create(this.context, this.rounds.get(this.roundIndex));
        return true;
    }

    /** Removes whatever the current round spawned. Safe to call more than once. */
    void closeCurrent() {
        ShowInstance instance = this.current;
        this.current = null;
        if (instance == null) {
            return;
        }
        try {
            instance.cleanup();
        } catch (RuntimeException exception) {
            this.logger.log(Level.WARNING, "Show '" + this.show.id() + "' failed to clean up", exception);
        }
    }

    Location anchor() {
        return this.context.anchor();
    }

    Completion completion() {
        return this.completion;
    }

    String showId() {
        return this.show.id();
    }
}
