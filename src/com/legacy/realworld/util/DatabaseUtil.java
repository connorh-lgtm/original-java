package com.legacy.realworld.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

    // TODO: This should not be hardcoded. Should come from JNDI or properties file.
    private static final String DB_URL = "jdbc:sqlite:realworld.db";

    // Dead code: connection counting was used for debugging a leak, left behind
    // private static int connectionCount = 0;
    // private static final int MAX_CONNECTIONS = 50;

    // Static initializer block to load the JDBC driver
    // TODO: This is the pre-JDBC 4.0 way. Modern drivers auto-register.
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            // TODO: This just prints and continues - the app will fail later
            // with a confusing "no suitable driver" error
            System.err.println("ERROR: SQLite JDBC driver not found!");
            System.err.println("Make sure sqlite-jdbc-3.20.0.jar is in lib/");
            e.printStackTrace();
        }
    }

    /**
     * Get a new database connection.
     * 
     * TODO: NO CONNECTION POOLING! Every call creates a new connection.
     * This is extremely inefficient under load. The modern version uses
     * HikariCP which maintains a pool of reusable connections.
     * 
     * TODO: Connection is never returned to a pool - caller must close it.
     * If they forget, we leak connections until the database locks up.
     * 
     * @return a new database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        // connectionCount++;
        // if (connectionCount > MAX_CONNECTIONS) {
        //     System.err.println("WARNING: Over " + MAX_CONNECTIONS + " connections opened!");
        // }
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialize the database schema.
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
