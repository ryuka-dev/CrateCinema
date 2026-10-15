package io.github.ryukadev.cratecinema.engine;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

/**
 * One viewer's show, driven by {@link SessionManager}.
 *
 * <p>A single reward plays the show once. A batch plays the show once for its best reward
 * (highest tier, then lowest chance), followed by a short summary of every reward in the batch,
 * so a large batch does not turn into minutes of repeated shows.
 */
final class Session {

    private final Show show;
    private final ShowContext context;
    private final List<Supplier<ShowInstance>> steps;
    private final Completion completion;
    private final Logger logger;
    private final int maxTicks;

    private int stepIndex;
    private int age;
    private @Nullable ShowInstance current;

    Session(Show show, ShowContext context, List<ShowRound> rounds, Completion completion, Logger logger, int maxTicks) {
        this.show = show;
        this.context = context;
        this.steps = plan(show, context, rounds);
        this.completion = completion;
        this.logger = logger;
        this.maxTicks = maxTicks;
    }

    private static List<Supplier<ShowInstance>> plan(Show show, ShowContext context, List<ShowRound> rounds) {
        if (rounds.size() == 1) {
            ShowRound only = rounds.get(0);
            return List.of(() -> show.create(context, only));
        }
        ShowRound best = rounds.stream()
            .max(Comparator.comparingInt((ShowRound round) -> context.tierOf(round.target()))
                .thenComparing(round -> -knownChance(round.target())))
            .orElseThrow();
        List<RewardView> all = rounds.stream().map(ShowRound::target).toList();
        return List.of(() -> show.create(context, best), () -> new SummaryShow(context, all));
    }

    private static double knownChance(RewardView reward) {
        return Double.isNaN(reward.chance()) ? 1.0 : reward.chance();
    }

    void start() {
        this.current = this.steps.get(0).get();
    }

    /** Whether the show has run longer than it may. */
    boolean expired() {
        return this.age >= this.maxTicks;
    }

    /** @return {@code false} once every step has finished */
    boolean tick() {
        this.age++;
        ShowInstance instance = this.current;
        if (instance == null) {
            return false;
        }
        if (instance.tick()) {
            return true;
        }

        this.closeCurrent();
        this.stepIndex++;
        if (this.stepIndex >= this.steps.size()) {
            return false;
        }
        this.current = this.steps.get(this.stepIndex).get();
        return true;
    }

    /** Removes whatever the current step spawned. Safe to call more than once. */
    void closeCurrent() {
        ShowInstance instance = this.current;
        this.current = null;
        if (instance != null) {
            try {
                instance.cleanup();
            } catch (RuntimeException exception) {
                this.logger.log(Level.WARNING, "Show '" + this.show.id() + "' failed to clean up", exception);
            }
        }
        this.context.removeAll();
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
