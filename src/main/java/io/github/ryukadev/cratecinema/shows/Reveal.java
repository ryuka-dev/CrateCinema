package io.github.ryukadev.cratecinema.shows;

import java.util.Locale;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import io.github.ryukadev.cratecinema.engine.Displays;
import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.ShowContext;

/**
 * The reveal every show ends with: the won reward's name label, the burst of particles and the
 * fanfare, all scaled by tier. Positions are in the plane facing the viewer through a show's
 * centre: {@code x} to their right, {@code y} up.
 */
final class Reveal {

    private static final float LABEL_SCALE = 0.6F;
    /** Height of one text line at scale 1, in blocks. */
    private static final float LINE_HEIGHT = 0.25F;
    private static final float LABEL_GAP = 0.12F;
    private static final float BURST_RADIUS = 1.2F;

    private Reveal() {
    }

    /** Size of the won reward once revealed. */
    static float itemScale(int tier) {
        return 0.9F + 0.1F * tier;
    }

    static Color color(RewardView reward) {
        RarityView rarity = reward.rarity();
        return rarity == null ? Color.WHITE : rarity.color();
    }

    static Particle.DustOptions dust(RewardView reward, int tier) {
        return new Particle.DustOptions(color(reward), 1.0F + 0.3F * tier);
    }

    /** The name label under the revealed item. Spawned invisibly small; call {@link Label#grow()} to show it. */
    static Label label(ShowContext context, Location center, RewardView reward, float itemY, float itemScale) {
        String text = labelText(reward);
        int lines = (int) text.lines().count();
        // Item displays are about one block tall at scale 1. Text grows upward from its anchor, so
        // the anchor goes below the item's bottom edge by the gap plus the text's own height.
        float y = itemY - itemScale / 2.0F - LABEL_GAP - lines * LINE_HEIGHT * LABEL_SCALE;
        TextDisplay display = context.spawn(center, TextDisplay.class, entity -> {
            Displays.setText(entity, text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            // A light backing keeps the text readable over sky and particles.
            entity.setBackgroundColor(Color.fromARGB(96, 0, 0, 0));
            entity.setShadowed(true);
            entity.setTransformation(Displays.at(0, y, 0, 0));
        });
        return new Label(display, y);
    }

    record Label(TextDisplay display, float y) {

        void grow() {
            Displays.animate(this.display, Displays.at(0, this.y, 0, LABEL_SCALE), 5);
        }

        void shrink() {
            Displays.animate(this.display, Displays.at(0, this.y, 0, 0), 6);
        }
    }

    /** A ring of rarity-coloured dust around the item, plus sparks at tier 2 and a totem burst at tier 3. */
    static void burst(ShowContext context, Location center, float itemY, RewardView reward, int tier) {
        Particle.DustOptions dust = dust(reward, tier);
        int points = 24 + 12 * tier;
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2 * index / points;
            Location at = context.inViewPlane(center, Math.cos(angle) * BURST_RADIUS, itemY + Math.sin(angle) * BURST_RADIUS);
            context.particle(Particle.DUST, at, 1, 0.0, 0.0, dust);
        }
        Location itemAt = context.inViewPlane(center, 0, itemY);
        if (tier >= 2) {
            context.particle(Particle.FIREWORK, itemAt, 30 + 15 * tier, 0.1, 0.15);
        }
        if (tier >= 3) {
            context.particle(Particle.TOTEM_OF_UNDYING, itemAt, 60, 0.2, 0.5);
        }
    }

    /** Sparkles above the revealed item, clear of the label below it. Call every few ticks while holding. */
    static void sparkle(ShowContext context, Location center, float itemY, float itemScale, RewardView reward, int tier) {
        if (tier >= 1) {
            context.particle(Particle.DUST, context.inViewPlane(center, 0, itemY + itemScale / 2.0F), 2, 0.25, 0.0, dust(reward, tier));
        }
    }

    static void fanfare(ShowContext context, int tier) {
        switch (tier) {
            case 0 -> {
                context.sound(Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
                context.sound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8F, 1.2F);
            }
            case 1 -> context.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.4F);
            case 2 -> {
                context.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.0F);
                context.sound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
            }
            default -> {
                context.sound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.0F);
                context.sound(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 0.9F);
                context.sound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8F, 1.0F);
            }
        }
    }

    static String labelText(RewardView reward) {
        StringBuilder text = new StringBuilder(reward.name());
        String second = "";
        RarityView rarity = reward.rarity();
        if (rarity != null) {
            second = rarity.name();
        }
        if (!Double.isNaN(reward.chance())) {
            String chance = "§7" + percent(reward.chance());
            second = second.isEmpty() ? chance : second + " §8· " + chance;
        }
        if (!second.isEmpty()) {
            text.append('\n').append(second);
        }
        return text.toString();
    }

    static String percent(double chance) {
        double value = chance * 100.0;
        String pattern = value >= 10 ? "%.0f%%" : value >= 1 ? "%.1f%%" : "%.2f%%";
        return String.format(Locale.ROOT, pattern, value);
    }
}
