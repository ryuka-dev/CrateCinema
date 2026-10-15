package io.github.ryukadev.cratecinema.shows;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import io.github.ryukadev.cratecinema.engine.Displays;
import io.github.ryukadev.cratecinema.engine.RewardView;
import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowContext;
import io.github.ryukadev.cratecinema.engine.ShowInstance;
import io.github.ryukadev.cratecinema.engine.ShowRound;
import io.github.ryukadev.cratecinema.engine.Timeline;

/**
 * Simple: text, particles and sound only, no item or block displays. Three dots light up one by
 * one, then the reward's name appears with a burst. Meant for Bedrock players, whose clients cannot
 * show item and block displays, and for servers that want something light.
 *
 * <p>Higher tiers count in more slowly and end with a bigger burst and fanfare.
 */
public final class SimpleShow implements Show {

    public static final String ID = "simple";

    private static final int[] BEAT_TICKS = {8, 10, 13, 16};
    private static final int[] HOLD_TICKS = {30, 40, 50, 60};
    private static final String DOT_OFF = "§8●";
    private static final String DOT_ON = "§f●";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ShowInstance create(ShowContext context, ShowRound round) {
        RewardView target = round.target();
        int tier = context.tierOf(target);
        Location center = context.anchor().add(0, 0.9, 0);
        Timeline timeline = new Timeline();

        TextDisplay text = context.spawn(center, TextDisplay.class, display -> {
            Displays.setText(display, dots(0));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.fromARGB(96, 0, 0, 0));
            display.setShadowed(true);
            display.setTransformation(Displays.at(0, 0, 0, 0));
        });

        int beat = BEAT_TICKS[tier];
        timeline.at(1, () -> Displays.animate(text, Displays.at(0, 0, 0, 1.0F), 5));
        for (int dot = 1; dot <= 3; dot++) {
            int lit = dot;
            timeline.at(lit * beat, () -> {
                Displays.setText(text, dots(lit));
                context.sound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 0.8F + 0.25F * lit);
            });
        }

        int revealAt = 4 * beat;
        timeline.at(revealAt, () -> {
            Displays.setText(text, Reveal.labelText(target));
            Displays.animate(text, Displays.at(0, 0, 0, 1.3F), 4);
            Reveal.burst(context, center, 0.1F, target, tier);
            Reveal.fanfare(context, tier);
        });
        timeline.at(revealAt + 4, () -> Displays.animate(text, Displays.at(0, 0, 0, 1.0F), 6));
        int holdEnd = revealAt + 10 + HOLD_TICKS[tier];
        timeline.during(revealAt + 10, holdEnd, step -> {
            if (step % 5 == 0) {
                Reveal.sparkle(context, center, 0.1F, 0.4F, target, tier);
            }
        });
        timeline.at(holdEnd, () -> Displays.animate(text, Displays.at(0, 0, 0, 0), 6));
        timeline.endAt(holdEnd + 6);

        return new ShowInstance() {
            @Override
            public boolean tick() {
                return timeline.tick();
            }

            @Override
            public void cleanup() {
                // Entities are removed by the context.
            }
        };
    }

    private static String dots(int lit) {
        StringBuilder text = new StringBuilder();
        for (int dot = 1; dot <= 3; dot++) {
            if (dot > 1) {
                text.append(' ');
            }
            text.append(dot <= lit ? DOT_ON : DOT_OFF);
        }
        return text.toString();
    }
}
