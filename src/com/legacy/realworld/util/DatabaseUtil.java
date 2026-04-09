package com.legacy.realworld.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Static database utility class.
 * 
 * Anti-patterns in this file:
 * - No connection pooling (new connection every time)
 * - Static methods everywhere (hard to test, hard to mock)
 * - Class.forName() driver loading (pre-JDBC 4.0 pattern)
 * - Inline DDL statements
 * - No migrations (schema changes require manual intervention)
 * 
 * Modern equivalent:
 * - Spring Boot auto-configured DataSource with HikariCP connection pool
 * - Flyway migrations for schema management
 * - MyBatis mappers for SQL abstraction
 * 
 * @author legacy-team
 * @since 1.0
 */
public class DatabaseUtil {

    private static final HikariDataSource dataSource;
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:realworld.db");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        dataSource = new HikariDataSource(config);
        System.out.println("HikariCP connection pool initialized");
    }

    /**
     * Get a database connection from the HikariCP pool.
     * 
     * @return a pooled database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shut down the HikariCP connection pool.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool shut down");
        }
    }

    /**
     * Initialize the database schema.
     * 
     * NOTE: Existing plain-text passwords in the DB need a one-time migration
     * to re-hash them with BCrypt, or users with old passwords must be forced
     * to reset their passwords. Until migrated, those accounts cannot log in.
     * 
     * TODO: No migration framework! Schema changes require manual coordination.
     * The modern version uses Flyway with versioned migration scripts.
     * TODO: CREATE TABLE IF NOT EXISTS masks schema drift issues.
     * TODO: No indexes defined - queries will be slow on large datasets.
     */
    public static void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // Create users table
            // TODO: No password hashing column type hint. Password stored as plain VARCHAR.
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    id TEXT PRIMARY KEY," +
                "    email TEXT UNIQUE NOT NULL," +
                "    username TEXT UNIQUE NOT NULL," +
                "    password TEXT NOT NULL," +  // PLAIN TEXT! Should be hashed!
                "    bio TEXT DEFAULT ''," +
                "    image TEXT DEFAULT ''" +
                ")"
            );

            // Create articles table
            // TODO: No foreign key constraint enforcement in SQLite by default!
            // Need PRAGMA foreign_keys = ON; but we never set it.
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS articles (" +
                "    id TEXT PRIMARY KEY," +
                "    slug TEXT UNIQUE NOT NULL," +
                "    title TEXT NOT NULL," +
                "    description TEXT," +
                "    body TEXT," +
                "    user_id TEXT NOT NULL," +
                "    created_at INTEGER NOT NULL," +
                "    updated_at INTEGER NOT NULL," +
                "    FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")"
            );

            // Create comments table
            // TODO: No ON DELETE CASCADE - orphaned comments if article is deleted
            // (We handle this manually in ArticleServlet.doDelete, but it's error-prone)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS comments (" +
                "    id TEXT PRIMARY KEY," +
                "    body TEXT NOT NULL," +
                "    user_id TEXT NOT NULL," +
                "    article_id TEXT NOT NULL," +
                "    created_at INTEGER NOT NULL," +
                "    FOREIGN KEY (user_id) REFERENCES users(id)," +
                "    FOREIGN KEY (article_id) REFERENCES articles(id)" +
                ")"
            );

            // TODO: Should add indexes for common queries
            // stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_slug ON articles(slug)");
            // stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_user ON articles(user_id)");
            // stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_comments_article ON comments(article_id)");

            System.out.println("Database initialized successfully");

        } catch (SQLException e) {
            // TODO: This swallows the exception! App will start but with no tables.
            System.err.println("ERROR: Failed to initialize database!");
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Convenience method to close resources.
     * TODO: Should use try-with-resources instead of this helper.
     * 
     * Dead code: this method exists but most servlets do their own cleanup
     * because they were written before this utility method was added.
     */
    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    // Swallow the exception - "quietly"
                    // TODO: At least log this somewhere
                }
            }
        }
    }

    // Prevent instantiation
    private DatabaseUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
