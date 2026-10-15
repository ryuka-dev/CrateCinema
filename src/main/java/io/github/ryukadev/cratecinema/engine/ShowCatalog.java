package io.github.ryukadev.cratecinema.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The shows that crates can be bound to, by id.
 */
public final class ShowCatalog {

    private final Map<String, Show> shows = new LinkedHashMap<>();

    public void register(Show show) {
        if (this.shows.putIfAbsent(show.id(), show) != null) {
            throw new IllegalArgumentException("Duplicate show id: " + show.id());
        }
    }

    public Optional<Show> get(String id) {
        return Optional.ofNullable(this.shows.get(id));
    }

    public Set<String> ids() {
        return Set.copyOf(this.shows.keySet());
    }
}
