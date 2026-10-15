package io.github.ryukadev.cratecinema.shows;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowContext;
import io.github.ryukadev.cratecinema.engine.ShowInstance;
import io.github.ryukadev.cratecinema.engine.ShowRound;

/**
 * The simplest possible show: the reward grows out of nothing above the crate, spins, and
 * disappears. Used to verify the integration, not meant to be pretty.
 */
public final class TestShow implements Show {

    public static final String ID = "cinema_test";

    private static final int GROW_AT = 2;
    private static final int GROW_TICKS = 10;
    private static final int SPIN_FROM = GROW_AT + GROW_TICKS;
    private static final int SPIN_STEP_TICKS = 10;
    private static final int SPIN_STEPS = 4;
    private static final int HOLD_TICKS = 20;
    private static final int TOTAL_TICKS = SPIN_FROM + SPIN_STEP_TICKS * SPIN_STEPS + HOLD_TICKS;

    private static final float SCALE = 0.8F;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        Location anchor = context.anchor();
        ItemDisplay display = context.spawn(anchor, ItemDisplay.class, entity -> {
            entity.setItemStack(round.target().icon());
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setTransformation(transformation(0.0F, 0.0F));
        });
        return new Instance(display);
    }

    private static Transformation transformation(float scale, float yaw) {
        return new Transformation(
            new Vector3f(),
            new Quaternionf().rotateY(yaw),
            new Vector3f(scale, scale, scale),
            new Quaternionf());
    }

    private static final class Instance implements ShowInstance {

        private final ItemDisplay display;
        private int ticks;

        private Instance(ItemDisplay display) {
            this.display = display;
        }

        @Override
        public boolean tick() {
            this.ticks++;
            if (!this.display.isValid()) {
                return false;
            }

            if (this.ticks == GROW_AT) {
                this.animateTo(transformation(SCALE, 0.0F), GROW_TICKS);
            } else if (this.ticks >= SPIN_FROM && this.ticks < SPIN_FROM + SPIN_STEP_TICKS * SPIN_STEPS
                && (this.ticks - SPIN_FROM) % SPIN_STEP_TICKS == 0) {
                int step = (this.ticks - SPIN_FROM) / SPIN_STEP_TICKS + 1;
                this.animateTo(transformation(SCALE, (float) (Math.PI / 2.0) * step), SPIN_STEP_TICKS);
            }
            return this.ticks < TOTAL_TICKS;
        }

        /** The client interpolates to the new transformation; nothing is sent in between. */
        private void animateTo(Transformation target, int durationTicks) {
            this.display.setInterpolationDelay(0);
            this.display.setInterpolationDuration(durationTicks);
            this.display.setTransformation(target);
        }

        @Override
        public void cleanup() {
            if (this.display.isValid()) {
                this.display.remove();
            }
        }
    }
}
