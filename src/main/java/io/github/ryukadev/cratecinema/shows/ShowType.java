package io.github.ryukadev.cratecinema.shows;

import java.util.List;
import java.util.function.Function;

import io.github.ryukadev.cratecinema.config.SettingsFile;
import io.github.ryukadev.cratecinema.engine.Show;

/**
 * A kind of show and how to build it from a settings file. The default settings, with comments for
 * server owners, are {@link #defaultsResource()} in the plugin jar.
 */
public record ShowType(String id, Function<SettingsFile, Show> factory) {

    public static final List<ShowType> ALL = List.of(
        new ShowType(TestShow.ID, settings -> new TestShow()),
        new ShowType(SimpleShow.ID, settings -> new SimpleShow()),
        new ShowType(OrbitShow.ID, settings -> new OrbitShow(OrbitShow.Settings.read(settings))),
        new ShowType(StarfallShow.ID, settings -> new StarfallShow(StarfallShow.Settings.read(settings))),
        new ShowType(CardsShow.ID, settings -> new CardsShow(CardsShow.Settings.read(settings))),
        new ShowType(RiftShow.ID, settings -> new RiftShow(RiftShow.Settings.read(settings))));

    public String defaultsResource() {
        return "shows/" + this.id + ".yml";
    }

    public Show create(SettingsFile settings) {
        return this.factory.apply(settings);
    }
}
