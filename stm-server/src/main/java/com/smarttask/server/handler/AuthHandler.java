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
 * Handler HTTP pour gérer l'authentification (login, register).
 */
public class AuthHandler implements HttpHandler {
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method)) {
                if (path.equals("/api/auth/register")) {
                    handleRegister(exchange);
                } else if (path.equals("/api/auth/login")) {
                    handleLogin(exchange);
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

    private void handleRegister(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        User user = gson.fromJson(requestBody, User.class);

        // Vérifier si l'utilisateur existe déjà
        if (userDAO.existsByUsername(user.getUsername())) {
            sendResponse(exchange, 400, "{\"error\":\"Username already exists\"}");
            return;
        }

        // Créer le nouvel utilisateur
        Long userId = userDAO.save(user);
        user.setId(userId);
        user.setPassword(null); // Ne pas renvoyer le mot de passe

        sendResponse(exchange, 201, gson.toJson(user));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        LoginRequest loginRequest = gson.fromJson(requestBody, LoginRequest.class);

        // Trouver l'utilisateur
        User user = userDAO.findByUsername(loginRequest.username)
                .orElse(null);

        if (user == null || !user.getPassword().equals(loginRequest.password)) {
            sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
            return;
        }

        // Ne pas renvoyer le mot de passe
        user.setPassword(null);
        sendResponse(exchange, 200, gson.toJson(user));
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

    // Classe interne pour désérialiser la requête de login
    private static class LoginRequest {
        String username;
        String password;
    }
}

