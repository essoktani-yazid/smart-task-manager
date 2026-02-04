package com.smarttask.server.handler;

import com.google.gson.Gson;

import com.smarttask.server.util.GsonUtils;
import com.smarttask.model.Task;
import com.smarttask.model.TaskTag;
import com.smarttask.server.dao.TaskTagDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskTagHandler implements HttpHandler {
    private final TaskTagDAO dao = new TaskTagDAO();
    // Remplacez : private final Gson gson = new Gson();
    private final Gson gson = GsonUtils.getGson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && path.startsWith("/api/tags/task/")) {
                String taskId = path.substring("/api/tags/task/".length());
                handleGetByTask(exchange, taskId);
                return;
            }

            if ("POST".equals(method) && path.equals("/api/tags")) {
                handleCreate(exchange);
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/api/tags/")) {
                String suffix = path.substring("/api/tags/".length()); // taskId/tagName
                String[] parts = suffix.split("/", 2);
                if (parts.length == 2) handleDelete(exchange, parts[0], parts[1]);
                else sendResponse(exchange, 400, "{\"error\":\"Bad request\"}");
                return;
            }

            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetByTask(HttpExchange exchange, String taskId) throws IOException {
        List<TaskTag> list = dao.findByTaskId(taskId);
        sendResponse(exchange, 200, gson.toJson(list));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        TaskTag t = gson.fromJson(body, TaskTag.class);
        if (t.getTask() == null || t.getTask().getId() == null || t.getTagName() == null) {
            sendResponse(exchange, 400, "{\"error\":\"taskId and tagName required\"}");
            return;
        }
        dao.save(t);
        sendResponse(exchange, 201, gson.toJson(t));
    }

    private void handleDelete(HttpExchange exchange, String taskId, String tagName) throws IOException {
        boolean ok = dao.delete(taskId, tagName);
        if (ok) sendResponse(exchange, 200, "{\"message\":\"Tag deleted\"}");
        else sendResponse(exchange, 404, "{\"error\":\"Tag not found\"}");
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
