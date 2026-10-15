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
    /** The crate plugin is going away or this plugin is being disabled. */
    SHUTDOWN
}
