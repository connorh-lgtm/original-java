package com.legacy.realworld.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.legacy.realworld.service.CommentService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Servlet that handles all comment HTTP operations.
 * Business logic is delegated to CommentService; data access to CommentRepository.
 *
 * Mapped to /api/comments/* because the Servlet spec doesn't support
 * mid-path wildcards like /api/articles/{slug}/comments/{id}.
 * The ArticleServlet at /api/articles/* forwards comment requests here
 * via RequestDispatcher when it detects /comments/ in the path.
 *
 * @author legacy-team
 * @since 1.0
 */
public class CommentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private CommentService commentService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.commentService = (CommentService) getServletContext().getAttribute("commentService");
        System.out.println("CommentServlet initialized");
    }

    /**
     * POST /api/articles/{slug}/comments - Create a comment
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = (String) request.getAttribute("userId");

        try {
            String slug = extractSlugFromPath(request);
            if (slug == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid URL - article slug required");
                out.print(error.toString());
                return;
            }

            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject commentJson = json.getAsJsonObject("comment");
            String body = commentJson.get("body").getAsString();

            JsonObject comment = commentService.createComment(slug, body, userId);

            if (comment != null) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("comment", comment);
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.print(responseJson.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found");
                out.print(error.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal error: " + e.getMessage());
            out.print(error.toString());
        }

        out.flush();
    }

    /**
     * GET /api/articles/{slug}/comments - List comments for article
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String slug = extractSlugFromPath(request);
            if (slug == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid URL - article slug required");
                out.print(error.toString());
                return;
            }

            JsonArray comments = commentService.getComments(slug);

            if (comments != null) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("comments", comments);
                out.print(responseJson.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found");
                out.print(error.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        }

        out.flush();
    }

    /**
     * DELETE /api/articles/{slug}/comments/{id} - Delete a comment
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = (String) request.getAttribute("userId");

        try {
            String commentId = extractCommentIdFromPath(request);
            if (commentId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Comment ID required");
                out.print(error.toString());
                return;
            }

            int rowsDeleted = commentService.deleteComment(commentId, userId);

            if (rowsDeleted > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Comment not found or not authorized");
                out.print(error.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Database error: " + e.getMessage());
            out.print(error.toString());
        }

        out.flush();
    }

    /**
     * Extract article slug from the request path.
     *
     * TODO: This is incredibly brittle. Any change to URL structure breaks this.
     * The modern version uses @PathVariable("slug") which is declarative and robust.
     */
    private String extractSlugFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String path = pathInfo.substring(1);
            if (path.contains("/")) {
                return path.substring(0, path.indexOf("/"));
            }
            return path;
        }

        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("articles".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /**
     * Extract comment ID from the request path.
     *
     * TODO: Same brittleness as extractSlugFromPath
     */
    private String extractCommentIdFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String path = pathInfo.substring(1);
            if (path.contains("/")) {
                return path.substring(path.indexOf("/") + 1);
            }
        }

        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("comments".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
