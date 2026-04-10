package com.legacy.realworld.servlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.legacy.realworld.service.ArticleService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Servlet that handles all article-related HTTP operations.
 * Business logic is delegated to ArticleService; data access to ArticleRepository.
 *
 * @author legacy-team
 * @since 1.0
 */
public class ArticleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ArticleService articleService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.articleService = (ArticleService) getServletContext().getAttribute("articleService");
        System.out.println("ArticleServlet initialized");
    }

    /**
     * Check if the request is actually for the /comments sub-resource and forward
     * to CommentServlet if so. This is needed because the Servlet spec doesn't
     * support mid-path wildcards like /api/articles/{slug}/comments/{id}.
     *
     * TODO: This forwarding hack is fragile. A front-controller pattern or
     * a framework like Spring MVC would handle this cleanly.
     *
     * @return true if the request was forwarded (caller should return immediately)
     */
    private boolean forwardIfCommentRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/[^/]+/comments(/.*)?$")) {
            String slug = pathInfo.substring(1);
            if (slug.contains("/")) {
                slug = slug.substring(0, slug.indexOf("/"));
            }
            String remainder = pathInfo.substring(pathInfo.indexOf("/comments") + "/comments".length());
            String forwardPath = "/api/comments/" + slug + remainder;
            RequestDispatcher dispatcher = request.getRequestDispatcher(forwardPath);
            dispatcher.forward(request, response);
            return true;
        }
        return false;
    }

    /**
     * POST /api/articles - Create a new article
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (forwardIfCommentRequest(request, response)) return;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = (String) request.getAttribute("userId");

        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject articleJson = json.getAsJsonObject("article");

            String title = articleJson.get("title").getAsString();
            String description = articleJson.get("description").getAsString();
            String body = articleJson.get("body").getAsString();

            JsonObject article = articleService.createArticle(title, description, body, userId);

            if (article != null) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("article", article);
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.print(responseJson.toString());
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
     * GET /api/articles - List articles
     * GET /api/articles/{slug} - Get single article
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (forwardIfCommentRequest(request, response)) return;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String pathInfo = request.getPathInfo();

            if (pathInfo != null && pathInfo.length() > 1) {
                // GET /api/articles/{slug} - single article
                String slug = pathInfo.substring(1);
                if (slug.contains("/")) {
                    slug = slug.substring(0, slug.indexOf("/"));
                }

                JsonObject article = articleService.getArticle(slug);

                if (article != null) {
                    JsonObject responseJson = new JsonObject();
                    responseJson.add("article", article);
                    out.print(responseJson.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Article not found");
                    out.print(error.toString());
                }
            } else {
                // GET /api/articles - list all articles
                String offsetParam = request.getParameter("offset");
                String limitParam = request.getParameter("limit");
                int offset = (offsetParam != null) ? Integer.parseInt(offsetParam) : 0;
                int limit = (limitParam != null) ? Integer.parseInt(limitParam) : 20;

                JsonObject result = articleService.listArticles(limit, offset);
                out.print(result.toString());
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
     * PUT /api/articles/{slug} - Update an article
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = (String) request.getAttribute("userId");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article slug is required");
                out.print(error.toString());
                return;
            }
            String slug = pathInfo.substring(1);

            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonObject articleJson = json.getAsJsonObject("article");

            String newTitle = articleJson.has("title") ? articleJson.get("title").getAsString() : null;
            String newDescription = articleJson.has("description") ? articleJson.get("description").getAsString() : null;
            String newBody = articleJson.has("body") ? articleJson.get("body").getAsString() : null;

            JsonObject article = articleService.updateArticle(slug, userId, newTitle, newDescription, newBody);

            if (article != null) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("article", article);
                out.print(responseJson.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found or not authorized");
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
     * DELETE /api/articles/{slug} - Delete an article
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (forwardIfCommentRequest(request, response)) return;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = (String) request.getAttribute("userId");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article slug is required");
                out.print(error.toString());
                return;
            }
            String slug = pathInfo.substring(1);

            boolean deleted = articleService.deleteArticle(slug, userId);

            if (deleted) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Article not found or not authorized");
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
}
