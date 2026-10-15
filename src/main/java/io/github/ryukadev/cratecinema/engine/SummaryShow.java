package io.github.ryukadev.cratecinema.engine;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;

/**
 * Closes a batch opening: every reward of the batch pops up in rows facing the viewer, the ones
 * above tier 0 glowing in their rarity colour.
 */
final class SummaryShow implements ShowInstance {

    private static final int PER_ROW = 7;
    private static final float SPACING = 0.55F;
    private static final float SCALE = 0.4F;
    private static final int POP_INTERVAL = 2;
    private static final int POP_TICKS = 4;
    private static final int HOLD_TICKS = 40;
    private static final int OUTRO_TICKS = 6;

    private final List<ItemDisplay> items = new ArrayList<>();
    private final Timeline timeline = new Timeline();

    SummaryShow(ShowContext context, List<RewardView> rewards) {
        int rows = (rewards.size() + PER_ROW - 1) / PER_ROW;
        Location center = context.anchor().add(0, 1.0 + (rows - 1) * SPACING / 2.0, 0);

        for (int index = 0; index < rewards.size(); index++) {
            RewardView reward = rewards.get(index);
            int row = index / PER_ROW;
            int inRow = Math.min(PER_ROW, rewards.size() - row * PER_ROW);
            double x = ((index % PER_ROW) - (inRow - 1) / 2.0) * SPACING;
            double y = -row * SPACING;
            Location at = context.inViewPlane(center, x, y);
            int tier = context.tierOf(reward);

            ItemDisplay item = context.spawn(at, ItemDisplay.class, display -> {
                display.setItemStack(reward.icon());
                display.setBillboard(Display.Billboard.CENTER);
                display.setTransformation(Displays.at(0, 0, 0, 0));
                if (tier > 0 && reward.rarity() != null) {
                    display.setGlowing(true);
                    display.setGlowColorOverride(reward.rarity().color());
                }
            });
            this.items.add(item);

            int popAt = 1 + index * POP_INTERVAL;
            float pitch = 0.8F + 0.8F * index / Math.max(1, rewards.size() - 1);
            this.timeline.at(popAt, () -> {
                Displays.animate(item, Displays.at(0, 0, 0, SCALE), POP_TICKS);
                context.sound(Sound.BLOCK_NOTE_BLOCK_BELL, 0.6F, pitch);
            });
        }

        int outroAt = 1 + rewards.size() * POP_INTERVAL + POP_TICKS + HOLD_TICKS;
        this.timeline.at(outroAt, () -> this.items.forEach(item -> Displays.animate(item, Displays.at(0, 0, 0, 0), OUTRO_TICKS)));
        this.timeline.endAt(outroAt + OUTRO_TICKS);
    }

    @Override
    public boolean tick() {
        return this.timeline.tick();
    }

    @Override
    public void cleanup() {
        // Entities are removed by the context.
    }
}
