package com.legacy.realworld.filter;

import com.legacy.realworld.util.AuthUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (requiresAuth(request)) {
            if (!AuthUtil.isAuthenticated(request)) {
                AuthUtil.sendUnauthorized(response);
                return;
            }
            request.setAttribute("userId", AuthUtil.getCurrentUserId(request));
        }

        chain.doFilter(request, response);
    }

    private boolean requiresAuth(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Public: POST /api/users (register), POST /api/users/login
        if (path.startsWith("/api/users") && "POST".equals(method)) {
            return false;
        }
        // Public: GET /api/articles
        if (path.startsWith("/api/articles") && "GET".equals(method)) {
            return false;
        }
        // Public: GET /api/comments
        if (path.startsWith("/api/comments") && "GET".equals(method)) {
            return false;
        }

        return path.startsWith("/api/");
    }

    @Override
    public void destroy() {}
}
