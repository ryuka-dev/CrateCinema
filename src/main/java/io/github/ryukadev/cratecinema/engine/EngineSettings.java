package io.github.ryukadev.cratecinema.engine;

/**
 * Engine-wide settings. A show keeps the settings it started with; a reload only affects shows
 * started afterwards.
 *
 * @param audience           who sees a show
 * @param nearbyRadius       for {@link Audience#NEARBY}: how close other players must be when the show starts
 * @param soundVolume        multiplier for every show sound; 0 silences them
 * @param particleDensity    multiplier for particle counts; 0 turns particles off
 * @param maxConcurrentShows openings beyond this many running shows get no show
 * @param maxShowTicks       a show still running after this long is ended and its reward granted
 */
public record EngineSettings(
    Tiers tiers,
    Audience audience,
    double nearbyRadius,
    double soundVolume,
    double particleDensity,
    int maxConcurrentShows,
    int maxShowTicks) {

    public enum Audience {
        /** Only the player opening the crate. */
        VIEWER,
        /** The opener and every player near the crate when the show starts. */
        NEARBY
    }
}
