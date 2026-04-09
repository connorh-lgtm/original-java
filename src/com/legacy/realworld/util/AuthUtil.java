package com.legacy.realworld.util;

import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Static authentication utility class.
 * 
 * Anti-patterns:
 * - Session-based auth instead of stateless JWT
 * - Static methods make testing difficult (can't mock)
 * - These methods are called at the top of EVERY servlet method (code duplication)
 * - No role-based access control
 * 
 * Modern equivalent:
 * - Spring Security filter chain
 * - JWT token validation in a single filter
 * - @AuthenticationPrincipal annotation injection
 * - Role-based @PreAuthorize annotations
 * 
 * @author legacy-team
 * @since 1.0
 */
public class AuthUtil {

    // Dead code: was going to add rate limiting but never did
    // private static final Map<String, Integer> loginAttempts = new HashMap<>();
    // private static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * Check if the current request is from an authenticated user.
     * 
     * TODO: This only checks for session existence - no token validation,
     * no expiry check, no signature verification.
     * The modern version validates a signed JWT token on every request.
     * 
     * @param request the HTTP request
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        // Just check if userId exists in session - that's our "authentication"
        // TODO: No token validation, no expiry, no signature check
        Object userId = session.getAttribute("userId");
        return userId != null;
    }

    /**
     * Get the current authenticated user's ID from the session.
     * 
     * TODO: This casts to String without type checking.
     * If someone puts a non-String in the session, this will throw ClassCastException.
     * 
     * @param request the HTTP request
     * @return the user ID or null
     */
    public static String getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        // TODO: No type safety here - just a raw cast
        return (String) session.getAttribute("userId");
    }

    /**
     * Send a 401 Unauthorized response with JSON body.
     * 
     * This is called at the top of every protected servlet method.
     * TODO: Should be a servlet filter instead of copy-pasted calls.
     * The modern version handles this in a single Spring Security filter.
     * 
     * @param response the HTTP response
     * @throws IOException if writing fails
     */
    public static void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject error = new JsonObject();
        error.addProperty("error", "Unauthorized");
        error.addProperty("message", "You must be logged in to perform this action");

        PrintWriter out = response.getWriter();
        out.print(error.toString());
        out.flush();
    }

    /**
     * Send a 403 Forbidden response.
     * Dead code: was added but never used because we only check authentication,
     * not authorization. There's no role system.
     */
    /*
    public static void sendForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JsonObject error = new JsonObject();
        error.addProperty("error", "Forbidden");
        error.addProperty("message", "You do not have permission to perform this action");
        
        PrintWriter out = response.getWriter();
        out.print(error.toString());
        out.flush();
    }
    */

    // Dead code: rate limiting was planned but never completed
    /*
    public static boolean isRateLimited(String ipAddress) {
        Integer attempts = loginAttempts.get(ipAddress);
        return attempts != null && attempts >= MAX_LOGIN_ATTEMPTS;
    }
    
    public static void recordLoginAttempt(String ipAddress) {
        loginAttempts.merge(ipAddress, 1, Integer::sum);
    }
    
    public static void clearLoginAttempts(String ipAddress) {
        loginAttempts.remove(ipAddress);
    }
    */

    // Prevent instantiation
    private AuthUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
