package io.github.ryukadev.cratecinema.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * What a show may use while it runs. Everything goes to the show's audience only: spawned
 * entities are hidden from everyone else, and particles and sounds are sent to the audience alone.
 * The audience is the viewer, plus, if configured, the players near the crate when the show began.
 *
 * <p>The context remembers every entity it spawned and removes them when the show ends, so a show
 * cannot leave anything behind even if it forgets to clean up.
 */
public final class ShowContext {

    private final Plugin plugin;
    private final Player viewer;
    private final List<Player> audience;
    private final Location anchor;
    private final Tiers tiers;
    private final EngineSettings settings;
    private final Vector towardViewer;
    private final Vector right;
    private final List<Entity> spawned = new ArrayList<>();

    ShowContext(Plugin plugin, Player viewer, Location anchor, Tiers tiers, EngineSettings settings) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.anchor = anchor.clone();
        this.tiers = tiers;
        this.settings = settings;
        this.audience = audienceFor(viewer, anchor, settings);

        Vector horizontal = viewer.getEyeLocation().toVector().subtract(anchor.toVector()).setY(0);
        this.towardViewer = horizontal.lengthSquared() < 1.0E-4 ? new Vector(0, 0, 1) : horizontal.normalize();
        // The viewer looks along -towardViewer; their right-hand side is that direction crossed with up.
        this.right = new Vector(this.towardViewer.getZ(), 0, -this.towardViewer.getX());
    }

    private static List<Player> audienceFor(Player viewer, Location anchor, EngineSettings settings) {
        List<Player> audience = new ArrayList<>();
        audience.add(viewer);
        if (settings.audience() == EngineSettings.Audience.NEARBY) {
            double radiusSquared = settings.nearbyRadius() * settings.nearbyRadius();
            for (Player player : anchor.getWorld().getPlayers()) {
                if (player != viewer && player.getLocation().distanceSquared(anchor) <= radiusSquared) {
                    audience.add(player);
                }
            }
        }
        return List.copyOf(audience);
    }

    public Player viewer() {
        return this.viewer;
    }

    /** The point the show is centred on, half a block above the crate. */
    public Location anchor() {
        return this.anchor.clone();
    }

    public int tierOf(RewardView reward) {
        return this.tiers.of(reward);
    }

    /** Horizontal unit vector from the anchor toward where the viewer stood when the show began. */
    public Vector towardViewer() {
        return this.towardViewer.clone();
    }

    /** Horizontal unit vector to the viewer's right, as they face the anchor. */
    public Vector right() {
        return this.right.clone();
    }

    /** A point in the plane facing the viewer through {@code center}: {@code x} to their right, {@code y} up. */
    public Location inViewPlane(Location center, double x, double y) {
        return center.clone().add(this.right.clone().multiply(x)).add(0, y, 0);
    }

    /**
     * Spawns an entity for this show. It is never saved with the chunk, only the audience can see
     * it, and it is removed when the show ends. {@code setup} runs before any client receives it.
     */
    public <T extends Entity> T spawn(Location at, Class<T> type, Consumer<? super T> setup) {
        T entity = at.getWorld().spawn(at, type, created -> {
            created.setPersistent(false);
            created.setVisibleByDefault(false);
            setup.accept(created);
        });
        this.spawned.add(entity);
        for (Player player : this.audience) {
            if (player.isOnline()) {
                player.showEntity(this.plugin, entity);
            }
        }
        return entity;
    }

    public void remove(Entity entity) {
        this.spawned.remove(entity);
        if (entity.isValid()) {
            entity.remove();
        }
    }

    void removeAll() {
        for (Entity entity : this.spawned) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        this.spawned.clear();
    }

    public void particle(Particle particle, Location at, int count, double spread, double speed) {
        int scaled = this.scaled(count);
        for (Player player : this.audience) {
            if (scaled > 0 && player.isOnline()) {
                player.spawnParticle(particle, at, scaled, spread, spread, spread, speed);
            }
        }
    }

    public <D> void particle(Particle particle, Location at, int count, double spread, double speed, D data) {
        int scaled = this.scaled(count);
        for (Player player : this.audience) {
            if (scaled > 0 && player.isOnline()) {
                player.spawnParticle(particle, at, scaled, spread, spread, spread, speed, data);
            }
        }
    }

    public void sound(Sound sound, float volume, float pitch) {
        float scaled = (float) (volume * this.settings.soundVolume());
        if (scaled <= 0) {
            return;
        }
        for (Player player : this.audience) {
            if (player.isOnline()) {
                player.playSound(this.anchor, sound, SoundCategory.MASTER, scaled, pitch);
            }
        }
    }

    private int scaled(int count) {
        return (int) Math.round(count * this.settings.particleDensity());
    }
}
