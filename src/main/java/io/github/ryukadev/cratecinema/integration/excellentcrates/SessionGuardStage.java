package io.github.ryukadev.cratecinema.integration.excellentcrates;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineChain;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineStage;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;

/**
 * Refuses a new opening while the player is still watching one of our shows. Runs in the
 * PRE_ROLL phase, before ExcellentCrates takes the cost, so a refused opening costs nothing.
 */
final class SessionGuardStage implements PipelineStage {

    /** Before ExcellentCrates' own animation stage (500) and long before the cost is taken. */
    static final int ORDER = 450;

    private final Registration registration;
    private final SessionManager sessions;
    private final String busyMessage;

    SessionGuardStage(Registration registration, SessionManager sessions, String busyMessage) {
        this.registration = registration;
        this.sessions = sessions;
        this.busyMessage = busyMessage;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        if (this.registration.isActive() && this.sessions.isBusy(player.getUniqueId())) {
            if (!this.busyMessage.isEmpty()) {
                player.sendMessage(this.busyMessage);
            }
            chain.abort();
            return;
        }
        chain.proceed(player, context);
    }
}
