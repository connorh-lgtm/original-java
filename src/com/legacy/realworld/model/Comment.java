package com.legacy.realworld.model;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Comment model class.
 * 
 * TODO: No validation at all - body can be null or empty.
 * TODO: The modern version uses @NotBlank validation annotations.
 * 
 * @author legacy-team
 * @since 1.0
 */
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                         .withZone(ZoneOffset.UTC);

    private String id;
    private String body;
    private String userId;
    private String articleId;
    private Date createdAt;

    // Dead code: edit tracking was planned but never implemented
    // private Date updatedAt;
    // private boolean isEdited = false;
    // private int editCount = 0;

    public Comment() {
        // Default constructor
    }

    public Comment(String id, String body, String userId, String articleId, Date createdAt) {
        this.id = id;
        this.body = body;
        this.userId = userId;
        this.articleId = articleId;
        this.createdAt = createdAt;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns a formatted date string.
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return null;
        }
        return DATE_FORMATTER.format(createdAt.toInstant());
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", articleId='" + articleId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    // Dead code: edit functionality was abandoned mid-implementation
    /*
    public void markAsEdited() {
        this.isEdited = true;
        this.editCount++;
        this.updatedAt = new Date();
    }
    
    public boolean isEdited() {
        return isEdited;
    }
    
    public int getEditCount() {
        return editCount;
    }
    */
}
