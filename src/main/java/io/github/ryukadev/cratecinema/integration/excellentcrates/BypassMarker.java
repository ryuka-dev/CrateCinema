package io.github.ryukadev.cratecinema.integration.excellentcrates;

import su.nightexpress.engine.entity.EntityComponentKey;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineComponent;

/**
 * Marks an opening that should be left to ExcellentCrates even though its crate is bound, for
 * example a Bedrock player's. Set by the guard stage, which knows the player; read by the
 * processor, which does not.
 */
final class BypassMarker implements PipelineComponent {

    static final EntityComponentKey<BypassMarker> KEY = EntityComponentKey.of("cratecinema_bypass", BypassMarker.class);

    static final BypassMarker INSTANCE = new BypassMarker();

    private BypassMarker() {
    }
}
