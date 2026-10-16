package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.function.Function;
import java.util.logging.Logger;

import io.github.ryukadev.cratecinema.engine.SessionManager;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.platform.BedrockPlayers;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;

/**
 * What every animation profile and instance of ours shares for one ExcellentCrates run.
 *
 * @param fallback  the show played instead of the selected one for Bedrock players and once the
 *                  concurrent show limit is reached
 * @param requests  builds show requests with the preview API ExcellentCrates hands each session
 */
record ShowRuntime(SessionManager sessions, BedrockPlayers bedrock, Show fallback,
                   Function<RewardPreviewAPI, ShowRequests> requests, InterruptedOpenings interrupted, Logger logger) {
}
