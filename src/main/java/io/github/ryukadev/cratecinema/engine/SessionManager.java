package io.github.ryukadev.cratecinema.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

/**
 * Owns every running show and the plugin's only tick task.
 *
 * <p>Guarantee: the completion callback handed to {@link #play} runs exactly once, whatever
 * happens to the show. The crate plugin has already taken the player's key by the time a show
 * starts, so a lost callback would mean a lost reward.
 *
 * <p>All methods must be called on the main thread.
 */
public final class SessionManager {

    /** A teleport further than this from the show's anchor ends the show. */
    private static final double MAX_TELEPORT_DISTANCE = 24.0;

    private final Plugin plugin;
    private final Logger logger;
    private final Diagnostics diagnostics;
    private final Supplier<EngineSettings> settings;

    private final Map<UUID, Session> sessions = new HashMap<>();
    /** Completions held back because the viewer died; released once they are alive again. */
    private final Map<UUID, Completion> heldCompletions = new HashMap<>();

    private @Nullable BukkitTask task;

    public SessionManager(Plugin plugin, Diagnostics diagnostics, Supplier<EngineSettings> settings) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.diagnostics = diagnostics;
        this.settings = settings;
    }

    /** Whether another show may start now, under the configured limit on concurrent shows. */
    public boolean hasCapacity() {
        return this.sessions.size() < this.settings.get().maxConcurrentShows();
    }

    /** Whether the player is watching a show or still has a reward waiting from one. */
    public boolean isBusy(UUID playerId) {
        return this.sessions.containsKey(playerId) || this.heldCompletions.containsKey(playerId);
    }

    /**
     * Plays a show and runs {@code onComplete} when it ends. Takes ownership of
     * {@code onComplete} even if the show cannot start; in that case it runs immediately.
     */
    public void play(Show show, ShowRequest request, Runnable onComplete) {
        this.play(show, request, onComplete, this.settings.get().tiers());
    }

    /** As {@link #play(Show, ShowRequest, Runnable)}, with a tier mapping other than the configured one. */
    public void play(Show show, ShowRequest request, Runnable onComplete, Tiers tiers) {
        Player viewer = request.viewer();
        UUID viewerId = viewer.getUniqueId();
        Completion completion = new Completion(onComplete, this.logger);

        if (!viewer.isOnline() || viewer.isDead() || this.isBusy(viewerId)) {
            this.diagnostics.debug(() -> "Skipping show for " + viewer.getName() + ": viewer not available");
            completion.run();
            return;
        }

        EngineSettings current = this.settings.get();
        ShowContext context = new ShowContext(this.plugin, viewer, request.anchor(), tiers, current);
        Session session = new Session(show, context, request.rounds(), completion, this.logger, current.maxShowTicks());
        this.sessions.put(viewerId, session);
        try {
            session.start();
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Show '" + show.id() + "' failed to start", exception);
            this.end(viewerId, EndReason.ERROR);
            return;
        }

        this.diagnostics.debug(() -> "Started show '" + show.id() + "' for " + viewer.getName()
            + " (" + request.rounds().size() + " reward(s))");
        this.ensureTicking();
    }

    public void handleQuit(UUID playerId) {
        this.end(playerId, EndReason.QUIT);
        // A player who quits while dead never respawns in this session; grant now, as the crate
        // plugin itself does for its own animations on quit.
        Completion held = this.heldCompletions.remove(playerId);
        if (held != null) {
            held.run();
        }
    }

    public void handleDeath(UUID playerId) {
        this.end(playerId, EndReason.DEATH);
    }

    public void handleWorldChange(UUID playerId) {
        this.end(playerId, EndReason.WORLD_CHANGE);
    }

    public void handleTeleport(UUID playerId, Location to) {
        Session session = this.sessions.get(playerId);
        if (session == null) {
            return;
        }
        Location anchor = session.anchor();
        if (to.getWorld() != anchor.getWorld() || to.distanceSquared(anchor) > MAX_TELEPORT_DISTANCE * MAX_TELEPORT_DISTANCE) {
            this.end(playerId, EndReason.TELEPORT);
        }
    }

    /** Ends every show and releases every held reward. */
    public void shutdown() {
        for (UUID playerId : List.copyOf(this.sessions.keySet())) {
            this.end(playerId, EndReason.SHUTDOWN);
        }
        for (UUID playerId : List.copyOf(this.heldCompletions.keySet())) {
            Completion held = this.heldCompletions.remove(playerId);
            if (held != null) {
                held.run();
            }
        }
        this.stopTicking();
    }

    private void end(UUID playerId, EndReason reason) {
        // Remove first: the completion may start another opening for the same player.
        Session session = this.sessions.remove(playerId);
        if (session == null) {
            return;
        }
        session.closeCurrent();
        this.diagnostics.debug(() -> "Ended show '" + session.showId() + "' for " + playerId + ": " + reason);

        if (reason == EndReason.DEATH) {
            // Items given to a dead player can be lost with the death inventory.
            this.heldCompletions.put(playerId, session.completion());
            this.ensureTicking();
        } else {
            session.completion().run();
        }
    }

    private void tick() {
        for (UUID playerId : List.copyOf(this.sessions.keySet())) {
            Session session = this.sessions.get(playerId);
            if (session == null) {
                continue;
            }
            if (session.expired()) {
                this.logger.warning("Show '" + session.showId() + "' ran past the configured time limit; ending it and granting the reward.");
                this.end(playerId, EndReason.TIMEOUT);
                continue;
            }
            boolean running;
            try {
                running = session.tick();
            } catch (RuntimeException exception) {
                this.logger.log(Level.SEVERE, "Show '" + session.showId() + "' failed while running", exception);
                this.end(playerId, EndReason.ERROR);
                continue;
            }
            if (!running) {
                this.end(playerId, EndReason.FINISHED);
            }
        }

        this.releaseHeldCompletions();

        if (this.sessions.isEmpty() && this.heldCompletions.isEmpty()) {
            this.stopTicking();
        }
    }

    private void releaseHeldCompletions() {
        for (UUID playerId : List.copyOf(this.heldCompletions.keySet())) {
            Player player = this.plugin.getServer().getPlayer(playerId);
            if (player == null || player.isDead()) {
                continue;
            }
            Completion held = this.heldCompletions.remove(playerId);
            if (held != null) {
                this.diagnostics.debug(() -> "Granting held reward for " + player.getName() + " after respawn");
                held.run();
            }
        }
    }

    private void ensureTicking() {
        if (this.task == null) {
            this.task = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this::tick, 1L, 1L);
        }
    }

    private void stopTicking() {
        BukkitTask current = this.task;
        this.task = null;
        if (current != null) {
            current.cancel();
        }
    }
}
