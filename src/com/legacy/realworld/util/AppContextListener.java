package com.legacy.realworld.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Application lifecycle listener that manages shared resources.
 * Ensures the HikariCP connection pool is cleanly shut down on undeploy.
 */
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Pool is initialized statically in DatabaseUtil; nothing extra needed here.
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DatabaseUtil.shutdown();
    }
}
