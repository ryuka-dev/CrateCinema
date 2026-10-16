package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateRewardsPipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.CrateSourcePipelineComponent;
import su.nightexpress.excellentcrates.api.crate.pipeline.component.PipelineComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.context.PipelineContext;
import su.nightexpress.excellentcrates.api.crate.pipeline.reward.PipelineReward;
import su.nightexpress.excellentcrates.api.rarity.Rarity;
import su.nightexpress.excellentcrates.api.rarity.registry.RarityResolver;
import su.nightexpress.excellentcrates.api.rarity.reward.RarityComponent;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.component.RewardComponentKeys;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.util.text.night.NightMessage;

/**
 * Translates an ExcellentCrates opening (a pipeline, or an animation session) into the engine's
 * show request.
 */
final class ShowRequests {

    private final RewardPreviewAPI preview;
    private final @Nullable RarityResolver rarities;

    ShowRequests(RewardPreviewAPI preview, @Nullable RarityResolver rarities) {
        this.preview = preview;
        this.rarities = rarities;
    }

    ShowRequest create(Player player, PipelineContext context, CrateRewardsPipelineComponent rewards) {
        List<PipelineReward> targets = rewards.getTargetRewards();
        List<List<Reward>> fillers = rewards.getFillerRewards();

        List<ShowRound> rounds = new ArrayList<>(targets.size());
        for (int index = 0; index < targets.size(); index++) {
            // Each target was rolled from the pool recorded as its fillers; selectable-reward
            // crates record none.
            List<Reward> pool = index < fillers.size() ? fillers.get(index) : List.of();
            rounds.add(this.round(targets.get(index), pool));
        }
        return new ShowRequest(player, anchor(player, context), rounds);
    }

    /** One reward of an ExcellentCrates animation session, which plays a batch one reward at a time. */
    ShowRequest create(AnimationContext context) {
        ShowRound round = this.round(context.reward(), context.availableRewards());
        return new ShowRequest(context.player(), anchor(context.location()), List.of(round));
    }

    private ShowRound round(PipelineReward target, List<Reward> pool) {
        double totalWeight = pool.stream().mapToDouble(Reward::getWeight).sum();
        // Shows display what will actually be granted, which reward limits may have changed.
        RewardView targetView = this.view(target.getGranted(), totalWeight);
        List<RewardView> fillers = pool.stream().map(reward -> this.view(reward, totalWeight)).toList();
        return new ShowRound(targetView, fillers);
    }

    private RewardView view(Reward reward, double poolWeight) {
        String name = NightMessage.asLegacy(this.preview.getDisplayInfo(reward).name());
        double chance = poolWeight > 0 && reward.getWeight() > 0 ? reward.getWeight() / poolWeight : Double.NaN;
        return new RewardView(this.preview.createPreviewItem(reward), name, this.rarity(reward), chance);
    }

    private @Nullable RarityView rarity(Reward reward) {
        RarityComponent component = reward.getComponentOrNull(RewardComponentKeys.RARITY);
        if (this.rarities == null || component == null || !component.isEnabled()) {
            return null;
        }
        Rarity rarity = this.rarities.resolveRarity(component.getRarityId());
        if (rarity == null) {
            return null;
        }
        return new RarityView(rarity.idString(), NightMessage.asLegacy(rarity.getName()), rarity.getColor());
    }

    /**
     * Above the crate block when opened from one, otherwise in front of the player at eye height.
     * Mirrors where ExcellentCrates places its own animations.
     */
    private static Location anchor(Player player, PipelineContext context) {
        CrateSourcePipelineComponent source = context.getComponentOrNull(PipelineComponentKeys.CRATE_SOURCE);
        Location sourceLocation = source == null ? null : source.getLocation();
        if (sourceLocation != null) {
            return above(sourceLocation.getBlock());
        }

        Block front = player.getLocation().getBlock().getRelative(player.getFacing());
        Location anchor = front.getLocation().add(0.5, 0.0, 0.5);
        anchor.setY(player.getEyeLocation().getY());
        return anchor;
    }

    /**
     * ExcellentCrates passes an animation the crate block's location, or, for an opening without a
     * block, a point in front of the player at eye height.
     */
    // TODO: tell the two apart by the crate's linked block positions instead of by the block there.
    private static Location anchor(Location location) {
        Block block = location.getBlock();
        return block.isPassable() ? location.clone() : above(block);
    }

    private static Location above(Block block) {
        double height = block.getBoundingBox().getHeight();
        return block.getLocation().add(0.5, height + 0.5, 0.5);
    }
}
