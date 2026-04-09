package com.legacy.realworld.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.legacy.realworld.model.Article;
import com.legacy.realworld.repository.ArticleRepository;
import com.legacy.realworld.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Service layer for article business logic.
 * Orchestrates ArticleRepository calls and manages transactions.
 */
public class ArticleService {

    private final ArticleRepository articleRepo;

    public ArticleService(ArticleRepository articleRepo) {
        this.articleRepo = articleRepo;
    }

    public JsonObject createArticle(String title, String description, String body,
                                    String userId) throws SQLException {
        String id = UUID.randomUUID().toString();
        String slug = Article.toSlug(title);
        long now = new Date().getTime();

        articleRepo.save(id, slug, title, description, body, userId, now, now);
        return articleRepo.findByIdWithAuthor(id);
    }

    public JsonObject getArticle(String slug) throws SQLException {
        return articleRepo.findBySlugWithAuthor(slug);
    }

    public JsonObject listArticles(int limit, int offset) throws SQLException {
        JsonArray articles = articleRepo.findAllWithAuthor(limit, offset);
        int totalCount = articleRepo.countAll();

        JsonObject result = new JsonObject();
        result.add("articles", articles);
        result.addProperty("articlesCount", totalCount);
        return result;
    }

    public JsonObject updateArticle(String slug, String userId, String title,
                                    String description, String body) throws SQLException {
        int rowsUpdated = articleRepo.update(slug, userId, title, description, body);
        if (rowsUpdated == 0) {
            return null;
        }
        String newSlug = (title != null) ? Article.toSlug(title) : slug;
        return articleRepo.findBySlugWithAuthor(newSlug);
    }

    public boolean deleteArticle(String slug, String userId) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rowsDeleted = articleRepo.deleteBySlugAndUserId(conn, slug, userId);
                if (rowsDeleted > 0) {
                    articleRepo.deleteOrphanedComments(conn);
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
