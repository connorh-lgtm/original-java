package com.legacy.realworld.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.legacy.realworld.model.Article;
import com.legacy.realworld.util.DatabaseUtil;
import com.legacy.realworld.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Repository for article JDBC operations.
 * Extracted from ArticleServlet to separate data access from HTTP handling.
 */
public class ArticleRepository {

    public void save(String id, String slug, String title, String description,
                     String body, String userId, long createdAt, long updatedAt) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO articles (id, slug, title, description, body, user_id, created_at, updated_at) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, slug);
            stmt.setString(3, title);
            stmt.setString(4, description);
            stmt.setString(5, body);
            stmt.setString(6, userId);
            stmt.setLong(7, createdAt);
            stmt.setLong(8, updatedAt);
            stmt.executeUpdate();
        }
    }

    public JsonObject findBySlugWithAuthor(String slug) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT a.*, u.username, u.bio, u.image " +
                 "FROM articles a JOIN users u ON a.user_id = u.id " +
                 "WHERE a.slug = ?")) {
            stmt.setString(1, slug);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapArticleFromResultSet(rs);
                }
                return null;
            }
        }
    }

    public JsonObject findByIdWithAuthor(String id) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT a.*, u.username, u.bio, u.image " +
                 "FROM articles a JOIN users u ON a.user_id = u.id " +
                 "WHERE a.id = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapArticleFromResultSet(rs);
                }
                return null;
            }
        }
    }

    public JsonArray findAllWithAuthor(int limit, int offset) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT a.*, u.username, u.bio, u.image " +
                 "FROM articles a JOIN users u ON a.user_id = u.id " +
                 "ORDER BY a.created_at DESC " +
                 "LIMIT ? OFFSET ?")) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                JsonArray articles = new JsonArray();
                while (rs.next()) {
                    articles.add(mapArticleFromResultSet(rs));
                }
                return articles;
            }
        }
    }

    public int countAll() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM articles");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int update(String slug, String userId, String newTitle,
                      String newDescription, String newBody) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE articles SET updated_at = ?");
        int paramIndex = 2;

        if (newTitle != null) {
            sqlBuilder.append(", title = ?, slug = ?");
        }
        if (newDescription != null) {
            sqlBuilder.append(", description = ?");
        }
        if (newBody != null) {
            sqlBuilder.append(", body = ?");
        }

        sqlBuilder.append(" WHERE slug = ? AND user_id = ?");

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            stmt.setLong(1, new Date().getTime());

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

            return stmt.executeUpdate();
        }
    }

    public int deleteBySlugAndUserId(Connection conn, String slug, String userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM articles WHERE slug = ? AND user_id = ?")) {
            stmt.setString(1, slug);
            stmt.setString(2, userId);
            return stmt.executeUpdate();
        }
    }

    public void deleteOrphanedComments(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM comments WHERE article_id NOT IN (SELECT id FROM articles)")) {
            stmt.executeUpdate();
        }
    }

    private JsonObject mapArticleFromResultSet(ResultSet rs) throws SQLException {
        JsonObject article = new JsonObject();
        article.addProperty("id", rs.getString("id"));
        article.addProperty("slug", rs.getString("slug"));
        article.addProperty("title", rs.getString("title"));
        article.addProperty("description", rs.getString("description"));
        article.addProperty("body", rs.getString("body"));

        long createdAtMs = rs.getLong("created_at");
        long updatedAtMs = rs.getLong("updated_at");
        article.addProperty("createdAt", JsonUtil.formatDate(new Date(createdAtMs)));
        article.addProperty("updatedAt", JsonUtil.formatDate(new Date(updatedAtMs)));

        JsonObject author = new JsonObject();
        author.addProperty("username", rs.getString("username"));
        author.addProperty("bio", rs.getString("bio"));
        author.addProperty("image", rs.getString("image"));
        author.addProperty("following", false);
        article.add("author", author);

        article.addProperty("favorited", false);
        article.addProperty("favoritesCount", 0);
        article.add("tagList", new JsonArray());

        return article;
    }
}
