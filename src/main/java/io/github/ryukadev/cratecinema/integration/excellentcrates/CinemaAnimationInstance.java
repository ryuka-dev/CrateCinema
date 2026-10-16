package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.UUID;
import java.util.logging.Level;

import io.github.ryukadev.cratecinema.engine.EndReason;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;

/**
 * One reward's show inside an ExcellentCrates animation session. ExcellentCrates starts the
 * session, asks each tick whether it is still running, and stops it when the player quits or
 * changes world or when it shuts down. The show itself is driven by our engine.
 *
 * <p>ExcellentCrates' completion callback (which plays the next reward of a batch, or grants them
 * all after the last) runs exactly once: when the show ends or the session is stopped, whichever
 * comes first. ExcellentCrates may call {@link #stop()} more than once, and after the show has
 * completed; both are no-ops here.
 */
final class CinemaAnimationInstance implements AnimationInstance {

    private final Show show;
    private final AnimationContext context;
    private final ShowRuntime runtime;
    private final ShowRequests requests;
    private final Runnable onComplete;
    private final UUID playerId;

    private boolean running;
    private boolean completed;

    CinemaAnimationInstance(Show show, AnimationContext context, ShowRuntime runtime, Runnable onComplete) {
        this.show = show;
        this.context = context;
        this.runtime = runtime;
        this.requests = runtime.requests().apply(context.rewardPreview());
        this.onComplete = onComplete;
        this.playerId = context.player().getUniqueId();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void start() {
        this.running = true;
        if (this.runtime.interrupted().isCompleting(this.playerId)) {
            // A later reward of a batch whose show was interrupted.
            this.finish(EndReason.SKIPPED);
            return;
        }
        ShowRequest request;
        try {
            request = this.requests.create(this.context);
        } catch (RuntimeException exception) {
            this.runtime.logger().log(Level.SEVERE, "Could not prepare a show; granting the opening without one", exception);
            this.finish(EndReason.ERROR);
            return;
        }
        // The engine calls finish at most once, and at once if the show cannot start.
        this.runtime.sessions().play(this.show, request, this::finish);
    }

    /**
     * Still running while the show plays and while a reward is held back after a death, so that
     * ExcellentCrates keeps the session (and refuses another opening) until it is granted.
     */
    @Override
    public boolean tick() {
        return this.running;
    }

    @Override
    public void stop() {
        if (this.completed) {
            return;
        }
        this.runtime.sessions().handleStop(this.playerId);
        // In case the engine never had the show.
        this.finish(EndReason.STOPPED);
    }

    private void finish(EndReason reason) {
        if (this.completed) {
            return;
        }
        this.completed = true;
        // Not running before the callback: it may start the next session for the same player,
        // which ExcellentCrates refuses while this one counts as running.
        this.running = false;
        if (reason == EndReason.FINISHED) {
            this.complete();
        } else {
            this.runtime.interrupted().complete(this.playerId, this::complete);
        }
    }

    private void complete() {
        try {
            this.onComplete.run();
        } catch (RuntimeException exception) {
            this.runtime.logger().log(Level.SEVERE, "ExcellentCrates failed while completing an opening", exception);
        }
    }
}
