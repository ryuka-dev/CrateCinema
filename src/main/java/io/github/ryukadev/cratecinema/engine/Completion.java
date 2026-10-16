package io.github.ryukadev.cratecinema.engine;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps the crate plugin's completion callback so it runs at most once and a failure inside it
 * cannot break the session bookkeeping. The callback learns why the show ended.
 */
final class Completion {

    private final Consumer<EndReason> callback;
    private final Logger logger;
    private boolean done;

    Completion(Consumer<EndReason> callback, Logger logger) {
        this.callback = callback;
        this.logger = logger;
    }

    void run(EndReason reason) {
        if (this.done) {
            return;
        }
        this.done = true;
        try {
            this.callback.accept(reason);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "The crate plugin failed while completing an opening", exception);
        }
    }
}
