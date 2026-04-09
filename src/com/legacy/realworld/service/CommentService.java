package com.legacy.realworld.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.legacy.realworld.repository.CommentRepository;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

/**
 * Service layer for comment business logic.
 * Orchestrates CommentRepository calls.
 */
public class CommentService {

    private final CommentRepository commentRepo;

    public CommentService(CommentRepository commentRepo) {
        this.commentRepo = commentRepo;
    }

    /**
     * Create a comment on an article identified by slug.
     * Returns the created comment JSON, or null if article not found.
     */
    public JsonObject createComment(String slug, String body, String userId) throws SQLException {
        String articleId = commentRepo.findArticleIdBySlug(slug);
        if (articleId == null) {
            return null;
        }

        String commentId = UUID.randomUUID().toString();
        long now = new Date().getTime();
        commentRepo.save(commentId, body, userId, articleId, now);

        return commentRepo.findByIdWithAuthor(commentId);
    }

    /**
     * Get all comments for an article identified by slug.
     * Returns the comments array, or null if article not found.
     */
    public JsonArray getComments(String slug) throws SQLException {
        String articleId = commentRepo.findArticleIdBySlug(slug);
        if (articleId == null) {
            return null;
        }
        return commentRepo.findByArticleIdWithAuthor(articleId);
    }

    public int deleteComment(String commentId, String userId) throws SQLException {
        return commentRepo.deleteByIdAndUserId(commentId, userId);
    }
}
