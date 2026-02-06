package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.Comment;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.dao.CommentDAO;
import com.smarttask.server.dao.TaskDAO;
import com.smarttask.server.dao.UserDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommentHandler implements HttpHandler {
    private final CommentDAO commentDAO = new CommentDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();
    // Au lieu de new Gson(), on utilise votre configurateur
    // On utilise le package server.util que vous venez de créer
    private final Gson gson = com.smarttask.server.util.GsonUtils.getGson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/comments/task/")) {
                    String taskId = path.substring("/api/comments/task/".length());
                    handleGetByTask(exchange, taskId);
                } else if (path.startsWith("/api/comments/")) {
                    String id = path.substring("/api/comments/".length());
                    handleGetById(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/comments")) {
                    handleCreate(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("PUT".equals(method)) {
                if (path.startsWith("/api/comments/")) {
                    String id = path.substring("/api/comments/".length());
                    handleUpdate(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/comments/")) {
                    String id = path.substring("/api/comments/".length());
                    handleDelete(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetByTask(HttpExchange exchange, String taskId) throws IOException {
        List<Comment> comments = commentDAO.findByTaskId(taskId);
        sendResponse(exchange, 200, gson.toJson(comments));
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        Comment c = commentDAO.findById(id).orElse(null);
        if (c == null) sendResponse(exchange, 404, "{\"error\":\"Comment not found\"}");
        else sendResponse(exchange, 200, gson.toJson(c));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        try {
            com.google.gson.JsonObject obj = gson.fromJson(body, com.google.gson.JsonObject.class);
            String taskId = null;
            String userId = null;
            if (obj.has("task") && obj.getAsJsonObject("task").has("id")) {
                taskId = obj.getAsJsonObject("task").get("id").getAsString();
            } else if (obj.has("taskId")) {
                taskId = obj.get("taskId").getAsString();
            }
            if (obj.has("user") && obj.getAsJsonObject("user").has("id")) {
                userId = obj.getAsJsonObject("user").get("id").getAsString();
            } else if (obj.has("userId")) {
                userId = obj.get("userId").getAsString();
            }
            String content = obj.has("content") ? obj.get("content").getAsString() : null;

            if (taskId == null || userId == null || content == null) {
                sendResponse(exchange, 400, "{\"error\":\"taskId, userId and content required\"}");
                return;
            }

            Comment c = new Comment();
            com.smarttask.model.Task t = new com.smarttask.model.Task();
            t.setId(taskId);
            com.smarttask.model.User u = new com.smarttask.model.User();
            u.setId(userId);
            c.setTask(t);
            c.setUser(u);
            c.setContent(content);
            c.touchOnCreate();

            String id = commentDAO.save(c);
            c.setId(id);
            sendResponse(exchange, 201, gson.toJson(c));
        } catch (com.google.gson.JsonSyntaxException ex) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid JSON\"}");
        }
    }

    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        Comment existing = commentDAO.findById(id).orElse(null);
        if (existing == null) {
            sendResponse(exchange, 404, "{\"error\":\"Comment not found\"}");
            return;
        }
        String body = readRequestBody(exchange);
        try {
            com.google.gson.JsonObject obj = gson.fromJson(body, com.google.gson.JsonObject.class);
            String content = obj.has("content") ? obj.get("content").getAsString() : null;
            if (content == null) {
                sendResponse(exchange, 400, "{\"error\":\"content required\"}");
                return;
            }
            existing.setContent(content);
            existing.touchOnUpdate();
            commentDAO.update(existing);
            sendResponse(exchange, 200, gson.toJson(existing));
        } catch (com.google.gson.JsonSyntaxException ex) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid JSON\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        boolean ok = commentDAO.deleteById(id);
        if (ok) sendResponse(exchange, 200, "{\"message\":\"Comment deleted\"}");
        else sendResponse(exchange, 404, "{\"error\":\"Comment not found\"}");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
