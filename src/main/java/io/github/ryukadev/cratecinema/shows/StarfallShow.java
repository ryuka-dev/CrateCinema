package io.github.ryukadev.cratecinema.shows;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import io.github.ryukadev.cratecinema.config.SettingsFile;
import io.github.ryukadev.cratecinema.engine.Displays;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowContext;
import io.github.ryukadev.cratecinema.engine.ShowInstance;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Timeline;

/**
 * Starfall: a burning meteor streaks down from behind the crate, glowing in the reward's rarity
 * colour, and strikes it. A shockwave rolls out and the reward rises from the impact.
 *
 * <p>Higher tiers send smaller decoy meteors first (configurable), fall slower, and hit harder.
 */
public final class StarfallShow implements Show {

    public static final String ID = "starfall";

    /** Settings from {@code shows/starfall.yml}; arrays hold one value per tier. */
    public record Settings(Material meteorBlock, int[] fallTicks, int[] decoys, int[] holdTicks) {

        public static Settings read(SettingsFile config) {
            return new Settings(
                config.material("meteor-block", Material::isBlock, "a block, for example MAGMA_BLOCK"),
                config.tierInts("fall-ticks", 8, 40),
                config.tierInts("decoys", 0, 5),
                config.tierInts("hold-ticks", 10, 200));
        }
    }

    private final Settings settings;

    public StarfallShow(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        return new Instance(context, round, this.settings);
    }

    private static final class Instance implements ShowInstance {

        /** Where meteors come from, relative to where they land: up, to the viewer's left, behind. */
        private static final double FROM_UP = 7.0;
        private static final double FROM_LEFT = 4.5;
        private static final double FROM_BEHIND = 2.5;
        private static final float RISE = 0.9F;

        private static final int DECOY_FALL_TICKS = 12;
        private static final int DECOY_GAP = 12;

        private final ShowContext context;
        private final Settings settings;
        private final RewardView target;
        private final int tier;
        private final float revealScale;
        private final Location impact;
        /** Centre of the revealed reward, above the impact point. */
        private final Location top;
        private final ItemDisplay hero;
        private final Reveal.Label label;
        private final Timeline timeline = new Timeline();

        Instance(ShowContext context, ShowRound round, Settings settings) {
            this.context = context;
            this.settings = settings;
            this.target = round.target();
            this.tier = context.tierOf(this.target);
            this.revealScale = Reveal.itemScale(this.tier);
            this.impact = context.anchor();
            this.top = this.impact.clone().add(0, RISE, 0);

            this.hero = context.spawn(this.top, ItemDisplay.class, display -> {
                display.setItemStack(this.target.icon());
                display.setBillboard(Display.Billboard.CENTER);
                display.setTransformation(Displays.at(0, -RISE, 0, 0));
            });
            this.label = Reveal.label(context, this.top, this.target, 0, this.revealScale);

            this.script();
        }

        private void script() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Vector right = this.context.right();
            Vector behind = this.context.towardViewer().multiply(-1);
            double groundY = this.impact.getY() - 1.3;

            // Decoys land on the ground around the crate before the real one.
            int decoys = this.settings.decoys()[this.tier];
            for (int decoy = 0; decoy < decoys; decoy++) {
                Location landing = this.impact.clone()
                    .add(right.clone().multiply(random.nextDouble(-2.5, 2.5)))
                    .add(behind.clone().multiply(random.nextDouble(-0.5, 1.5)));
                landing.setY(groundY);
                this.meteor(4 + decoy * DECOY_GAP, landing, DECOY_FALL_TICKS, 0.35F, null, true);
            }

            int launch = 14 + decoys * DECOY_GAP;
            int fallTicks = this.settings.fallTicks()[this.tier];
            int impactAt = launch + 1 + fallTicks;
            int riseTicks = 10;
            int revealAt = impactAt + 4 + riseTicks;
            int holdEnd = revealAt + 8 + this.settings.holdTicks()[this.tier];

            // A glint and a rumble before the real one.
            this.timeline.at(launch - 6, () -> {
                this.context.sound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8F, 0.5F);
                this.context.particle(Particle.END_ROD, this.origin(this.impact), 8, 0.2, 0.02);
            });
            @Nullable Color glow = this.tier >= 1 ? Reveal.color(this.target) : null;
            this.meteor(launch, this.impact, fallTicks, 0.6F + 0.1F * this.tier, glow, false);

            this.timeline.at(impactAt, this::strike);
            this.timeline.during(impactAt, impactAt + 10, this::shockwave);

            // The reward rises out of the impact.
            this.timeline.at(impactAt + 4, () -> {
                if (this.tier >= 1) {
                    this.hero.setGlowing(true);
                    this.hero.setGlowColorOverride(Reveal.color(this.target));
                }
                Displays.animate(this.hero, Displays.at(0, 0, 0, this.revealScale * 1.25F), riseTicks);
            });
            this.timeline.during(impactAt + 4, revealAt, step -> {
                if (step % 2 == 0) {
                    this.context.particle(Particle.FLAME, this.impact, 3, 0.2, 0.02);
                }
            });
            this.timeline.at(revealAt, () -> {
                Displays.animate(this.hero, Displays.at(0, 0, 0, this.revealScale), 6);
                Reveal.burst(this.context, this.top, 0, this.target, this.tier);
                Reveal.fanfare(this.context, this.tier);
            });
            this.timeline.at(revealAt + 2, this.label::grow);

