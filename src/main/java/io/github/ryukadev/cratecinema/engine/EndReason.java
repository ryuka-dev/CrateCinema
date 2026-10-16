package io.github.ryukadev.cratecinema.engine;

/**
 * Why a session ended.
 */
public enum EndReason {
    FINISHED,
    QUIT,
    DEATH,
    WORLD_CHANGE,
    TELEPORT,
    ERROR,
    /** Ran past the configured maximum show length. */
    TIMEOUT,
    /** The crate plugin is going away or this plugin is being disabled. */
    SHUTDOWN,
    /** The crate plugin ended the show itself. */
    STOPPED,
    /** The show never started: the viewer was offline, dead or already watching one. */
    SKIPPED
}
