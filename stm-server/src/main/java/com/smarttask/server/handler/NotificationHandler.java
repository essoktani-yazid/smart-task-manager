package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.Notification;
import com.smarttask.server.dao.NotificationDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NotificationHandler implements HttpHandler {
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/notifications/user/")) {
                    String userId = path.substring("/api/notifications/user/".length());
                    String[] parts = userId.split("/");
                    if (parts.length == 2 && "unread".equals(parts[1])) {
                        handleGetUnread(exchange, parts[0]);
                    } else {
                        handleGetByUser(exchange, parts[0]);
                    }
                } else if (path.startsWith("/api/notifications/")) {
                    String id = path.substring("/api/notifications/".length());
                    handleGetById(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/notifications")) {
                    handleCreate(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("PUT".equals(method)) {
                if (path.startsWith("/api/notifications/") && path.endsWith("/read")) {
                    String id = path.substring("/api/notifications/".length(), path.length() - "/read".length());
                    handleMarkAsRead(exchange, id);
                } else if (path.startsWith("/api/notifications/")) {
                    String userId = path.substring("/api/notifications/".length());
                    if ("read-all".equals(userId)) {
                        String body = readRequestBody(exchange);
                        java.util.Map<String, String> req = gson.fromJson(body, java.util.Map.class);
                        handleMarkAllAsRead(exchange, req.get("userId"));
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    }
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/notifications/")) {
                    String id = path.substring("/api/notifications/".length());
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

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        Notification n = notifDAO.findById(id).orElse(null);
        if (n == null) sendResponse(exchange, 404, "{\"error\":\"Notification not found\"}");
        else sendResponse(exchange, 200, gson.toJson(n));
    }

    private void handleGetByUser(HttpExchange exchange, String userId) throws IOException {
        List<Notification> notifs = notifDAO.findByUserId(userId);
        sendResponse(exchange, 200, gson.toJson(notifs));
    }

    private void handleGetUnread(HttpExchange exchange, String userId) throws IOException {
        List<Notification> notifs = notifDAO.findUnreadByUserId(userId);
        sendResponse(exchange, 200, gson.toJson(notifs));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Notification n = gson.fromJson(body, Notification.class);
        if (n.getUser() == null) {
            sendResponse(exchange, 400, "{\"error\":\"user required\"}");
            return;
        }
        String id = notifDAO.save(n);
        n.setId(id);
        sendResponse(exchange, 201, gson.toJson(n));
    }

    private void handleMarkAsRead(HttpExchange exchange, String id) throws IOException {
        notifDAO.markAsRead(id);
        sendResponse(exchange, 200, "{\"message\":\"Notification marked as read\"}");
    }

    private void handleMarkAllAsRead(HttpExchange exchange, String userId) throws IOException {
        notifDAO.markAllAsRead(userId);
        sendResponse(exchange, 200, "{\"message\":\"All notifications marked as read\"}");
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        boolean ok = notifDAO.deleteById(id);
        if (ok) sendResponse(exchange, 200, "{\"message\":\"Notification deleted\"}");
        else sendResponse(exchange, 404, "{\"error\":\"Notification not found\"}");
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
