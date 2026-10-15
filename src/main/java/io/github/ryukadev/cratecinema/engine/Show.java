package io.github.ryukadev.cratecinema.engine;

/**
 * A kind of show that server owners can bind crates to.
 */
public interface Show {

    /** The id used in the bindings file. */
    String id();

    /** Creates the instance that plays a single round. Called on the main thread. */
    ShowInstance create(ShowContext context, ShowRound round);
}
