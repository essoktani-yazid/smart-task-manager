package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.TaskDependency;
import com.smarttask.model.Task;
import com.smarttask.server.dao.TaskDependencyDAO;
import com.smarttask.server.dao.TaskDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DependencyHandler implements HttpHandler {
    private final TaskDependencyDAO depDAO = new TaskDependencyDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/dependencies/successor/")) {
                    String taskId = path.substring("/api/dependencies/successor/".length());
                    handleGetBySuccessor(exchange, taskId);
                } else if (path.startsWith("/api/dependencies/predecessor/")) {
                    String taskId = path.substring("/api/dependencies/predecessor/".length());
                    handleGetByPredecessor(exchange, taskId);
                } else if (path.startsWith("/api/dependencies/")) {
                    String id = path.substring("/api/dependencies/".length());
                    handleGetById(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/dependencies")) {
                    handleCreate(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/dependencies/")) {
                    String id = path.substring("/api/dependencies/".length());
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

    private void handleGetBySuccessor(HttpExchange exchange, String taskId) throws IOException {
        List<TaskDependency> deps = depDAO.findBySuccessorId(taskId);
        sendResponse(exchange, 200, gson.toJson(deps));
    }

    private void handleGetByPredecessor(HttpExchange exchange, String taskId) throws IOException {
        List<TaskDependency> deps = depDAO.findByPredecessorId(taskId);
        sendResponse(exchange, 200, gson.toJson(deps));
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        TaskDependency d = depDAO.findById(id).orElse(null);
        if (d == null) sendResponse(exchange, 404, "{\"error\":\"Dependency not found\"}");
        else sendResponse(exchange, 200, gson.toJson(d));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        TaskDependency d = gson.fromJson(body, TaskDependency.class);
        if (d.getPredecessor() == null || d.getSuccessor() == null) {
            sendResponse(exchange, 400, "{\"error\":\"predecessor and successor required\"}");
            return;
        }
        String id = depDAO.save(d);
        d.setId(id);
        sendResponse(exchange, 201, gson.toJson(d));
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        boolean ok = depDAO.deleteById(id);
        if (ok) sendResponse(exchange, 200, "{\"message\":\"Dependency deleted\"}");
        else sendResponse(exchange, 404, "{\"error\":\"Dependency not found\"}");
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
