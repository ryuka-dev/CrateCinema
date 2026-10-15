package io.github.ryukadev.cratecinema.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import io.github.ryukadev.cratecinema.engine.Tiers;

/**
 * Typed, validated reads from a settings file ({@code config.yml} or a show's file). The server owner's file is read first; a
 * value that is missing or invalid falls back to the default shipped in the jar, with a warning
 * for invalid ones. A default that is itself invalid is a packaging error and fails loudly.
 */
public final class SettingsFile {

    private final String fileName;
    private final ConfigurationSection values;
    private final Configuration defaults;
    private final Logger logger;

    public SettingsFile(String fileName, ConfigurationSection values, Configuration defaults, Logger logger) {
        this.fileName = fileName;
        this.values = values;
        this.defaults = defaults;
        this.logger = logger;
    }

    /** One whole number per tier, each within {@code [min, max]}. A single number applies to every tier. */
    public int[] tierInts(String key, int min, int max) {
        int[] parsed = parseTierInts(this.values.get(key), min, max);
        if (parsed == null) {
            this.warnInvalid(key, (Tiers.MAX + 1) + " whole numbers from " + min + " to " + max);
            parsed = parseTierInts(this.defaults.get(key), min, max);
        }
        return requireDefault(parsed, key);
    }

    /** One number per tier, each within {@code [min, max]}. A single number applies to every tier. */
    public double[] tierDoubles(String key, double min, double max) {
        double[] parsed = parseTierDoubles(this.values.get(key), min, max);
        if (parsed == null) {
            this.warnInvalid(key, (Tiers.MAX + 1) + " numbers from " + min + " to " + max);
            parsed = parseTierDoubles(this.defaults.get(key), min, max);
        }
        return requireDefault(parsed, key);
    }

    public boolean bool(String key) {
        Object value = this.values.get(key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        this.warnInvalid(key, "true or false");
        if (this.defaults.get(key) instanceof Boolean flag) {
            return flag;
        }
        throw missingDefault(key);
    }

    public String text(String key) {
        Object value = this.values.get(key);
        if (value instanceof String string) {
            return string;
        }
        this.warnInvalid(key, "text");
        if (this.defaults.get(key) instanceof String string) {
            return string;
        }
        throw missingDefault(key);
    }

    /** One of {@code choices}, matched ignoring case and with '-' for '_'. */
    public <E extends Enum<E>> E choice(String key, Class<E> choices) {
        E parsed = parseChoice(this.values.get(key), choices);
        if (parsed == null) {
            this.warnInvalid(key, "one of " + Arrays.stream(choices.getEnumConstants())
                .map(choice -> choice.name().toLowerCase(Locale.ROOT).replace('_', '-')).toList());
            parsed = parseChoice(this.defaults.get(key), choices);
        }
        return requireDefault(parsed, key);
    }

    public int integer(String key, int min, int max) {
        double value = this.decimal(key, min, max);
        return (int) Math.round(value);
    }

    /** A section of whole numbers keyed by name, each within {@code [min, max]}; invalid entries are skipped. */
    public Map<String, Integer> intMap(String key, int min, int max) {
        ConfigurationSection section = this.values.getConfigurationSection(key);
        if (section == null) {
            section = this.defaults.getConfigurationSection(key);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String name : section.getKeys(false)) {
            Object value = section.get(name);
            if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())
                && number.intValue() >= min && number.intValue() <= max) {
                result.put(name, number.intValue());
            } else {
                this.logger.warning(this.fileName + ": '" + key + "." + name + "' should be a whole number from "
                    + min + " to " + max + "; ignoring it.");
            }
        }
        return result;
    }

    public double decimal(String key, double min, double max) {
        Object value = this.values.get(key);
        if (value instanceof Number number && number.doubleValue() >= min && number.doubleValue() <= max) {
            return number.doubleValue();
        }
        this.warnInvalid(key, "a number from " + min + " to " + max);
        Object fallback = this.defaults.get(key);
        if (fallback instanceof Number number) {
            return number.doubleValue();
        }
        throw missingDefault(key);
    }

    /** A material name that passes {@code check}, for example a block or an item. */
    public Material material(String key, Predicate<Material> check, String expected) {
        Material parsed = parseMaterial(this.values.get(key), check);
        if (parsed == null) {
            this.warnInvalid(key, expected);
            parsed = parseMaterial(this.defaults.get(key), check);
        }
        return requireDefault(parsed, key);
    }

    /** A colour written as {@code "#RRGGBB"}. */
    public Color color(String key) {
        Color parsed = parseColor(this.values.get(key));
        if (parsed == null) {
            this.warnInvalid(key, "a colour like \"#AA3CFF\"");
            parsed = parseColor(this.defaults.get(key));
        }
        return requireDefault(parsed, key);
    }

    private void warnInvalid(String key, String expected) {
        if (this.values.contains(key, true)) {
            this.logger.warning(this.fileName + ": '" + key + "' should be " + expected + "; using the default.");
        }
    }

    private static int[] parseTierInts(Object value, int min, int max) {
        double[] doubles = parseTierDoubles(value, min, max);
        if (doubles == null) {
            return null;
        }
        int[] ints = new int[doubles.length];
        for (int index = 0; index < doubles.length; index++) {
            if (doubles[index] != Math.rint(doubles[index])) {
                return null;
            }
            ints[index] = (int) doubles[index];
        }
        return ints;
    }

    private static double[] parseTierDoubles(Object value, double min, double max) {
        int count = Tiers.MAX + 1;
        double[] result = new double[count];
        if (value instanceof Number number) {
            Arrays.fill(result, number.doubleValue());
        } else if (value instanceof List<?> list && list.size() == count) {
            for (int index = 0; index < count; index++) {
                if (!(list.get(index) instanceof Number number)) {
                    return null;
                }
                result[index] = number.doubleValue();
            }
        } else {
            return null;
        }
        for (double each : result) {
            if (each < min || each > max) {
                return null;
            }
        }
        return result;
    }

    private static <E extends Enum<E>> E parseChoice(Object value, Class<E> choices) {
        if (!(value instanceof String name)) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (E choice : choices.getEnumConstants()) {
            if (choice.name().equals(normalized)) {
                return choice;
            }
        }
        return null;
    }

    private static Material parseMaterial(Object value, Predicate<Material> check) {
        if (!(value instanceof String name)) {
            return null;
        }
        Material material = Material.matchMaterial(name.trim());
        return material != null && check.test(material) ? material : null;
    }

    private static Color parseColor(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String hex = text.trim().toUpperCase(Locale.ROOT);
        if (!hex.matches("#[0-9A-F]{6}")) {
            return null;
        }
        return Color.fromRGB(Integer.parseInt(hex.substring(1), 16));
    }

    private <T> T requireDefault(T value, String key) {
        if (value == null) {
            throw missingDefault(key);
        }
        return value;
    }

    private IllegalStateException missingDefault(String key) {
        return new IllegalStateException("Default for '" + key + "' in " + this.fileName + " is missing or invalid");
    }
}
