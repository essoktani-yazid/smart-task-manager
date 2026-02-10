package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.TimeTracking;
import com.smarttask.server.dao.TimeTrackingDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TimeTrackingHandler implements HttpHandler {
    
    // On utilise une seule instance DAO
    private final TimeTrackingDAO dao;
    private final Gson gson;

    public TimeTrackingHandler() {
        this.dao = new TimeTrackingDAO();
        this.gson = GsonUtils.getGson(); // Utiliser GsonUtils pour gérer les Dates correctement
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        // Ajout des headers CORS pour éviter les blocages navigateur/client
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("POST".equals(method)) {
                handlePost(exchange);
            } else if ("PUT".equals(method)) {
                handlePut(exchange);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Cas 1: Récupérer par USER ID -> /api/timetracking/{userId}/user
        if (path.matches(".*/api/timetracking/[^/]+/user")) {
            String[] segments = path.split("/");
            // segments: ["", "api", "timetracking", "UUID", "user"]
            String userId = segments[3];
            
            List<TimeTracking> logs = dao.findByUserId(userId);
            sendResponse(exchange, 200, gson.toJson(logs));
            return;
        }

        // Cas 2: Récupérer par Task ID -> /api/timetracking/task/{taskId}
        if (path.matches(".*/api/timetracking/task/[^/]+")) {
            String taskId = path.substring(path.lastIndexOf("/") + 1);
            List<TimeTracking> logs = dao.findByTaskId(taskId);
            sendResponse(exchange, 200, gson.toJson(logs));
            return;
        }

        // Cas 3: Tout récupérer
        if (path.endsWith("/api/timetracking")) {
            sendResponse(exchange, 200, "[]"); // Pas implémenté pour sécurité
            return;
        }

        sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        TimeTracking tt = gson.fromJson(body, TimeTracking.class);
        String newId = dao.save(tt);
        tt.setId(newId);
        sendResponse(exchange, 201, gson.toJson(tt));
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        TimeTracking tt = gson.fromJson(body, TimeTracking.class);
        dao.update(tt);
        sendResponse(exchange, 200, gson.toJson(tt));
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}