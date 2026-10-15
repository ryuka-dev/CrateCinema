package io.github.ryukadev.cratecinema.integration.excellentcrates;

/**
 * One attachment to one ExcellentCrates pipeline. ExcellentCrates offers no way to unregister,
 * so a processor or stage left behind after we detach is switched off through this flag.
 */
final class Registration {

    private boolean active = true;

    boolean isActive() {
        return this.active;
    }

    void deactivate() {
        this.active = false;
    }
}
