package io.github.ryukadev.cratecinema.engine;

import java.util.List;
import java.util.Optional;

/**
 * The configured shows that can be previewed, by name.
 */
public interface ShowCatalog {

    /** Every name {@link #get} accepts, sorted. */
    List<String> names();

    Optional<Show> get(String name);
}
