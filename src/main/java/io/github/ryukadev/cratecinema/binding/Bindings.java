package io.github.ryukadev.cratecinema.binding;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.ryukadev.cratecinema.engine.Show;
import io.github.ryukadev.cratecinema.engine.ShowCatalog;

/**
 * Which crates play which show, as configured by the server owner in {@code bindings.yml}.
 * Crates without a binding are left to the crate plugin.
 */
public final class Bindings {

    private final Map<String, Show> showsByCrate;

    private Bindings(Map<String, Show> showsByCrate) {
        this.showsByCrate = Map.copyOf(showsByCrate);
    }

    public static Bindings load(File file, ShowCatalog catalog, Logger logger) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("bindings");
        Map<String, Show> showsByCrate = new HashMap<>();
        if (section != null) {
            for (String crateId : section.getKeys(false)) {
                String showId = section.getString(crateId);
                Optional<Show> show = showId == null ? Optional.empty() : catalog.get(showId);
                if (show.isEmpty()) {
                    logger.warning("bindings.yml: crate '" + crateId + "' is bound to unknown show '" + showId
                        + "'; it keeps its normal opening. Known shows: " + catalog.ids());
                    continue;
                }
                showsByCrate.put(crateId, show.get());
            }
        }
        logger.info("Loaded " + showsByCrate.size() + " crate binding(s).");
        return new Bindings(showsByCrate);
    }

    public Set<String> crateIds() {
        return this.showsByCrate.keySet();
    }

    public Optional<Show> showFor(String crateId) {
        return Optional.ofNullable(this.showsByCrate.get(crateId));
    }
}
