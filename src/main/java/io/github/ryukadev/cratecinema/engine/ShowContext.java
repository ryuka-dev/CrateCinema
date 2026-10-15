package io.github.ryukadev.cratecinema.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * What a show instance may use while it runs.
 */
public final class ShowContext {

    private final Plugin plugin;
    private final Player viewer;
    private final Location anchor;

    ShowContext(Plugin plugin, Player viewer, Location anchor) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.anchor = anchor.clone();
    }

    public Player viewer() {
        return this.viewer;
    }

    public Location anchor() {
        return this.anchor.clone();
    }

    /**
     * Prepares an entity that is being spawned for a show: it is never saved with the chunk and
     * only the viewer can see it. Call it from the spawn callback so the entity is configured
     * before any client receives it, then call {@link #reveal(Entity)} on the spawned entity.
     */
    public void prepare(Entity entity) {
        entity.setPersistent(false);
        entity.setVisibleByDefault(false);
    }

    /** Shows a prepared entity to the viewer. */
    public void reveal(Entity entity) {
        this.viewer.showEntity(this.plugin, entity);
    }
}
