package io.github.ryukadev.cratecinema.engine;

import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Helpers for animating display entities. Movement is sent as a target plus a duration and the
 * client interpolates, so nothing needs to be sent on the ticks in between.
 */
public final class Displays {

    private Displays() {
    }

    public static Transformation at(float x, float y, float z, float scale) {
        return new Transformation(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf());
    }

    public static Transformation at(float x, float y, float z, float scale, Quaternionf rotation) {
        return new Transformation(new Vector3f(x, y, z), rotation, new Vector3f(scale, scale, scale), new Quaternionf());
    }

    /** Moves the display to {@code target} over {@code ticks}, starting now. */
    public static void animate(Display display, Transformation target, int ticks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(0, ticks));
        display.setTransformation(target);
    }

    /**
     * Sets legacy-formatted text. Paper deprecates the string setter in favour of Adventure
     * components; the string form is kept because it also works on Spigot.
     */
    @SuppressWarnings("deprecation")
    public static void setText(TextDisplay display, String legacyText) {
        display.setText(legacyText);
    }

    public static float easeInOutSine(float t) {
        return (float) (-(Math.cos(Math.PI * clamp01(t)) - 1.0) / 2.0);
    }

    public static float easeOutCubic(float t) {
        float inverse = 1.0F - clamp01(t);
        return 1.0F - inverse * inverse * inverse;
    }

    private static float clamp01(float t) {
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}
