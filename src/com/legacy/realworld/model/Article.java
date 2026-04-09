package com.legacy.realworld.model;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Article model class.
 * 
 * TODO: Consider using an ORM like Hibernate instead of manual mapping.
 * TODO: This class has way too many getters/setters - consider Lombok or records.
 * 
 * @author legacy-team
 * @since 1.0
 */
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                         .withZone(ZoneOffset.UTC);

    private String id;
    private String slug;
    private String title;
    private String description;
    private String body;
    private String userId;
    private Date createdAt;
    private Date updatedAt;

    // Dead code: was used for caching but never cleaned up
    // private transient String cachedJson = null;
    // private static int instanceCount = 0;

    public Article() {
        // Default constructor required for deserialization
        // instanceCount++;
    }

    public Article(String id, String slug, String title, String description, 
                   String body, String userId, Date createdAt, Date updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.body = body;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        // instanceCount++;
    }

    // --- Getters and Setters (manual, no Lombok) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getFormattedUpdatedAt() {
        if (updatedAt == null) {
            return null;
        }
        return DATE_FORMATTER.format(updatedAt.toInstant());
    }

    /**
     * Generate a URL-friendly slug from the title.
     * TODO: This doesn't handle Unicode properly.
     * TODO: No collision detection - two articles with the same title get the same slug.
     */
    public static String toSlug(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase()
                     .replaceAll("[^a-z0-9\\s-]", "")
                     .replaceAll("\\s+", "-")
                     .replaceAll("-+", "-")
                     .trim();
    }

    @Override
    public String toString() {
        return "Article{" +
                "id='" + id + '\'' +
                ", slug='" + slug + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    // Dead code: was part of a caching experiment that was abandoned
    /*
    public String toCachedJson() {
        if (cachedJson == null) {
            cachedJson = new Gson().toJson(this);
        }
        return cachedJson;
    }
    
    public void invalidateCache() {
        cachedJson = null;
    }
    
    public static int getInstanceCount() {
        return instanceCount;
    }
    */
}
