package io.github.ryukadev.cratecinema.shows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import io.github.ryukadev.cratecinema.engine.Displays;
import io.github.ryukadev.cratecinema.engine.RarityView;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowContext;
import io.github.ryukadev.cratecinema.engine.ShowInstance;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Timeline;

/**
 * Orbit: rewards from the pool circle above the crate in a ring facing the viewer, spin up and
 * slow down, then wink out one by one until only the won reward is left. It gathers light in its
 * rarity colour, bursts, and its name appears beneath it.
 *
 * <p>Higher tiers spin longer, charge longer, glow, and end with a bigger burst and fanfare.
 */
public final class OrbitShow implements Show {

    public static final String ID = "orbit";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        return new Instance(context, round);
    }

    private static final class Instance implements ShowInstance {

        private static final float RADIUS = 1.1F;
        private static final float RING_SCALE = 0.45F;
        private static final float HERO_SCALE = 0.6F;
        private static final float HERO_RISE = 0.25F;
        private static final int MIN_RING = 6;
        private static final int MAX_RING = 8;
        /** Ticks between ring keyframes; the client interpolates in between. */
        private static final int KEYFRAME = 2;

        private static final int[] SPIN_TICKS = {40, 60, 80, 100};
        private static final double[] SPIN_TURNS = {1.5, 2.0, 2.5, 3.0};
        private static final int[] COLLAPSE_GAP = {3, 3, 3, 3};
        private static final int[] CHARGE_TICKS = {8, 16, 28, 40};
        private static final int[] HOLD_TICKS = {25, 35, 45, 55};

        private final ShowContext context;
        private final RewardView target;
        private final int tier;
        private final Color color;
        private final Location center;
        private final List<ItemDisplay> ring = new ArrayList<>();
        private final double[] baseAngles;
        private final int heroIndex;
        private final Timeline timeline = new Timeline();
        private final TextDisplay label;

        Instance(ShowContext context, ShowRound round) {
            this.context = context;
            this.target = round.target();
            this.tier = context.tierOf(this.target);
            RarityView rarity = this.target.rarity();
            this.color = rarity == null ? Color.WHITE : rarity.color();
            this.center = context.anchor().add(0, 0.9, 0);

            List<RewardView> members = ringMembers(round);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            this.heroIndex = random.nextInt(members.size());
            members.add(this.heroIndex, this.target);
            this.baseAngles = new double[members.size()];
            double offset = random.nextDouble(Math.PI * 2);
            for (int index = 0; index < members.size(); index++) {
                this.baseAngles[index] = offset + Math.PI * 2 * index / members.size();
                RewardView member = members.get(index);
                this.ring.add(context.spawn(this.center, ItemDisplay.class, display -> {
                    display.setItemStack(member.icon());
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setTransformation(Displays.at(0, 0, 0, 0));
                }));
            }

            this.label = this.spawnLabel();
            this.script();
        }

        /** Rewards other than the won one, shuffled, sized for the ring. Repeats if the pool is small. */
        private List<RewardView> ringMembers(ShowRound round) {
            List<RewardView> others = new ArrayList<>();
            for (RewardView filler : round.fillers()) {
                if (!filler.icon().isSimilar(this.target.icon())) {
                    others.add(filler);
                }
            }
            if (others.isEmpty()) {
                others.add(this.target);
            }
            Collections.shuffle(others);
            int size = Math.max(MIN_RING, Math.min(MAX_RING, others.size() + 1)) - 1;
            List<RewardView> members = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                members.add(others.get(index % others.size()));
            }
            return members;
        }

        private void script() {
            int spinStart = 12;
            int spinTicks = SPIN_TICKS[this.tier];
            double spinAngle = Math.PI * 2 * SPIN_TURNS[this.tier];
            int collapseStart = spinStart + spinTicks + 2;
            int gap = COLLAPSE_GAP[this.tier];
            int others = this.ring.size() - 1;
            int gatherAt = collapseStart + others * gap + 2;
            int chargeStart = gatherAt + 8;
            int chargeTicks = CHARGE_TICKS[this.tier];
            int revealAt = chargeStart + chargeTicks;
            int holdEnd = revealAt + 10 + HOLD_TICKS[this.tier];

            // Open: the ring unfolds from the centre.
            this.timeline.at(1, () -> {
                for (int index = 0; index < this.ring.size(); index++) {
                    Displays.animate(this.ring.get(index), this.onRing(this.baseAngles[index], RING_SCALE), 10);
                }
                this.context.sound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
                if (this.tier >= 2) {
                    this.context.sound(Sound.BLOCK_BEACON_ACTIVATE, 0.6F, 1.6F);
                }
            });

            // Spin: ease in and out; keyframes every KEYFRAME ticks.
            this.timeline.during(spinStart, spinStart + spinTicks, step -> {
                if (step % KEYFRAME == 0) {
                    float progress = (float) (step + KEYFRAME) / spinTicks;
                    double angle = spinAngle * Displays.easeInOutSine(progress);
                    for (int index = 0; index < this.ring.size(); index++) {
                        Displays.animate(this.ring.get(index), this.onRing(this.baseAngles[index] + angle, RING_SCALE), KEYFRAME);
                    }
                }
                if (step % 4 == 0) {
                    float speed = (float) Math.sin(Math.PI * step / spinTicks);
                    this.context.sound(Sound.BLOCK_NOTE_BLOCK_HAT, 0.35F, 0.6F + 1.2F * speed);
                }
                if (this.tier >= 1 && step % 3 == 0) {
                    double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                    this.context.particle(Particle.END_ROD, this.world(Math.cos(angle) * RADIUS, Math.sin(angle) * RADIUS), 1, 0.0, 0.0);
                }
            });

            // Collapse: everything but the hero winks out, the notes climbing.
            int popped = 0;
            for (int index = 0; index < this.ring.size(); index++) {
                if (index == this.heroIndex) {
                    continue;
                }
                ItemDisplay item = this.ring.get(index);
                double angle = this.baseAngles[index] + spinAngle;
                float pitch = 0.7F + 0.8F * popped / Math.max(1, others - 1);
                this.timeline.at(collapseStart + popped * gap, () -> {
                    Displays.animate(item, Displays.at(0, 0, 0, 0), 6);
                    this.context.sound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, pitch);
                    this.context.particle(Particle.END_ROD, this.world(Math.cos(angle) * RADIUS, Math.sin(angle) * RADIUS), 3, 0.05, 0.02);
                });
                popped++;
            }

            ItemDisplay hero = this.ring.get(this.heroIndex);
            this.timeline.at(gatherAt, () -> Displays.animate(hero, Displays.at(0, 0, 0, HERO_SCALE), 8));

            // Charge: light gathers in the rarity colour; higher tiers glow and throb.
            this.timeline.at(chargeStart, () -> {
                if (this.tier >= 1) {
                    hero.setGlowing(true);
                    hero.setGlowColorOverride(this.color);
                }
                if (this.tier >= 2) {
                    this.context.sound(Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.2F);
                }
                if (this.tier >= 3) {
                    this.context.sound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, 0.8F);
                }
            });
            Particle.DustOptions dust = new Particle.DustOptions(this.color, 1.0F + 0.3F * this.tier);
            this.timeline.during(chargeStart, revealAt, step -> {
                if (step % 2 == 0) {
                    double radius = 1.2 * (1.0 - (double) step / chargeTicks);
                    for (int arm = 0; arm < 2; arm++) {
                        double angle = step * 0.5 + Math.PI * arm;
                        this.context.particle(Particle.DUST, this.world(Math.cos(angle) * radius, Math.sin(angle) * radius), 1, 0.0, 0.0, dust);
                    }
                }
                if (this.tier >= 1 && step % 8 == 0) {
                    Displays.animate(hero, Displays.at(0, 0, 0, HERO_SCALE + 0.08F), 4);
                    if (this.tier >= 2) {
                        this.context.sound(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.8F, 0.7F);
                    }
                } else if (this.tier >= 1 && step % 8 == 4) {
                    Displays.animate(hero, Displays.at(0, 0, 0, HERO_SCALE), 4);
                }
            });

            // Reveal: pop, burst, fanfare, name.
            float revealScale = 0.9F + 0.1F * this.tier;
            this.timeline.at(revealAt, () -> {
                Displays.animate(hero, Displays.at(0, HERO_RISE, 0, revealScale * 1.25F), 4);
                this.burst(dust);
                this.fanfare();
            });
            this.timeline.at(revealAt + 4, () -> Displays.animate(hero, Displays.at(0, HERO_RISE, 0, revealScale), 6));
            this.timeline.at(revealAt + 2, () -> Displays.animate(this.label, Displays.at(0, -0.55F, 0, 1.0F), 5));

            // Hold: bob gently, sparkle.
            this.timeline.during(revealAt + 10, holdEnd, step -> {
                if (step % 20 == 0) {
                    float bob = (step / 20) % 2 == 0 ? 0.06F : -0.02F;
                    Displays.animate(hero, Displays.at(0, HERO_RISE + bob, 0, revealScale), 20);
                }
                if (this.tier >= 1 && step % 5 == 0) {
                    this.context.particle(Particle.DUST, this.world(0, HERO_RISE), 2, 0.35, 0.0, dust);
                }
            });

            // Out.
            this.timeline.at(holdEnd, () -> {
                Displays.animate(hero, Displays.at(0, HERO_RISE, 0, 0), 6);
                Displays.animate(this.label, Displays.at(0, -0.55F, 0, 0), 6);
            });
            this.timeline.endAt(holdEnd + 6);
        }

        private void burst(Particle.DustOptions dust) {
            int points = 24 + 12 * this.tier;
            for (int index = 0; index < points; index++) {
                double angle = Math.PI * 2 * index / points;
                this.context.particle(Particle.DUST, this.world(Math.cos(angle) * 0.9, HERO_RISE + Math.sin(angle) * 0.9), 1, 0.0, 0.0, dust);
            }
            Location heroAt = this.world(0, HERO_RISE);
            if (this.tier >= 2) {
                this.context.particle(Particle.FIREWORK, heroAt, 30 + 15 * this.tier, 0.1, 0.15);
            }
            if (this.tier >= 3) {
                this.context.particle(Particle.TOTEM_OF_UNDYING, heroAt, 60, 0.2, 0.5);
            }
        }

        private void fanfare() {
            switch (this.tier) {
                case 0 -> {
                    this.context.sound(Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
                    this.context.sound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8F, 1.2F);
                }
                case 1 -> this.context.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.4F);
                case 2 -> {
                    this.context.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.0F);
                    this.context.sound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
                }
                default -> {
                    this.context.sound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.0F);
                    this.context.sound(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 0.9F);
                    this.context.sound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8F, 1.0F);
                }
            }
        }

        /** Spawns the label invisibly small; it grows in at the reveal. */
        private TextDisplay spawnLabel() {
            String text = labelText(this.target);
            return this.context.spawn(this.center, TextDisplay.class, display -> {
                Displays.setText(display, text);
                display.setBillboard(Display.Billboard.CENTER);
                display.setAlignment(TextDisplay.TextAlignment.CENTER);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                display.setShadowed(true);
                display.setTransformation(Displays.at(0, -0.55F, 0, 0));
            });
        }

        /** Ring position {@code angle} radians round, in the plane facing the viewer. */
        private org.bukkit.util.Transformation onRing(double angle, float scale) {
            return Displays.at((float) (Math.cos(angle) * RADIUS), (float) (Math.sin(angle) * RADIUS), 0, scale);
        }

        /** World position of a point in the ring's plane, for particles. */
        private Location world(double x, double y) {
            return this.context.inViewPlane(this.center, x, y);
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

    static String labelText(RewardView reward) {
        StringBuilder text = new StringBuilder(reward.name());
        String second = "";
        RarityView rarity = reward.rarity();
        if (rarity != null) {
            second = rarity.name();
        }
        if (!Double.isNaN(reward.chance())) {
            second = second.isEmpty() ? "§7" + percent(reward.chance()) : second + " §8· §7" + percent(reward.chance());
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
