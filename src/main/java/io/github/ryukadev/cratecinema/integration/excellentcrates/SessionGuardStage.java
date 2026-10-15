package io.github.ryukadev.cratecinema.integration.excellentcrates;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.binding.Bindings;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineChain;
import su.nightexpress.excellentcrates.api.crate.pipeline.PipelineStage;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.PipelineComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;

/**
 * Runs in the PRE_ROLL phase, before ExcellentCrates takes the cost:
 * <ul>
 * <li>refuses a new opening while the player is still watching one of our shows, so a refused
 * opening costs nothing;</li>
 * <li>marks openings of bound crates as animated, so ExcellentCrates hides the crate's hologram
 * from the player during the show.</li>
 * </ul>
 */
final class SessionGuardStage implements PipelineStage {

    /** Before ExcellentCrates' own animation stage (500) and long before the cost is taken. */
    static final int ORDER = 450;

    private final Registration registration;
    private final SessionManager sessions;
    private final Bindings bindings;
    private final AdaptedKey markerKey;
    private final String busyMessage;

    SessionGuardStage(Registration registration, SessionManager sessions, Bindings bindings,
                      AdaptedKey markerKey, String busyMessage) {
        this.registration = registration;
        this.sessions = sessions;
        this.bindings = bindings;
        this.markerKey = markerKey;
        this.busyMessage = busyMessage;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        if (!this.registration.isActive()) {
            chain.proceed(player, context);
            return;
        }
        if (this.sessions.isBusy(player.getUniqueId())) {
            if (!this.busyMessage.isEmpty()) {
                player.sendMessage(this.busyMessage);
            }
            chain.abort();
            return;
        }
        if (this.bindings.showFor(crate.idString()).isPresent()
            && !context.hasComponent(PipelineComponentKeys.FAST_OPEN)
            && !context.hasComponent(PipelineComponentKeys.ANIMATION)) {
            context.putComponent(PipelineComponentKeys.ANIMATION, new AnimationMarker(this.markerKey));
        }
        chain.proceed(player, context);
    }
}
