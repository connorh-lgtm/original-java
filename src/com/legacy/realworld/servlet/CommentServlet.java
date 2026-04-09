package com.legacy.realworld.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.legacy.realworld.model.Comment;
import com.legacy.realworld.util.AuthUtil;
import com.legacy.realworld.util.DatabaseUtil;
import com.legacy.realworld.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Fat servlet that handles all comment operations.
 * 
 * Same problems as ArticleServlet: inline SQL, manual auth checks,
 * manual JSON construction, no separation of concerns.
 * 
 * URL pattern: /api/articles/{slug}/comments
 * URL pattern: /api/articles/{slug}/comments/{id}
 * 
 * TODO: The URL parsing is especially painful here because we need to
 * extract both the article slug AND the comment ID from the path.
 * The modern version uses @PathVariable which handles this cleanly.
 * 
 * @author legacy-team
 * @since 1.0
 */
public class CommentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("CommentServlet initialized");
    }

    /**
     * POST /api/articles/{slug}/comments - Create a comment
     * 
     * TODO: No validation on comment body - could be empty or null.
     * The modern version uses @Valid @NotBlank annotations.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Auth check copy-paste #3
        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Parse slug from URL path manually
            // URL: /api/articles/{slug}/comments
            // TODO: This parsing is extremely fragile
            String slug = extractSlugFromPath(request);
            if (slug == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid URL - article slug required");
                out.print(error.toString());
                return;
            }

            conn = DatabaseUtil.getConnection();

            // First, find the article by slug
            stmt = conn.prepareStatement("SELECT id FROM articles WHERE slug = ?");
            stmt.setString(1, slug);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found");
                out.print(error.toString());
                return;
            }

            String articleId = rs.getString("id");
            rs.close();
            stmt.close();

            // Parse request body
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject commentJson = json.getAsJsonObject("comment");
            String body = commentJson.get("body").getAsString();

            String commentId = UUID.randomUUID().toString();
            Date now = new Date();

            // Inline SQL INSERT
            stmt = conn.prepareStatement(
                "INSERT INTO comments (id, body, user_id, article_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, commentId);
            stmt.setString(2, body);
            stmt.setString(3, userId);
            stmt.setString(4, articleId);
            stmt.setLong(5, now.getTime());
            stmt.executeUpdate();
            stmt.close();

            // Fetch the comment with author info for response
            stmt = conn.prepareStatement(
                "SELECT c.*, u.username, u.bio, u.image " +
                "FROM comments c JOIN users u ON c.user_id = u.id " +
                "WHERE c.id = ?"
            );
            stmt.setString(1, commentId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("comment", mapCommentFromResultSet(rs));
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.print(responseJson.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal error: " + e.getMessage());
            out.print(error.toString());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        out.flush();
    }

    /**
     * GET /api/articles/{slug}/comments - List comments for article
     * 
     * TODO: No pagination on comments. Could return thousands of results.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String slug = extractSlugFromPath(request);
            if (slug == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid URL - article slug required");
                out.print(error.toString());
                return;
            }

            conn = DatabaseUtil.getConnection();

            // First find the article
            stmt = conn.prepareStatement("SELECT id FROM articles WHERE slug = ?");
            stmt.setString(1, slug);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found");
                out.print(error.toString());
                return;
            }

            String articleId = rs.getString("id");
            rs.close();
            stmt.close();

            // Fetch all comments with author info via JOIN
            // TODO: No pagination, no ordering options
            stmt = conn.prepareStatement(
                "SELECT c.*, u.username, u.bio, u.image " +
                "FROM comments c " +
                "JOIN users u ON c.user_id = u.id " +
                "WHERE c.article_id = ? " +
                "ORDER BY c.created_at DESC"
            );
            stmt.setString(1, articleId);
            rs = stmt.executeQuery();

            // Manual JSON array building
            JsonArray commentsArray = new JsonArray();
            while (rs.next()) {
                commentsArray.add(mapCommentFromResultSet(rs));
            }

            JsonObject responseJson = new JsonObject();
            responseJson.add("comments", commentsArray);
            out.print(responseJson.toString());

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
     * DELETE /api/articles/{slug}/comments/{id} - Delete a comment
     * 
     * TODO: Only checks if the user owns the comment, not if they own the article.
     * The modern version uses AuthorizationService.canWriteComment() which checks both.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Auth check #4 - same pattern everywhere
        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Extract comment ID from URL path
            // URL: /api/articles/{slug}/comments/{id}
            String commentId = extractCommentIdFromPath(request);
            if (commentId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Comment ID required");
                out.print(error.toString());
                return;
            }

            conn = DatabaseUtil.getConnection();

            // Delete the comment - only if owned by current user
            // TODO: Should also allow article owner to delete comments
            stmt = conn.prepareStatement(
                "DELETE FROM comments WHERE id = ? AND user_id = ?"
            );
            stmt.setString(1, commentId);
            stmt.setString(2, userId);

            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Comment not found or not authorized");
                out.print(error.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        out.flush();
    }

    /**
     * Extract article slug from the request path.
     * Expected path patterns:
     *   /api/articles/{slug}/comments
     *   /api/articles/{slug}/comments/{id}
     * 
     * The servlet is mapped to /api/articles/*, so pathInfo starts after that.
     * We need to look at the full request URI.
     * 
     * TODO: This is incredibly brittle. Any change to URL structure breaks this.
     * The modern version uses @PathVariable("slug") which is declarative and robust.
     */
    private String extractSlugFromPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Expected: /api/articles/{slug}/comments or /api/articles/{slug}/comments/{id}
        String[] parts = uri.split("/");
        // parts: ["", "api", "articles", "{slug}", "comments", ...]
        for (int i = 0; i < parts.length; i++) {
            if ("articles".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /**
     * Extract comment ID from the request path.
     * Expected: /api/articles/{slug}/comments/{id}
     * 
     * TODO: Same brittleness as extractSlugFromPath
     */
    private String extractCommentIdFromPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        // parts: ["", "api", "articles", "{slug}", "comments", "{id}"]
        for (int i = 0; i < parts.length; i++) {
            if ("comments".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /**
     * Map a ResultSet row to a comment JSON object.
     */
    private JsonObject mapCommentFromResultSet(ResultSet rs) throws SQLException {
        JsonObject comment = new JsonObject();
        comment.addProperty("id", rs.getString("id"));
        comment.addProperty("body", rs.getString("body"));
        comment.addProperty("createdAt", 
            JsonUtil.formatDate(new Date(rs.getLong("created_at"))));

        // Inline author object
        JsonObject author = new JsonObject();
        author.addProperty("username", rs.getString("username"));
        author.addProperty("bio", rs.getString("bio"));
        author.addProperty("image", rs.getString("image"));
        author.addProperty("following", false);  // TODO: Not implemented
        comment.add("author", author);

        return comment;
    }
}
