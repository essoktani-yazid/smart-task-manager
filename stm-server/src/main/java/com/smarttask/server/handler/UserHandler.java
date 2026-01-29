package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.User;
import com.smarttask.server.dao.UserDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler for user-related operations.
 */
public class UserHandler implements HttpHandler {
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        // Expected path: /api/users/{id}
        String[] pathParts = path.split("/");
        
        try {
            if (pathParts.length == 4 && "PUT".equals(method)) {
                // Update user
                String userId = pathParts[3];
                handleUpdateUser(exchange, userId);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleUpdateUser(HttpExchange exchange, String userId) throws IOException {
        String requestBody = readRequestBody(exchange);
        User updatedUser = gson.fromJson(requestBody, User.class);
        
        if (!userId.equals(updatedUser.getId())) {
             sendResponse(exchange, 400, "{\"error\":\"ID mismatch\"}");
             return;
        }

        // Check if user exists
        if (userDAO.findById(userId).isEmpty()) {
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
            return;
        }

        userDAO.update(updatedUser);
        
        // Don't return password
        updatedUser.setPassword(null);
        sendResponse(exchange, 200, gson.toJson(updatedUser));
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
