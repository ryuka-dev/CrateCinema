package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.config.PluginSettings;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import io.github.ryukadev.cratecinema.platform.BedrockPlayers;
import io.github.ryukadev.cratecinema.shows.SimpleShow;
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
 * <p>Rewards are rolled and the cost is taken by ExcellentCrates before this runs. Openings it
 * declines (fast openings, bypassed ones, or when the concurrent show limit is reached) go to
 * ExcellentCrates' own processors.
 */
final class CinemaPipelineProcessor implements PipelineProcessor {

    /** Above ExcellentCrates' animation processor (1) and default processor (0). */
    static final int PRIORITY = 10;

    private final Registration registration;
    private final LiveConfig config;
    private final SessionManager sessions;
    private final BedrockPlayers bedrock;
    private final ShowRequests requests;
    private final Logger logger;

    CinemaPipelineProcessor(Registration registration, LiveConfig config, SessionManager sessions,
                            BedrockPlayers bedrock, ShowRequests requests, Logger logger) {
        this.registration = registration;
        this.config = config;
        this.sessions = sessions;
        this.bedrock = bedrock;
        this.requests = requests;
        this.logger = logger;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean shouldHandle(Crate crate, PipelineContext context) {
        if (!this.registration.isActive()
            || context.hasComponent(PipelineComponentKeys.FAST_OPEN)
            || context.hasComponent(BypassMarker.KEY)) {
            return false;
        }
        if (this.config.bindings().showFor(crate.idString()).isEmpty() || !this.sessions.hasCapacity()) {
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
            show = this.showFor(player, context);
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

    /** The bound show, or the text-only one for Bedrock players when configured so. */
    private Optional<Show> showFor(Player player, PipelineContext context) {
        if (this.config.settings().bedrockMode() == PluginSettings.BedrockMode.SIMPLE && this.bedrock.isBedrock(player)) {
            return this.config.catalog().get(SimpleShow.ID);
        }
        return this.config.bindings().showFor(context.getCrate().idString());
    }
}
