package io.github.ryukadev.cratecinema.integration.excellentcrates;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.ShowRequest;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.crate.block.crate.BlockComponent;
import su.nightexpress.excellentcrates.api.crate.component.CrateComponentKeys;
import su.nightexpress.excellentcrates.api.crate.pipeline.reward.PipelineReward;
import su.nightexpress.excellentcrates.api.rarity.Rarity;
import su.nightexpress.excellentcrates.api.rarity.registry.RarityResolver;
import su.nightexpress.excellentcrates.api.rarity.reward.RarityComponent;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.component.RewardComponentKeys;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.bridge.BukkitKeys;
import su.nightexpress.nightcore.util.geodata.pos.BlockPos;
import su.nightexpress.nightcore.util.text.night.NightMessage;

/**
 * Translates one reward of an ExcellentCrates animation session into the engine's show request.
 */
final class ShowRequests {

    private final RewardPreviewAPI preview;
    private final @Nullable RarityResolver rarities;

    ShowRequests(RewardPreviewAPI preview, @Nullable RarityResolver rarities) {
        this.preview = preview;
        this.rarities = rarities;
    }

    /** One reward of an ExcellentCrates animation session, which plays a batch one reward at a time. */
    ShowRequest create(AnimationContext context) {
        ShowRound round = this.round(context.reward(), context.availableRewards());
        return new ShowRequest(context.player(), anchor(context), List.of(round));
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
     * ExcellentCrates passes an animation the crate block's location, or, for an opening without a
     * block, a point in front of the player at eye height. It is the crate block if the crate is
     * linked to the block there.
     */
    private static Location anchor(AnimationContext context) {
        Location location = context.location();
        Block block = location.getBlock();
        BlockComponent linked = context.crate().getComponentOrNull(CrateComponentKeys.BLOCK);
        if (linked != null) {
            BlockPos position = BlockPos.from(block);
            boolean isCrateBlock = linked.getBlockPositions(BukkitKeys.getKey(block.getWorld())).stream()
                .anyMatch(linkedPosition -> BlockPos.from(linkedPosition).equals(position));
            if (isCrateBlock) {
                return above(block);
            }
        }
        return location.clone();
    }

    private static Location above(Block block) {
        double height = block.getBoundingBox().getHeight();
        return block.getLocation().add(0.5, height + 0.5, 0.5);
    }
}
