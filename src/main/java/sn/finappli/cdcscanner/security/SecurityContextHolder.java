package sn.finappli.cdcscanner.security;

/**
 * Utility class for managing the security context in a multithreading environment.
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    private SecurityContextHolder() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Retrieves the security context for the current thread.
     *
     * @return The security context for the current thread.
     */
    public static SecurityContext getContext() {
        return contextHolder.get();
    }

    /**
     * Sets the security context for the current thread.
     *
     * @param context The security context to set.
     */
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }

    /**
     * Clears the security context for the current thread.
     */
    public static void clearContext() {
        contextHolder.remove();
    }
}
