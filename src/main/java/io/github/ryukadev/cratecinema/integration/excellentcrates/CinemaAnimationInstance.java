package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;

/**
 * One reward's show inside an ExcellentCrates animation session. ExcellentCrates starts, ticks
 * and stops it; the show itself runs in our engine. ExcellentCrates' completion callback (which
 * plays the next reward of a batch, or grants them all after the last) runs exactly once, when
 * the show ends or the session is stopped, whichever comes first.
 *
 * <p>ExcellentCrates may call {@link #stop()} more than once, and after the show has already
 * completed; both are no-ops here.
 */
final class CinemaAnimationInstance implements AnimationInstance {

    private final @Nullable Show show;
    private final AnimationContext context;
    private final SessionManager sessions;
    private final ShowRequests requests;
    private final Logger logger;
    private final Runnable onComplete;

    private boolean running;
    private boolean completed;

    CinemaAnimationInstance(@Nullable Show show, AnimationContext context, SessionManager sessions,
                            ShowRequests requests, Logger logger, Runnable onComplete) {
        this.show = show;
        this.context = context;
        this.sessions = sessions;
        this.requests = requests;
        this.logger = logger;
        this.onComplete = onComplete;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void start() {
        this.running = true;
        if (this.show == null) {
            this.finish();
            return;
        }
        ShowRequest request;
        try {
            request = this.requests.create(this.context);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not prepare a show; granting the opening without one", exception);
            this.finish();
            return;
        }
        // The engine runs finish at most once, and at once if the show cannot start.
        this.sessions.play(this.show, request, this::finish);
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
        this.sessions.handleStop(this.context.player().getUniqueId());
        // In case the engine never had the show.
        this.finish();
    }

    private void finish() {
        if (this.completed) {
            return;
        }
        this.completed = true;
        // Not running before the callback: it may start the next session for the same player,
        // which ExcellentCrates refuses while this one counts as running.
        this.running = false;
        try {
            this.onComplete.run();
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "ExcellentCrates failed while completing an opening", exception);
        }
    }
}
