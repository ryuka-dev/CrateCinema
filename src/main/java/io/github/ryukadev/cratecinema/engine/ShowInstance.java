package io.github.ryukadev.cratecinema.engine;

/**
 * One round of a show in progress. Owned and driven by {@link SessionManager}.
 */
public interface ShowInstance {

    /**
     * Advances the round by one server tick.
     *
     * @return {@code false} once the round has finished
     */
    boolean tick();

    /**
     * Removes everything the round spawned. Called exactly once, whether the round finished or
     * was interrupted. Must not throw.
     */
    void cleanup();
}
