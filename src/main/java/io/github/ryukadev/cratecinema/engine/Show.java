package io.github.ryukadev.cratecinema.engine;

/**
 * A kind of show that crates can play.
 */
public interface Show {

    /** The show's id: how the crate plugin integration names it, and the name used in previews. */
    String id();

    /** Creates the instance that plays a single round. Called on the main thread. */
    ShowInstance create(ShowContext context, ShowRound round);
}
