package com.legacy.realworld.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.legacy.realworld.model.User;
import com.legacy.realworld.util.AuthUtil;
import com.legacy.realworld.util.DatabaseUtil;
import com.legacy.realworld.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Fat servlet that handles user registration and login.
 * 
 * Major anti-patterns in this file:
 * - Plain text password storage (no hashing)
 * - Password comparison with .equals() instead of constant-time comparison
 * - Session-based auth instead of stateless JWT
 * - URL path parsing to determine operation (register vs login)
 * - Business logic, SQL, and presentation all in one class
 * 
 * Modern equivalent: UsersApi.java with Spring Security, PasswordEncoder, JwtService
 * 
 * @author legacy-team
 * @since 1.0
 */
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("UserServlet initialized");
    }

    /**
     * POST /api/users - Register new user
     * POST /api/users/login - Login existing user
     * 
     * Determines which operation based on URL path.
     * TODO: This is fragile. The modern version uses separate @RequestMapping annotations.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Determine operation from URL path - fragile string matching
        String pathInfo = request.getPathInfo();
        boolean isLogin = (pathInfo != null && pathInfo.contains("login"));

        try {
            // Read request body
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject userJson = json.getAsJsonObject("user");

            if (isLogin) {
                handleLogin(userJson, request, response, out);
            } else {
                handleRegistration(userJson, request, response, out);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal error: " + e.getMessage());
            out.print(error.toString());
        }

        out.flush();
    }

    /**
     * Handle user registration.
     * 
     * TODO: SECURITY - Password is stored as plain text!
     * The modern version uses PasswordEncoder.encode() to hash passwords.
     * TODO: No email uniqueness check before INSERT (will fail with SQL constraint).
     * TODO: No input validation - email could be "asdf", username could be empty.
     */
    private void handleRegistration(JsonObject userJson, HttpServletRequest request,
                                     HttpServletResponse response, PrintWriter out) 
            throws SQLException {

        String email = userJson.get("email").getAsString();
        String username = userJson.get("username").getAsString();
        String password = userJson.get("password").getAsString();
        String id = UUID.randomUUID().toString();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();

            // Check if email already exists
            // TODO: Race condition - another request could insert between check and insert
            stmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Email already in use");
                out.print(error.toString());
                rs.close();
                return;
            }
            rs.close();
            stmt.close();

            // Check if username already exists
            // TODO: Same race condition as the email check above
            stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Username already in use");
                out.print(error.toString());
                rs.close();
                return;
            }
            rs.close();
            stmt.close();

            // Insert new user with PLAIN TEXT password
            // TODO: CRITICAL SECURITY ISSUE - must hash the password!
            // Modern version: passwordEncoder.encode(password)
            stmt = conn.prepareStatement(
                "INSERT INTO users (id, email, username, password, bio, image) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, id);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.setString(4, password);  // PLAIN TEXT! No hashing!
            stmt.setString(5, "");
            stmt.setString(6, "");
            stmt.executeUpdate();

            // Set session - old-school session-based auth
            // TODO: Modern version uses stateless JWT tokens
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", id);

            // Build response
            User user = new User(id, email, username, password, "", "");
            JsonObject responseJson = new JsonObject();
            responseJson.add("user", buildUserResponse(user, session.getId()));

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(responseJson.toString());

        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Handle user login.
     * 
     * TODO: SECURITY - Compares passwords with .equals() on plain text.
     * This is vulnerable to timing attacks.
     * The modern version uses passwordEncoder.matches() which:
     *   1. Compares against a hashed value
     *   2. Uses constant-time comparison
     */
    private void handleLogin(JsonObject userJson, HttpServletRequest request,
                              HttpServletResponse response, PrintWriter out) 
            throws SQLException {

        String email = userJson.get("email").getAsString();
        String password = userJson.get("password").getAsString();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();

            // Inline SQL query
            stmt = conn.prepareStatement(
                "SELECT * FROM users WHERE email = ?"
            );
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                // TODO: SECURITY - Plain text password comparison with .equals()!
                // This is NOT constant-time and passwords are NOT hashed.
                // Modern version: passwordEncoder.matches(rawPassword, hashedPassword)
                if (password.equals(storedPassword)) {
                    String userId = rs.getString("id");

                    // Create session
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userId", userId);

                    User user = new User(
                        userId,
                        rs.getString("email"),
                        rs.getString("username"),
                        storedPassword,  // Including password in object - bad practice
                        rs.getString("bio"),
                        rs.getString("image")
                    );

                    JsonObject responseJson = new JsonObject();
                    responseJson.add("user", buildUserResponse(user, session.getId()));
                    out.print(responseJson.toString());
                } else {
                    // TODO: Should not distinguish between "user not found" and "wrong password"
                    // This leaks information about which emails are registered
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid password");
                    out.print(error.toString());
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonObject error = new JsonObject();
                error.addProperty("error", "User not found");
                out.print(error.toString());
            }

        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * GET /api/users - Get current user (from session)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            stmt.setString(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    rs.getString("id"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("bio"),
                    rs.getString("image")
                );

                JsonObject responseJson = new JsonObject();
                responseJson.add("user", buildUserResponse(user, request.getSession().getId()));
                out.print(responseJson.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "User not found");
                out.print(error.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        out.flush();
    }

    /**
     * Build user JSON response.
     * TODO: This includes the session ID as a "token" - not a real JWT.
     * The modern version generates a signed JWT with JwtService.toToken().
     */
    private JsonObject buildUserResponse(User user, String sessionId) {
        JsonObject userObj = new JsonObject();
        userObj.addProperty("email", user.getEmail());
        userObj.addProperty("username", user.getUsername());
        userObj.addProperty("bio", user.getBio() != null ? user.getBio() : "");
        userObj.addProperty("image", user.getImage() != null ? user.getImage() : "");
        // Using session ID as "token" - this is not a real JWT
        // TODO: Implement proper JWT token generation
        userObj.addProperty("token", sessionId);
        return userObj;
    }
}
