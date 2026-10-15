package io.github.ryukadev.cratecinema.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Tick-based script for a show: actions run at fixed ticks, repeating actions run every tick in a
 * range. Shows build one in their constructor and advance it from {@link ShowInstance#tick()}.
 */
public final class Timeline {

    private record Cue(int tick, Runnable action) {
    }

    private record Span(int from, int to, IntConsumer action) {
    }

    private final List<Cue> cues = new ArrayList<>();
    private final List<Span> spans = new ArrayList<>();
    private int length;
    private int tick;
    private int nextCue;
    private boolean sorted;

    /** Runs {@code action} once, at {@code tick}. */
    public Timeline at(int tick, Runnable action) {
        this.cues.add(new Cue(tick, action));
        this.length = Math.max(this.length, tick + 1);
        this.sorted = false;
        return this;
    }

    /** Runs {@code action} with the tick offset from {@code from}, on every tick in {@code [from, to)}. */
    public Timeline during(int from, int to, IntConsumer action) {
        this.spans.add(new Span(from, to, action));
        this.length = Math.max(this.length, to);
        return this;
    }

    /** Makes the timeline last at least until {@code tick}. */
    public Timeline endAt(int tick) {
        this.length = Math.max(this.length, tick);
        return this;
    }

    public int length() {
        return this.length;
    }

    /**
     * Runs everything due at the current tick, then advances.
     *
     * @return {@code false} once the timeline has run to its end
     */
    public boolean tick() {
        if (!this.sorted) {
            this.cues.sort(Comparator.comparingInt(Cue::tick));
            this.sorted = true;
        }
        while (this.nextCue < this.cues.size() && this.cues.get(this.nextCue).tick() <= this.tick) {
            this.cues.get(this.nextCue++).action().run();
        }
        for (Span span : this.spans) {
            if (this.tick >= span.from() && this.tick < span.to()) {
                span.action().accept(this.tick - span.from());
            }
        }
        this.tick++;
        return this.tick < this.length;
    }
}
