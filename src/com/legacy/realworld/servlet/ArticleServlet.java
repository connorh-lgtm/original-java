package com.legacy.realworld.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.legacy.realworld.model.Article;
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
 * Fat servlet that handles all article-related operations.
 * 
 * This servlet contains business logic, SQL queries, authentication checks,
 * and JSON serialization all in one class. This is the opposite of the
 * modern version's clean separation (Controller -> Service -> Repository).
 * 
 * TODO: This class is over 300 lines and does everything. Should be split into
 * separate service and repository layers.
 * 
 * @author legacy-team
 * @since 1.0
 */
public class ArticleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        // TODO: This is our "poor man's DI" - should use a real framework
        // ServiceLocator.register("articleServlet", this);
        System.out.println("ArticleServlet initialized");
    }

    /**
     * POST /api/articles - Create a new article
     * 
     * TODO: No input validation at all. Title could be null or empty.
     * The modern version uses @Valid @RequestBody with Bean Validation.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Manual auth check - repeated in every method
        // TODO: Should be a filter or interceptor, not copy-pasted code
        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Manual JSON parsing
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject articleJson = json.getAsJsonObject("article");

            String title = articleJson.get("title").getAsString();
            String description = articleJson.get("description").getAsString();
            String body = articleJson.get("body").getAsString();
            String slug = Article.toSlug(title);
            String id = UUID.randomUUID().toString();
            Date now = new Date();

            // Inline SQL - no ORM, no mapper, just raw JDBC
            // TODO: SQL injection risk? PreparedStatement helps but the query is still inline
            conn = DatabaseUtil.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO articles (id, slug, title, description, body, user_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, id);
            stmt.setString(2, slug);
            stmt.setString(3, title);
            stmt.setString(4, description);
            stmt.setString(5, body);
            stmt.setString(6, userId);
            stmt.setLong(7, now.getTime());
            stmt.setLong(8, now.getTime());
            stmt.executeUpdate();

            // Manual JSON response construction
            Article article = new Article(id, slug, title, description, body, userId, now, now);
            JsonObject responseJson = new JsonObject();
            responseJson.add("article", JsonUtil.toJsonObject(article));

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(responseJson.toString());

        } catch (SQLException e) {
            // TODO: This exposes internal error details to the client
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
            // Manual resource cleanup - no try-with-resources
            // TODO: Should use try-with-resources (Java 7+)
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        out.flush();
    }

    /**
     * GET /api/articles - List articles
     * GET /api/articles/{slug} - Get single article
     * 
     * TODO: No pagination limits - could return thousands of rows.
     * The modern version uses Page objects with offset/limit.
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
            conn = DatabaseUtil.getConnection();

            // Parse slug from URL path manually
            // TODO: This URL parsing is fragile and error-prone
            String pathInfo = request.getPathInfo();

            if (pathInfo != null && pathInfo.length() > 1) {
                // GET /api/articles/{slug} - single article
                String slug = pathInfo.substring(1);  // Remove leading "/"

                // Remove trailing path components (e.g., /comments)
                if (slug.contains("/")) {
                    slug = slug.substring(0, slug.indexOf("/"));
                }

                stmt = conn.prepareStatement(
                    "SELECT a.*, u.username, u.bio, u.image " +
                    "FROM articles a JOIN users u ON a.user_id = u.id " +
                    "WHERE a.slug = ?"
                );
                stmt.setString(1, slug);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    JsonObject responseJson = new JsonObject();
                    responseJson.add("article", mapArticleFromResultSet(rs));
                    out.print(responseJson.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Article not found");
                    out.print(error.toString());
                }
            } else {
                // GET /api/articles - list all articles
                // TODO: No pagination! This will be a problem with lots of articles.
                String offsetParam = request.getParameter("offset");
                String limitParam = request.getParameter("limit");
                int offset = (offsetParam != null) ? Integer.parseInt(offsetParam) : 0;
                int limit = (limitParam != null) ? Integer.parseInt(limitParam) : 20;

                // TODO: No parameterized author/tag/favorited filtering like the modern version
                stmt = conn.prepareStatement(
                    "SELECT a.*, u.username, u.bio, u.image " +
                    "FROM articles a JOIN users u ON a.user_id = u.id " +
                    "ORDER BY a.created_at DESC " +
                    "LIMIT ? OFFSET ?"
                );
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                rs = stmt.executeQuery();

                // Manual JSON array building
                JsonArray articlesArray = new JsonArray();
                int count = 0;
                while (rs.next()) {
                    articlesArray.add(mapArticleFromResultSet(rs));
                    count++;
                }

                JsonObject responseJson = new JsonObject();
                responseJson.add("articles", articlesArray);
                responseJson.addProperty("articlesCount", count);
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
            // Manual cleanup - same pattern repeated everywhere
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        out.flush();
    }

    /**
     * PUT /api/articles/{slug} - Update an article
     * 
     * TODO: No ownership check - any authenticated user can update any article!
     * The modern version uses AuthorizationService.canWriteArticle().
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Copy-pasted auth check again
        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article slug is required");
                out.print(error.toString());
                return;
            }
            String slug = pathInfo.substring(1);

            // Parse request body
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject articleJson = json.getAsJsonObject("article");

            // Build UPDATE query dynamically - this is messy
            // TODO: Should use a proper ORM or at least a query builder
            StringBuilder sqlBuilder = new StringBuilder("UPDATE articles SET updated_at = ?");
            int paramIndex = 2;

            String newTitle = null;
            String newDescription = null;
            String newBody = null;

            if (articleJson.has("title")) {
                newTitle = articleJson.get("title").getAsString();
                sqlBuilder.append(", title = ?, slug = ?");
            }
            if (articleJson.has("description")) {
                newDescription = articleJson.get("description").getAsString();
                sqlBuilder.append(", description = ?");
            }
            if (articleJson.has("body")) {
                newBody = articleJson.get("body").getAsString();
                sqlBuilder.append(", body = ?");
            }

            sqlBuilder.append(" WHERE slug = ? AND user_id = ?");

            conn = DatabaseUtil.getConnection();
            stmt = conn.prepareStatement(sqlBuilder.toString());

            Date now = new Date();
            stmt.setLong(1, now.getTime());

            if (newTitle != null) {
                stmt.setString(paramIndex++, newTitle);
                stmt.setString(paramIndex++, Article.toSlug(newTitle));
            }
            if (newDescription != null) {
                stmt.setString(paramIndex++, newDescription);
            }
            if (newBody != null) {
                stmt.setString(paramIndex++, newBody);
            }

            stmt.setString(paramIndex++, slug);
            stmt.setString(paramIndex++, userId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                // Re-fetch the updated article
                // TODO: This is an extra database round-trip. Could return the data more efficiently.
                stmt.close();
                String newSlug = (newTitle != null) ? Article.toSlug(newTitle) : slug;
                stmt = conn.prepareStatement(
                    "SELECT a.*, u.username, u.bio, u.image " +
                    "FROM articles a JOIN users u ON a.user_id = u.id " +
                    "WHERE a.slug = ?"
                );
                stmt.setString(1, newSlug);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    JsonObject responseJson = new JsonObject();
                    responseJson.add("article", mapArticleFromResultSet(rs));
                    out.print(responseJson.toString());
                }
                rs.close();
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found or not authorized");
                out.print(error.toString());
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
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        out.flush();
    }

    /**
     * DELETE /api/articles/{slug} - Delete an article
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Same auth check copy-pasted yet again
        if (!AuthUtil.isAuthenticated(request)) {
            AuthUtil.sendUnauthorized(response);
            return;
        }

        String userId = AuthUtil.getCurrentUserId(request);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article slug is required");
                out.print(error.toString());
                return;
            }
            String slug = pathInfo.substring(1);

            conn = DatabaseUtil.getConnection();

            // Delete comments first (no cascade delete configured)
            // TODO: Should use ON DELETE CASCADE in the schema
            stmt = conn.prepareStatement(
                "DELETE FROM comments WHERE article_id IN " +
                "(SELECT id FROM articles WHERE slug = ? AND user_id = ?)"
            );
            stmt.setString(1, slug);
            stmt.setString(2, userId);
            stmt.executeUpdate();
            stmt.close();

            // Now delete the article
            stmt = conn.prepareStatement(
                "DELETE FROM articles WHERE slug = ? AND user_id = ?"
            );
            stmt.setString(1, slug);
            stmt.setString(2, userId);

            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found or not authorized");
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
     * Map a ResultSet row to a JSON object.
     * TODO: This manual mapping is tedious and error-prone.
     * The modern version uses MyBatis mappers or JPA entity mapping.
     */
    private JsonObject mapArticleFromResultSet(ResultSet rs) throws SQLException {
        JsonObject article = new JsonObject();
        article.addProperty("id", rs.getString("id"));
        article.addProperty("slug", rs.getString("slug"));
        article.addProperty("title", rs.getString("title"));
        article.addProperty("description", rs.getString("description"));
        article.addProperty("body", rs.getString("body"));

        // TODO: This date formatting is not thread-safe
        long createdAtMs = rs.getLong("created_at");
        long updatedAtMs = rs.getLong("updated_at");
        article.addProperty("createdAt", JsonUtil.formatDate(new Date(createdAtMs)));
        article.addProperty("updatedAt", JsonUtil.formatDate(new Date(updatedAtMs)));

        // Inline author object construction
        JsonObject author = new JsonObject();
        author.addProperty("username", rs.getString("username"));
        author.addProperty("bio", rs.getString("bio"));
        author.addProperty("image", rs.getString("image"));
        author.addProperty("following", false);  // TODO: Not implemented
        article.add("author", author);

        article.addProperty("favorited", false);    // TODO: Not implemented
        article.addProperty("favoritesCount", 0);   // TODO: Not implemented
        article.add("tagList", new JsonArray());     // TODO: Not implemented

        return article;
    }
}
