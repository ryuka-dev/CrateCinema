package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineProcessor;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateRewardsPipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.PipelineComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;

/**
 * Takes over the last step of an opening for bound crates: instead of ExcellentCrates' own
 * animation, our show plays, and ExcellentCrates' completion callback (which grants the
 * rewards) runs when it ends.
 *
 * <p>Rewards are rolled and the cost is taken by ExcellentCrates before this runs.
 */
final class CinemaPipelineProcessor implements PipelineProcessor {

    /** Above ExcellentCrates' animation processor (1) and default processor (0). */
    static final int PRIORITY = 10;

    private final Registration registration;
    private final Bindings bindings;
    private final SessionManager sessions;
    private final ShowRequests requests;
    private final Logger logger;

    CinemaPipelineProcessor(Registration registration, Bindings bindings, SessionManager sessions,
                            ShowRequests requests, Logger logger) {
        this.registration = registration;
        this.bindings = bindings;
        this.sessions = sessions;
        this.requests = requests;
        this.logger = logger;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean shouldHandle(Crate crate, PipelineContext context) {
        if (!this.registration.isActive() || context.hasComponent(PipelineComponentKeys.FAST_OPEN)) {
            return false;
        }
        if (this.bindings.showFor(crate.idString()).isEmpty()) {
            return false;
        }
        CrateRewardsPipelineComponent rewards = context.getComponentOrNull(PipelineComponentKeys.REWARDS);
        return rewards != null && !rewards.getTargetRewards().isEmpty();
    }

    @Override
    public void process(Player player, PipelineContext context, Runnable onComplete) {
        ShowRequest request;
        Optional<Show> show;
        try {
            show = this.bindings.showFor(context.getCrate().idString());
            CrateRewardsPipelineComponent rewards = context.getComponentStrict(PipelineComponentKeys.REWARDS);
            request = this.requests.create(player, context, rewards);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Could not prepare a show; granting the opening without one", exception);
            onComplete.run();
            return;
        }
        if (show.isEmpty()) {
            onComplete.run();
            return;
        }
        this.sessions.play(show.get(), request, onComplete);
    }
}
