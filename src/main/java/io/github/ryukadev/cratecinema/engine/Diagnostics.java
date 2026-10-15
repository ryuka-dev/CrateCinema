package io.github.ryukadev.cratecinema.engine;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Optional debug logging. Only controls log output; nothing else may depend on it.
 */
public final class Diagnostics {

    private final Logger logger;
    private final boolean enabled;

    public Diagnostics(Logger logger, boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    public void debug(Supplier<String> message) {
        if (this.enabled) {
            this.logger.info("[debug] " + message.get());
        }
    }
}
