package io.github.ryukadev.cratecinema.shows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
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
 * Cards: five face-down cards fan out above the crate and are shuffled. One steps forward,
 * gathers light in the reward's rarity colour and flips over to show the reward; the rest fall
 * away.
 *
 * <p>Higher tiers shuffle more, charge longer and flip more slowly.
 */
public final class CardsShow implements Show {

    public static final String ID = "cards";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        return new Instance(context, round);
    }

    private static final class Instance implements ShowInstance {

        private static final int CARDS = 5;
        private static final float SPACING = 0.6F;
        private static final float CARD_SCALE = 0.55F;
        private static final float CHOSEN_SCALE = 0.8F;
        private static final float CHOSEN_Y = 0.1F;
        private static final Material BACK = Material.MAP;

        private static final int DEAL_TICKS = 8;
        private static final int SWAP_TICKS = 8;
        private static final int[] SWAPS = {3, 4, 5, 6};
        private static final int[] CHARGE_TICKS = {6, 14, 24, 36};
        private static final int[] FLIP_TICKS = {4, 5, 6, 8};
        private static final int[] HOLD_TICKS = {25, 35, 45, 55};

        private final ShowContext context;
        private final RewardView target;
        private final int tier;
        private final float revealScale;
        private final Location center;
        private final List<ItemDisplay> cards = new ArrayList<>();
        /** Which card sits in each slot, left to right. */
        private final int[] cardInSlot = new int[CARDS];
        private final Reveal.Label label;
        private final Timeline timeline = new Timeline();

        Instance(ShowContext context, ShowRound round) {
            this.context = context;
            this.target = round.target();
            this.tier = context.tierOf(this.target);
            this.revealScale = Reveal.itemScale(this.tier);
            this.center = context.anchor().add(0, 1.0, 0);

            for (int index = 0; index < CARDS; index++) {
                this.cards.add(context.spawn(this.center, ItemDisplay.class, display -> {
                    display.setItemStack(new ItemStack(BACK));
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setBillboard(Display.Billboard.VERTICAL);
                    display.setTransformation(card(0, 0, 0, 0, 0));
                }));
                this.cardInSlot[index] = index;
            }
            this.label = Reveal.label(context, this.center, this.target, CHOSEN_Y, this.revealScale);

            this.script();
        }

        private void script() {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // Deal: the cards fan out one after another.
            for (int slot = 0; slot < CARDS; slot++) {
                ItemDisplay card = this.cards.get(slot);
                int dealt = slot;
                this.timeline.at(1 + slot * 2, () -> {
                    Displays.animate(card, this.inSlot(dealt, CARD_SCALE), DEAL_TICKS);
                    this.context.sound(Sound.ITEM_BOOK_PAGE_TURN, 0.8F, 1.0F + 0.1F * dealt);
                });
            }
            int shuffleStart = 1 + CARDS * 2 + DEAL_TICKS;

            // Shuffle: pairs of cards trade places, one lifting over the other.
            int swaps = SWAPS[this.tier];
            for (int swap = 0; swap < swaps; swap++) {
                int at = shuffleStart + swap * SWAP_TICKS;
                int slotA = random.nextInt(CARDS);
                int slotB = (slotA + 1 + random.nextInt(CARDS - 1)) % CARDS;
                int cardA = this.cardInSlot[slotA];
                int cardB = this.cardInSlot[slotB];
                this.cardInSlot[slotA] = cardB;
                this.cardInSlot[slotB] = cardA;
                float midX = (slotX(slotA) + slotX(slotB)) / 2;
                this.timeline.at(at, () -> {
                    Displays.animate(this.cards.get(cardA), card(midX, 0.3F, 0, CARD_SCALE, 0), SWAP_TICKS / 2);
                    Displays.animate(this.cards.get(cardB), card(midX, -0.15F, 0, CARD_SCALE, 0), SWAP_TICKS / 2);
                    this.context.sound(Sound.ITEM_BOOK_PAGE_TURN, 0.6F, 1.3F);
                });
                this.timeline.at(at + SWAP_TICKS / 2, () -> {
                    Displays.animate(this.cards.get(cardA), this.inSlot(slotB, CARD_SCALE), SWAP_TICKS / 2);
                    Displays.animate(this.cards.get(cardB), this.inSlot(slotA, CARD_SCALE), SWAP_TICKS / 2);
                });
            }
            int chooseAt = shuffleStart + swaps * SWAP_TICKS + 2;

            // Choose: one card steps forward, the others drop back.
            int chosenSlot = random.nextInt(CARDS);
            ItemDisplay chosen = this.cards.get(this.cardInSlot[chosenSlot]);
            this.timeline.at(chooseAt, () -> {
                for (int slot = 0; slot < CARDS; slot++) {
                    ItemDisplay card = this.cards.get(this.cardInSlot[slot]);
                    if (card == chosen) {
                        Displays.animate(card, card(0, CHOSEN_Y, 0, CHOSEN_SCALE, 0), 8);
                    } else {
                        Displays.animate(card, card(slotX(slot) * 1.3F, -0.35F, 0, 0.35F, 0), 8);
                    }
                }
                this.context.sound(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 0.8F);
            });

            // Charge: the chosen card glows and throbs; light gathers on it.
            int chargeStart = chooseAt + 8;
            int chargeTicks = CHARGE_TICKS[this.tier];
            Particle.DustOptions dust = Reveal.dust(this.target, this.tier);
            this.timeline.at(chargeStart, () -> {
                if (this.tier >= 1) {
                    chosen.setGlowing(true);
                    chosen.setGlowColorOverride(Reveal.color(this.target));
                }
                if (this.tier >= 2) {
                    this.context.sound(Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.2F);
                }
            });
            this.timeline.during(chargeStart, chargeStart + chargeTicks, step -> {
                if (step % 2 == 0) {
                    double radius = 1.0 * (1.0 - (double) step / chargeTicks);
                    for (int arm = 0; arm < 2; arm++) {
                        double angle = step * 0.5 + Math.PI * arm;
                        this.context.particle(Particle.DUST, this.world(Math.cos(angle) * radius, CHOSEN_Y + Math.sin(angle) * radius), 1, 0.0, 0.0, dust);
                    }
                }
                if (this.tier >= 1 && step % 8 == 0) {
                    Displays.animate(chosen, card(0, CHOSEN_Y, 0, CHOSEN_SCALE + 0.08F, 0), 4);
                    this.context.sound(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 1.0F + 0.1F * (step / 8));
                } else if (this.tier >= 1 && step % 8 == 4) {
                    Displays.animate(chosen, card(0, CHOSEN_Y, 0, CHOSEN_SCALE, 0), 4);
                }
            });

            // Flip: turn edge-on, swap the face, turn back. The others fall away.
            int flipStart = chargeStart + chargeTicks;
            int flipTicks = FLIP_TICKS[this.tier];
            float quarter = (float) (Math.PI / 2);
            this.timeline.at(flipStart, () -> {
                Displays.animate(chosen, card(0, CHOSEN_Y, 0, CHOSEN_SCALE, quarter), flipTicks);
                this.context.sound(Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 0.7F);
                for (int slot = 0; slot < CARDS; slot++) {
                    ItemDisplay card = this.cards.get(this.cardInSlot[slot]);
                    if (card != chosen) {
                        Displays.animate(card, card(slotX(slot) * 1.3F, -0.6F, 0, 0, 0), 8);
                    }
                }
            });
            int edgeAt = flipStart + flipTicks;
            this.timeline.at(edgeAt, () -> {
                chosen.setItemStack(this.target.icon());
                chosen.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                Displays.animate(chosen, card(0, CHOSEN_Y, 0, CHOSEN_SCALE, -quarter), 0);
            });
            int revealAt = edgeAt + 1;
            this.timeline.at(revealAt, () -> {
                Displays.animate(chosen, card(0, CHOSEN_Y, 0, this.revealScale, 0), flipTicks);
                Reveal.burst(this.context, this.center, CHOSEN_Y, this.target, this.tier);
                Reveal.fanfare(this.context, this.tier);
            });
            this.timeline.at(revealAt + 3, this.label::grow);

            int holdEnd = revealAt + flipTicks + 4 + HOLD_TICKS[this.tier];
            this.timeline.during(revealAt + flipTicks + 4, holdEnd, step -> {
                if (step % 5 == 0) {
                    Reveal.sparkle(this.context, this.center, CHOSEN_Y, this.revealScale, this.target, this.tier);
                }
            });
            this.timeline.at(holdEnd, () -> {
                Displays.animate(chosen, card(0, CHOSEN_Y, 0, 0, 0), 6);
                this.label.shrink();
            });
            this.timeline.endAt(holdEnd + 6);
        }

        private static float slotX(int slot) {
            return (slot - (CARDS - 1) / 2.0F) * SPACING;
        }

        /** A card in its fanned slot: lower and tilted outward towards the ends. */
        private Transformation inSlot(int slot, float scale) {
            float offset = slot - (CARDS - 1) / 2.0F;
            Quaternionf tilt = new Quaternionf().rotateZ((float) Math.toRadians(-6 * offset));
            return new Transformation(new Vector3f(slotX(slot), -Math.abs(offset) * 0.06F, 0), tilt,
                new Vector3f(scale, scale, scale), new Quaternionf());
        }

        /** A card at ({@code x}, {@code y}) in the plane facing the viewer, turned {@code turn} radians about the vertical. */
        private static Transformation card(float x, float y, float z, float scale, float turn) {
            return Displays.at(x, y, z, scale, new Quaternionf().rotateY(turn));
        }

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
}
