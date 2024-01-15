package sn.finappli.cdcscanner.utility;

import sn.finappli.cdcscanner.model.input.YamlConfig;

public final class ConfigHolder {

    private static final ThreadLocal<YamlConfig> CONFIG_HOLDER = new ThreadLocal<>();

    private ConfigHolder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void setContext(YamlConfig context) {
        CONFIG_HOLDER.set(context);
    }

    public static YamlConfig getContext() {
        return CONFIG_HOLDER.get();
    }

    public static void clearContext() {
        CONFIG_HOLDER.remove();
    }
}
