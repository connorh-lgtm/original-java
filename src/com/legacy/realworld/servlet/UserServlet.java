package com.legacy.realworld.servlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.legacy.realworld.model.User;
import com.legacy.realworld.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Servlet that handles user registration, login, and current-user retrieval.
 * Business logic is delegated to UserService; data access to UserRepository.
 *
 * @author legacy-team
 * @since 1.0
 */
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = (UserService) getServletContext().getAttribute("userService");
        System.out.println("UserServlet initialized");
    }

    /**
     * POST /api/users - Register new user
     * POST /api/users/login - Login existing user
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        boolean isLogin = (pathInfo != null && pathInfo.contains("login"));

        try {
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

    private void handleRegistration(JsonObject userJson, HttpServletRequest request,
                                     HttpServletResponse response, PrintWriter out)
            throws SQLException {

        String email = userJson.get("email").getAsString();
        String username = userJson.get("username").getAsString();
        String password = userJson.get("password").getAsString();

        UserService.RegistrationResult result = userService.register(email, username, password);

        if (!result.isSuccess()) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            JsonObject error = new JsonObject();
            error.addProperty("error", result.getError());
            out.print(error.toString());
            return;
        }

        User user = result.getUser();

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());

        JsonObject responseJson = new JsonObject();
        responseJson.add("user", buildUserResponse(user, session.getId()));

        response.setStatus(HttpServletResponse.SC_CREATED);
        out.print(responseJson.toString());
    }

    private void handleLogin(JsonObject userJson, HttpServletRequest request,
                              HttpServletResponse response, PrintWriter out)
            throws SQLException {

        String email = userJson.get("email").getAsString();
        String password = userJson.get("password").getAsString();

        User user = userService.login(email, password);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Invalid email or password");
            out.print(error.toString());
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());

        JsonObject responseJson = new JsonObject();
        responseJson.add("user", buildUserResponse(user, session.getId()));
        out.print(responseJson.toString());
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

        String userId = (String) request.getAttribute("userId");

        try {
            User user = userService.getCurrentUser(userId);

            if (user != null) {
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
        }

        out.flush();
    }

    /**
     * Build user JSON response.
     * TODO: This includes the session ID as a "token" - not a real JWT.
     */
    private JsonObject buildUserResponse(User user, String sessionId) {
        JsonObject userObj = new JsonObject();
        userObj.addProperty("email", user.getEmail());
        userObj.addProperty("username", user.getUsername());
        userObj.addProperty("bio", user.getBio() != null ? user.getBio() : "");
        userObj.addProperty("image", user.getImage() != null ? user.getImage() : "");
        userObj.addProperty("token", sessionId);
        return userObj;
    }
}
