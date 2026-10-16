package io.github.ryukadev.cratecinema.integration.excellentcrates;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineChain;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineStage;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;

/**
 * Runs in the PRE_ROLL phase, before ExcellentCrates takes the cost, and refuses a new opening
 * while the player is still watching a show or waiting for a reward held back after a death, so a
 * refused opening costs nothing. ExcellentCrates refuses openings during its own animation
 * sessions too, but does not know about previews.
 */
final class SessionGuardStage implements PipelineStage {

    /** Before ExcellentCrates' own animation stage (500) and long before the cost is taken. */
    static final int ORDER = 450;

    private final Registration registration;
    private final SessionManager sessions;
    private final LiveConfig config;

    SessionGuardStage(Registration registration, SessionManager sessions, LiveConfig config) {
        this.registration = registration;
        this.sessions = sessions;
        this.config = config;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        if (this.registration.isActive() && this.sessions.isBusy(player.getUniqueId())) {
            String message = this.config.settings().busyMessage();
            if (!message.isEmpty()) {
                player.sendMessage(message);
            }
            chain.abort();
            return;
        }
        chain.proceed(player, context);
    }
}
