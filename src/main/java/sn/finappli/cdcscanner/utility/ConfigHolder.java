package sn.finappli.cdcscanner.utility;

import sn.finappli.cdcscanner.model.input.YamlConfig;

/**
 * Utility class for managing and providing access to the configuration context in a multithreading environment.
 */
public final class ConfigHolder {

    private static final ThreadLocal<YamlConfig> CONFIG_HOLDER = new ThreadLocal<>();

    private ConfigHolder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Retrieves the configuration context for the current thread.
     *
     * @return The configuration context for the current thread.
     */
    public static YamlConfig getContext() {
        return CONFIG_HOLDER.get();
    }

    /**
     * Sets the configuration context for the current thread.
     *
     * @param context The configuration context to set.
     */
    public static void setContext(YamlConfig context) {
        CONFIG_HOLDER.set(context);
    }

    /**
     * Clears the configuration context for the current thread.
     */
    public static void clearContext() {
        CONFIG_HOLDER.remove();
    }
}
