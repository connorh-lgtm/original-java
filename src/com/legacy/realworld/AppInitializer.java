package com.legacy.realworld;

import com.legacy.realworld.repository.ArticleRepository;
import com.legacy.realworld.repository.CommentRepository;
import com.legacy.realworld.repository.UserRepository;
import com.legacy.realworld.service.ArticleService;
import com.legacy.realworld.service.CommentService;
import com.legacy.realworld.service.UserService;
import com.legacy.realworld.util.DatabaseUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Application composition root.
 *
 * Runs once when the web application starts (before any servlet is loaded).
 * Responsibilities:
 *   1. Initialize the database schema via {@link DatabaseUtil#initializeDatabase()}.
 *   2. Wire all repository and service instances.
 *   3. Publish the services as {@link ServletContext} attributes so servlets
 *      can retrieve them in their {@code init()} methods.
 *
 * This replaces the old {@code ServiceLocator} (static HashMap with string-based
 * lookup) and moves object creation out of individual servlet {@code init()} methods.
 */
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DatabaseUtil.initializeDatabase();

        ArticleRepository articleRepo = new ArticleRepository();
        UserRepository userRepo = new UserRepository();
        CommentRepository commentRepo = new CommentRepository();

        ArticleService articleService = new ArticleService(articleRepo);
        UserService userService = new UserService(userRepo);
        CommentService commentService = new CommentService(commentRepo);

        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute("articleService", articleService);
        ctx.setAttribute("userService", userService);
        ctx.setAttribute("commentService", commentService);

        System.out.println("AppInitializer: all services wired and registered");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup is handled by AppContextListener (shuts down the connection pool).
    }
}
