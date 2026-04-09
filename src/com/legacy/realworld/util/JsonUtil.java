package com.legacy.realworld.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.legacy.realworld.model.Article;
import com.legacy.realworld.model.Comment;
import com.legacy.realworld.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Static JSON utility class for manual serialization/deserialization.
 * 
 * Anti-patterns:
 * - Static SimpleDateFormat (not thread-safe!)
 * - Manual toJson/fromJson methods for each model
 * - No generic serialization strategy
 * - Password included in User JSON by default (security risk)
 * 
 * Modern equivalent:
 * - Jackson with Spring Boot auto-configuration
 * - @JsonIgnore for sensitive fields
 * - Custom serializers registered once
 * - java.time.Instant with ISO-8601 formatting
 * 
 * @author legacy-team
 * @since 1.0
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // TODO: THIS IS NOT THREAD-SAFE!
    // SimpleDateFormat is not synchronized. If two threads call formatDate()
    // at the same time, the output will be corrupted.
    // The modern version uses java.time.Instant which is immutable and thread-safe.
    private static final SimpleDateFormat ISO_DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // Dead code: alternate formats that were tried and abandoned
    // private static final SimpleDateFormat US_DATE_FORMAT = 
    //     new SimpleDateFormat("MM/dd/yyyy hh:mm a");
    // private static final SimpleDateFormat EU_DATE_FORMAT = 
    //     new SimpleDateFormat("dd.MM.yyyy HH:mm");

    /**
     * Format a Date to ISO-8601 string.
     * 
     * TODO: THIS IS NOT THREAD-SAFE! Two threads calling this simultaneously
     * will produce corrupted output because SimpleDateFormat.format() modifies
     * internal state.
     * 
     * Minimal fix: synchronize this method or use ThreadLocal<SimpleDateFormat>
     * Better fix: use java.time.Instant (Java 8+)
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        // TODO: This is not thread-safe!
        return ISO_DATE_FORMAT.format(date);
    }

    /**
     * Convert an Article to a JsonObject.
     * 
     * TODO: Manual field-by-field serialization. Tedious and error-prone.
     * If we add a field to Article, we have to remember to update this method.
     * The modern version uses Jackson annotations on the DTO classes.
     */
    public static JsonObject toJsonObject(Article article) {
        JsonObject json = new JsonObject();
        json.addProperty("id", article.getId());
        json.addProperty("slug", article.getSlug());
        json.addProperty("title", article.getTitle());
        json.addProperty("description", article.getDescription());
        json.addProperty("body", article.getBody());
        json.addProperty("createdAt", formatDate(article.getCreatedAt()));
        json.addProperty("updatedAt", formatDate(article.getUpdatedAt()));
        json.addProperty("favorited", false);    // TODO: Not implemented
        json.addProperty("favoritesCount", 0);   // TODO: Not implemented
        // tagList not included - TODO: implement tags
        return json;
    }

    /**
     * Convert a User to a JsonObject.
     * 
     * WARNING: This includes the password field!
     * TODO: SECURITY RISK - password should never be serialized.
     * The modern version uses @JsonIgnore on the password field.
     */
    public static JsonObject toJsonObject(User user) {
        JsonObject json = new JsonObject();
        json.addProperty("email", user.getEmail());
        json.addProperty("username", user.getUsername());
        // TODO: SECURITY - Do NOT include password in JSON response!
        // json.addProperty("password", user.getPassword());
        json.addProperty("bio", user.getBio() != null ? user.getBio() : "");
        json.addProperty("image", user.getImage() != null ? user.getImage() : "");
        return json;
    }

    /**
     * Convert a Comment to a JsonObject.
     */
    public static JsonObject toJsonObject(Comment comment) {
        JsonObject json = new JsonObject();
        json.addProperty("id", comment.getId());
        json.addProperty("body", comment.getBody());
        json.addProperty("createdAt", formatDate(comment.getCreatedAt()));
        return json;
    }

    /**
     * Parse a JSON string into an Article object.
     * 
     * TODO: No validation. Missing fields become null silently.
     * The modern version uses @Valid with Bean Validation constraints.
     */
    public static Article parseArticle(String json) {
        try {
            return GSON.fromJson(json, Article.class);
        } catch (Exception e) {
            // TODO: Swallowing the parse exception - caller won't know why it failed
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse a JSON string into a User object.
     * 
     * TODO: This will also deserialize the password field if present in JSON.
     */
    public static User parseUser(String json) {
        try {
            return GSON.fromJson(json, User.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generic toJson - but we don't use this because we have manual methods above.
     * Dead code that someone added "for future use".
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    // Prevent instantiation
    private JsonUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
