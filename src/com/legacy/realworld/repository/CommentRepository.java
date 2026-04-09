package com.legacy.realworld.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.legacy.realworld.util.DatabaseUtil;
import com.legacy.realworld.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Repository for comment JDBC operations.
 * Extracted from CommentServlet to separate data access from HTTP handling.
 */
public class CommentRepository {

    public String findArticleIdBySlug(String slug) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM articles WHERE slug = ?")) {
            stmt.setString(1, slug);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
                return null;
            }
        }
    }

    public void save(String id, String body, String userId, String articleId,
                     long createdAt) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO comments (id, body, user_id, article_id, created_at) " +
                 "VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, body);
            stmt.setString(3, userId);
            stmt.setString(4, articleId);
            stmt.setLong(5, createdAt);
            stmt.executeUpdate();
        }
    }

    public JsonObject findByIdWithAuthor(String commentId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT c.*, u.username, u.bio, u.image " +
                 "FROM comments c JOIN users u ON c.user_id = u.id " +
                 "WHERE c.id = ?")) {
            stmt.setString(1, commentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCommentFromResultSet(rs);
                }
                return null;
            }
        }
    }

    public JsonArray findByArticleIdWithAuthor(String articleId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT c.*, u.username, u.bio, u.image " +
                 "FROM comments c " +
                 "JOIN users u ON c.user_id = u.id " +
                 "WHERE c.article_id = ? " +
                 "ORDER BY c.created_at DESC")) {
            stmt.setString(1, articleId);
            try (ResultSet rs = stmt.executeQuery()) {
                JsonArray comments = new JsonArray();
                while (rs.next()) {
                    comments.add(mapCommentFromResultSet(rs));
                }
                return comments;
            }
        }
    }

    public int deleteByIdAndUserId(String commentId, String userId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM comments WHERE id = ? AND user_id = ?")) {
            stmt.setString(1, commentId);
            stmt.setString(2, userId);
            return stmt.executeUpdate();
        }
    }

    private JsonObject mapCommentFromResultSet(ResultSet rs) throws SQLException {
        JsonObject comment = new JsonObject();
        comment.addProperty("id", rs.getString("id"));
        comment.addProperty("body", rs.getString("body"));
        comment.addProperty("createdAt",
            JsonUtil.formatDate(new Date(rs.getLong("created_at"))));

        JsonObject author = new JsonObject();
        author.addProperty("username", rs.getString("username"));
        author.addProperty("bio", rs.getString("bio"));
        author.addProperty("image", rs.getString("image"));
        author.addProperty("following", false);
        comment.add("author", author);

        return comment;
    }
}
