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
    SHUTDOWN
}
