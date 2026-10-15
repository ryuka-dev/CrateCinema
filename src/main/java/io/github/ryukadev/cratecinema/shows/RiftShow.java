package io.github.ryukadev.cratecinema.shows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.ryukadev.cratecinema.engine.Displays;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowContext;
import io.github.ryukadev.cratecinema.engine.ShowInstance;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Timeline;

/**
 * Rift: a portal tear opens above the crate. Rewards from the pool drift around it and are pulled
 * in one by one; the tear throbs in the reward's rarity colour, pushes the reward out towards the
 * viewer, and seals.
 *
 * <p>Higher tiers open a wider, taller tear, charge longer, and end bigger.
 */
public final class RiftShow implements Show {

    public static final String ID = "rift";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        return new Instance(context, round);
    }

    private static final class Instance implements ShowInstance {

        private static final Color RIFT_GLOW = Color.fromRGB(170, 60, 255);
        /** The portal block's own model is a thin sheet, so this mostly sets the sheet's thickness. */
        private static final float DEPTH = 0.2F;
        private static final int PUSH_TICKS = 6;
        private static final float DEBRIS_SCALE = 0.35F;
        private static final int MAX_DEBRIS = 8;
        /** How far towards the viewer the reward is pushed out. */
        private static final double PUSH = 0.7;

        private static final int[] SUCK_GAP = {4, 4, 3, 3};
        private static final int[] CHARGE_TICKS = {6, 14, 24, 36};
        private static final int[] HOLD_TICKS = {25, 35, 45, 55};

        private final ShowContext context;
        private final RewardView target;
        private final int tier;
        private final float revealScale;
        private final float height;
        private final float width;
        private final Location center;
        /** Where the reward ends up, in front of the tear. */
        private final Location front;
        private final BlockDisplay rift;
        private final List<ItemDisplay> debris = new ArrayList<>();
        private final ItemDisplay hero;
        private final Reveal.Label label;
        private final Timeline timeline = new Timeline();

        Instance(ShowContext context, ShowRound round) {
            this.context = context;
            this.target = round.target();
            this.tier = context.tierOf(this.target);
            this.revealScale = Reveal.itemScale(this.tier);
            this.height = 1.6F + 0.2F * this.tier;
            this.width = 0.35F + 0.08F * this.tier;
            this.center = context.anchor().add(0, 1.0, 0);
            this.front = this.center.clone().add(context.towardViewer().multiply(PUSH));

            this.rift = context.spawn(this.center, BlockDisplay.class, display -> {
                // Axis x: the portal sheet lies across the x-y plane, which the billboard turns to face the viewer.
                display.setBlock(Material.NETHER_PORTAL.createBlockData("[axis=x]"));
                display.setBillboard(Display.Billboard.VERTICAL);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setGlowing(true);
                display.setGlowColorOverride(RIFT_GLOW);
                display.setTransformation(slit(0, 0.1F));
            });

            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<RewardView> pool = new ArrayList<>(round.fillers());
            Collections.shuffle(pool);
            for (RewardView member : pool.subList(0, Math.min(MAX_DEBRIS, pool.size()))) {
                double angle = random.nextDouble(Math.PI * 2);
                double distance = random.nextDouble(1.4, 2.1);
                float x = (float) (Math.cos(angle) * distance);
                float y = (float) (Math.sin(angle) * distance * 0.7);
                this.debris.add(context.spawn(this.center, ItemDisplay.class, display -> {
                    display.setItemStack(member.icon());
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setTransformation(Displays.at(x, y, 0, 0));
                }));
            }

            this.hero = context.spawn(this.center, ItemDisplay.class, display -> {
                display.setItemStack(this.target.icon());
                display.setBillboard(Display.Billboard.CENTER);
                // Sent with the spawn, so the later push out is interpolated rather than a snap.
                display.setTeleportDuration(PUSH_TICKS);
                display.setTransformation(Displays.at(0, 0, 0, 0));
            });
            this.label = Reveal.label(context, this.front, this.target, 0, this.revealScale);

            this.script();
        }

        private void script() {
            int crackAt = 1;
            int openAt = crackAt + 10;
            int suckStart = openAt + 12;
            int gap = SUCK_GAP[this.tier];
            int chargeStart = suckStart + this.debris.size() * gap + 6;
            int chargeTicks = CHARGE_TICKS[this.tier];
            int revealAt = chargeStart + chargeTicks;
            int holdEnd = revealAt + 10 + HOLD_TICKS[this.tier];

            // Crack: a hairline tear runs top to bottom.
            this.timeline.at(crackAt, () -> {
                Displays.animate(this.rift, slit(0.03F, this.height), 8);
                this.context.sound(Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 0.5F);
                this.context.sound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6F, 0.6F);
            });
            // Open, and the pool's rewards appear around it.
            this.timeline.at(openAt, () -> {
                Displays.animate(this.rift, slit(this.width, this.height), 10);
                this.context.sound(Sound.BLOCK_PORTAL_TRIGGER, 0.3F, 1.4F);
                for (ItemDisplay piece : this.debris) {
                    Transformation now = piece.getTransformation();
                    Displays.animate(piece, Displays.at(now.getTranslation().x, now.getTranslation().y, 0, DEBRIS_SCALE), 6);
                }
            });
            // Portal particles drift in towards the tear the whole time it is open.
            this.timeline.during(openAt, revealAt, step -> {
                if (step % 2 == 0) {
                    this.context.particle(Particle.PORTAL, this.center, 6 + 2 * this.tier, 0.6, 0.6);
                }
            });

            // Pull: the pool's rewards are drawn into the tear one by one.
            for (int index = 0; index < this.debris.size(); index++) {
                ItemDisplay piece = this.debris.get(index);
                float pitch = 1.0F + 0.1F * index;
                this.timeline.at(suckStart + index * gap, () -> {
                    Displays.animate(piece, Displays.at(0, 0, 0, 0), 6);
                    this.context.sound(Sound.ENTITY_ENDERMAN_TELEPORT, 0.4F, pitch);
                });
            }

            // Charge: the tear takes the rarity colour and throbs.
            this.timeline.at(chargeStart, () -> {
                if (this.tier >= 1) {
                    this.rift.setGlowColorOverride(Reveal.color(this.target));
                }
                if (this.tier >= 2) {
                    this.context.sound(Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.2F);
                }
                if (this.tier >= 3) {
                    this.context.sound(Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 1.2F);
                }
            });
            this.timeline.during(chargeStart, revealAt, step -> {
                if (step % 8 == 0) {
                    Displays.animate(this.rift, slit(this.width * 1.35F, this.height), 4);
                    if (this.tier >= 2) {
                        this.context.sound(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.8F, 0.7F);
                    }
                } else if (step % 8 == 4) {
                    Displays.animate(this.rift, slit(this.width, this.height), 4);
                }
                if (this.tier >= 1 && step % 2 == 0) {
                    this.context.particle(Particle.REVERSE_PORTAL, this.center, 4, 0.1, 0.05);
                }
            });

            // Reveal: the reward is pushed out towards the viewer and the tear seals behind it.
            this.timeline.at(revealAt, () -> {
                if (this.tier >= 1) {
                    this.hero.setGlowing(true);
                    this.hero.setGlowColorOverride(Reveal.color(this.target));
                }
                this.hero.teleport(this.front);
                Displays.animate(this.hero, Displays.at(0, 0, 0, this.revealScale * 1.25F), PUSH_TICKS);
                Displays.animate(this.rift, slit(0, this.height), 8);
                Reveal.burst(this.context, this.front, 0, this.target, this.tier);
                Reveal.fanfare(this.context, this.tier);
                this.context.sound(Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.6F);
            });
            this.timeline.at(revealAt + PUSH_TICKS, () -> Displays.animate(this.hero, Displays.at(0, 0, 0, this.revealScale), 6));
            this.timeline.at(revealAt + 4, this.label::grow);
            this.timeline.at(revealAt + 9, () -> this.context.remove(this.rift));

            this.timeline.during(revealAt + 10, holdEnd, step -> {
                if (step % 20 == 0) {
                    float bob = (step / 20) % 2 == 0 ? 0.06F : -0.02F;
                    Displays.animate(this.hero, Displays.at(0, bob, 0, this.revealScale), 20);
                }
                if (step % 5 == 0) {
                    Reveal.sparkle(this.context, this.front, 0, this.revealScale, this.target, this.tier);
                }
            });
            this.timeline.at(holdEnd, () -> {
                Displays.animate(this.hero, Displays.at(0, 0, 0, 0), 6);
                this.label.shrink();
            });
            this.timeline.endAt(holdEnd + 6);
        }

        /** The tear as a thin slab of {@code width} by {@code height}, centred on the entity. */
        private static Transformation slit(float width, float height) {
            return new Transformation(new Vector3f(-width / 2, -height / 2, -DEPTH / 2), new Quaternionf(),
                new Vector3f(width, height, DEPTH), new Quaternionf());
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