            this.timeline.during(revealAt + 8, holdEnd, step -> {
                if (step % 20 == 0) {
                    float bob = (step / 20) % 2 == 0 ? 0.06F : -0.02F;
                    Displays.animate(this.hero, Displays.at(0, bob, 0, this.revealScale), 20);
                }
                if (step % 5 == 0) {
                    Reveal.sparkle(this.context, this.top, 0, this.revealScale, this.target, this.tier);
                }
                if (step % 6 == 0) {
                    this.context.particle(Particle.SMOKE, this.impact, 1, 0.3, 0.01);
                }
            });

            this.timeline.at(holdEnd, () -> {
                Displays.animate(this.hero, Displays.at(0, 0, 0, 0), 6);
                this.label.shrink();
            });
            this.timeline.endAt(holdEnd + 6);
        }

        /**
         * Schedules a meteor that appears at {@code launch} and lands on {@code landing} after
         * {@code fallTicks}. The client interpolates the fall; the trail is drawn along the same line.
         * A decoy bursts on landing; the real meteor vanishes into the strike.
         */
        private void meteor(int launch, Location landing, int fallTicks, float size, @Nullable Color glow, boolean decoy) {
            Location from = this.origin(landing);
            Vector path = landing.toVector().subtract(from.toVector());
            BlockDisplay[] rock = new BlockDisplay[1];

            this.timeline.at(launch, () -> rock[0] = this.context.spawn(from, BlockDisplay.class, display -> {
                display.setBlock(this.settings.meteorBlock().createBlockData());
                display.setBrightness(new Display.Brightness(15, 15));
                display.setTransformation(spun(size, 0));
                // Must reach the client with the spawn: position updates are sent before metadata,
                // so a duration set alongside the teleport would arrive too late and the rock would snap.
                display.setTeleportDuration(fallTicks);
                if (glow != null) {
                    display.setGlowing(true);
                    display.setGlowColorOverride(glow);
                }
            }));
            this.timeline.at(launch + 1, () -> rock[0].teleport(landing));
            this.timeline.during(launch + 1, launch + 1 + fallTicks, step -> {
                Location at = from.clone().add(path.clone().multiply((double) step / fallTicks));
                this.context.particle(Particle.FLAME, at, decoy ? 2 : 4, 0.12, 0.01);
                this.context.particle(Particle.LARGE_SMOKE, at, decoy ? 1 : 2, 0.1, 0.01);
                if (glow != null) {
                    this.context.particle(Particle.DUST, at, 2, 0.15, 0.0, new Particle.DustOptions(glow, 1.5F));
                }
                if (step % 3 == 0) {
                    Displays.animate(rock[0], spun(size, (float) Math.toRadians(40) * (step / 3 + 1)), 3);
                }
            });
            if (decoy) {
                this.timeline.at(launch + 1 + fallTicks, () -> {
                    this.context.remove(rock[0]);
                    this.context.particle(Particle.POOF, landing, 10, 0.2, 0.05);
                    this.context.particle(Particle.LAVA, landing, 3, 0.1, 0.0);
                    this.context.sound(Sound.ENTITY_GENERIC_EXPLODE, 0.35F, 1.5F);
                });
            } else {
                this.timeline.at(launch + 1 + fallTicks, () -> this.context.remove(rock[0]));
            }
        }

        private void strike() {
            this.context.particle(Particle.EXPLOSION, this.impact, 1, 0.0, 0.0);
            this.context.particle(Particle.FLAME, this.impact, 20 + 10 * this.tier, 0.2, 0.1);
            this.context.particle(Particle.LAVA, this.impact, 4 + 2 * this.tier, 0.2, 0.0);
            this.context.sound(Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 0.9F);
            if (this.tier >= 2) {
                this.context.sound(Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6F, 1.0F);
            }
            if (this.tier >= 3) {
                this.context.sound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8F, 1.0F);
            }
        }

        /** A ring that rolls outward across the ground from the impact. */
        private void shockwave(int step) {
            double radius = 0.3 + step * (0.3 + 0.05 * this.tier);
            int points = 16 + 8 * this.tier;
            double y = this.impact.getY() - 0.3;
            Particle.DustOptions dust = Reveal.dust(this.target, this.tier);
            for (int index = 0; index < points; index++) {
                double angle = Math.PI * 2 * index / points;
                Location at = this.impact.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                at.setY(y);
                this.context.particle(Particle.CLOUD, at, 1, 0.0, 0.0);
                if (this.tier >= 1) {
                    this.context.particle(Particle.DUST, at, 1, 0.0, 0.0, dust);
                }
            }
        }

        /** Where a meteor landing at {@code landing} starts. */
        private Location origin(Location landing) {
            return landing.clone()
                .add(0, FROM_UP, 0)
                .add(this.context.right().multiply(-FROM_LEFT))
                .add(this.context.towardViewer().multiply(-FROM_BEHIND));
        }

        /** A cube of {@code size} centred on the entity, turned {@code angle} about a tilted axis. */
        private static Transformation spun(float size, float angle) {
            Quaternionf rotation = new Quaternionf().rotateAxis(angle, new Vector3f(1, 1, 0).normalize());
            Vector3f half = new Vector3f(size / 2, size / 2, size / 2);
            Vector3f offset = rotation.transform(new Vector3f(half)).negate();
            return new Transformation(offset, rotation, new Vector3f(size, size, size), new Quaternionf());
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
}
