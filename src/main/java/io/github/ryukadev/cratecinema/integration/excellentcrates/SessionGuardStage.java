package io.github.ryukadev.cratecinema.integration.excellentcrates;

import org.bukkit.entity.Player;

import io.github.ryukadev.cratecinema.config.LiveConfig;
import io.github.ryukadev.cratecinema.config.PluginSettings;
import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.platform.BedrockPlayers;
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
 * <li>for bound crates, either hands a Bedrock player's opening back to ExcellentCrates (when
 * configured so), or marks the opening as animated so ExcellentCrates hides the crate's hologram
 * from the player during the show.</li>
 * </ul>
 */
final class SessionGuardStage implements PipelineStage {

    /** Before ExcellentCrates' own animation stage (500) and long before the cost is taken. */
    static final int ORDER = 450;

    private final Registration registration;
    private final SessionManager sessions;
    private final LiveConfig config;
    private final BedrockPlayers bedrock;
    private final AdaptedKey markerKey;

    SessionGuardStage(Registration registration, SessionManager sessions, LiveConfig config,
                      BedrockPlayers bedrock, AdaptedKey markerKey) {
        this.registration = registration;
        this.sessions = sessions;
        this.config = config;
        this.bedrock = bedrock;
        this.markerKey = markerKey;
    }

    @Override
    public void intercept(Player player, Crate crate, PipelineContext context, PipelineChain chain) {
        if (!this.registration.isActive()) {
            chain.proceed(player, context);
            return;
        }
        PluginSettings settings = this.config.settings();
        if (this.sessions.isBusy(player.getUniqueId())) {
            if (!settings.busyMessage().isEmpty()) {
                player.sendMessage(settings.busyMessage());
            }
            chain.abort();
            return;
        }
        if (this.config.bindings().showFor(crate.idString()).isPresent()
            && !context.hasComponent(PipelineComponentKeys.FAST_OPEN)) {
            if (settings.bedrockMode() == PluginSettings.BedrockMode.CRATE_PLUGIN && this.bedrock.isBedrock(player)) {
                context.putComponent(BypassMarker.KEY, BypassMarker.INSTANCE);
            } else if (!context.hasComponent(PipelineComponentKeys.ANIMATION)) {
                context.putComponent(PipelineComponentKeys.ANIMATION, new AnimationMarker(this.markerKey));
            }
        }
        chain.proceed(player, context);
    }
}
