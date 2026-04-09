package com.legacy.realworld.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Poor man's dependency injection via Service Locator pattern.
 * 
 * This is a static registry of "services" that servlets can look up by name.
 * It's the opposite of modern dependency injection:
 * - Dependencies are pulled, not pushed
 * - No compile-time type safety
 * - Hidden dependencies (not visible in constructor signatures)
 * - Hard to test (global mutable state)
 * - No lifecycle management
 * 
 * Modern equivalent:
 * - Spring's ApplicationContext with @Autowired / constructor injection
 * - Type-safe, compile-time checked dependencies
 * - Automatic lifecycle management (singleton, prototype, etc.)
 * - Easy to test with mock injection
 * 
 * @author legacy-team
 * @since 1.0
 */
public class ServiceLocator {

    // Global mutable state - the antithesis of modern DI
    // TODO: This is not thread-safe! HashMap is not synchronized.
    private static final Map<String, Object> services = new HashMap<String, Object>();

    // Dead code: was going to add lazy initialization support
    // private static final Map<String, Callable<?>> factories = new HashMap<>();
    // private static boolean initialized = false;

    /**
     * Register a service with a name.
     * 
     * Called in servlet init() methods to register themselves.
     * TODO: No duplicate detection - registering the same name twice silently overwrites.
     * TODO: Not thread-safe - concurrent servlet initialization could corrupt the map.
     * 
     * @param name the service name
     * @param service the service instance
     */
    public static void register(String name, Object service) {
        // TODO: Should check for duplicates
        // if (services.containsKey(name)) {
        //     System.err.println("WARNING: Overwriting service: " + name);
        // }
        services.put(name, service);
        System.out.println("ServiceLocator: registered '" + name + "' -> " + 
                           service.getClass().getSimpleName());
    }

    /**
     * Look up a service by name.
     * 
     * Returns Object - caller must cast to the expected type.
     * TODO: No type safety! If someone changes the registered type,
     * callers will get ClassCastException at runtime.
     * 
     * Modern equivalent: @Autowired with compile-time type checking.
     * 
     * @param name the service name
     * @return the service instance, or null if not found
     */
    public static Object lookup(String name) {
        Object service = services.get(name);
        if (service == null) {
            // TODO: Should this throw an exception? Currently returns null
            // which will cause NullPointerException at the call site.
            System.err.println("WARNING: Service not found: " + name);
        }
        return service;
    }

    /**
     * Type-safe lookup (added later, but rarely used because existing code
     * already does the cast manually).
     * 
     * @param name the service name
     * @param type the expected type
     * @return the service instance cast to the expected type
     * @throws ClassCastException if the service is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name, Class<T> type) {
        Object service = lookup(name);
        if (service == null) {
            return null;
        }
        // TODO: This will throw ClassCastException with an unhelpful message
        return (T) service;
    }

    /**
     * Check if a service is registered.
     */
    public static boolean isRegistered(String name) {
        return services.containsKey(name);
    }

    /**
     * Clear all registered services.
     * Only used in testing (which we don't have).
     */
    public static void clear() {
        services.clear();
        System.out.println("ServiceLocator: all services cleared");
    }

    /**
     * Get the count of registered services.
     * Used for debugging.
     */
    public static int getServiceCount() {
        return services.size();
    }

    // Dead code: lazy initialization support that was never completed
    /*
    public static void registerFactory(String name, Callable<?> factory) {
        factories.put(name, factory);
    }
    
    public static Object lookupOrCreate(String name) {
        Object service = services.get(name);
        if (service == null && factories.containsKey(name)) {
            try {
                service = factories.get(name).call();
                services.put(name, service);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return service;
    }
    */

    // Prevent instantiation
    private ServiceLocator() {
        throw new UnsupportedOperationException("Utility class");
    }
}
