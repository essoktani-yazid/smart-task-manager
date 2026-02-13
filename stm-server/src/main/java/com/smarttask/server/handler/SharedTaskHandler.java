package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.SharedTask;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.dao.NotificationDAO;
import com.smarttask.server.dao.SharedTaskDAO;
import com.smarttask.server.dao.TaskDAO;
import com.smarttask.server.dao.UserDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SharedTaskHandler implements HttpHandler {
    private final SharedTaskDAO sharedDAO = new SharedTaskDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/shared-tasks/task/")) {
                    String taskId = path.substring("/api/shared-tasks/task/".length());
                    handleGetByTask(exchange, taskId);
                } else if (path.startsWith("/api/shared-tasks/user/")) {
                    String userId = path.substring("/api/shared-tasks/user/".length());
                    handleGetByUser(exchange, userId);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/shared-tasks")) {
                    handleShare(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("PUT".equals(method)) {
                if (path.startsWith("/api/shared-tasks/")) {
                    String[] parts = path.substring("/api/shared-tasks/".length()).split("/");
                    if (parts.length == 2) {
                        handleUpdatePermission(exchange, parts[0], parts[1]);
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    }
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/shared-tasks/")) {
                    String[] parts = path.substring("/api/shared-tasks/".length()).split("/");
                    if (parts.length == 2) {
                        handleUnshare(exchange, parts[0], parts[1]);
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    }
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
        List<SharedTask> shared = sharedDAO.findByTaskId(taskId);
        sendResponse(exchange, 200, gson.toJson(shared));
    }

    private void handleGetByUser(HttpExchange exchange, String userId) throws IOException {
        List<SharedTask> shared = sharedDAO.findByUserId(userId);
        sendResponse(exchange, 200, gson.toJson(shared));
    }

    private void handleShare(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> req = gson.fromJson(body, Map.class);
        String taskId = req.get("taskId");
        String userId = req.get("userId");
        String username = req.get("username");
        String senderUsername = req.getOrDefault("senderUsername", "Un utilisateur");
        String permission = req.getOrDefault("permissionLevel", "READ");

        if (taskId == null) {
            sendResponse(exchange, 400, "{\"error\":\"taskId required\"}");
            return;
        }

        if (userId == null && username != null) {
            Optional<User> userOpt = userDAO.findByUsername(username);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
            } else {
                sendResponse(exchange, 404, "{\"error\":\"User not found with username: " + username + "\"}");
                return;
            }
        }

        if (userId == null) {
            sendResponse(exchange, 400, "{\"error\":\"userId or username required\"}");
            return;
        }

        SharedTask st = new SharedTask();
        st.getTask().setId(taskId);
        st.getUser().setId(userId);
        try {
            st.setPermissionLevel(SharedTask.PermissionLevel.valueOf(permission));
        } catch (IllegalArgumentException e) {
            st.setPermissionLevel(SharedTask.PermissionLevel.READ);
        }
        sharedDAO.save(st);

        // Notification personnalisée
        try {
            Optional<Task> taskOpt = taskDAO.findById(taskId);
            String taskTitle = taskOpt.isPresent() ? taskOpt.get().getTitle() : "une tâche";
            notificationDAO.createAndSend(userId, "TASK_SHARED", "Tâche Partagée",
                    senderUsername + " a partagé la tâche '" + taskTitle + "' avec vous.");
        } catch (Exception e) {
            System.err.println("[ERROR] Echec envoi notification partage: " + e.getMessage());
        }

        sendResponse(exchange, 201, "{\"message\":\"Task shared\"}");
    }

    private void handleUpdatePermission(HttpExchange exchange, String taskId, String userId) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> req = gson.fromJson(body, Map.class);
        String permission = req.get("permissionLevel");
        if (permission == null) {
            sendResponse(exchange, 400, "{\"error\":\"permissionLevel required\"}");
            return;
        }
        sharedDAO.updatePermission(taskId, userId, permission);
        sendResponse(exchange, 200, "{\"message\":\"Permission updated\"}");
    }

    private void handleUnshare(HttpExchange exchange, String taskId, String userId) throws IOException {
        boolean ok = sharedDAO.delete(taskId, userId);
        if (ok)
            sendResponse(exchange, 200, "{\"message\":\"Sharing revoked\"}");
        else
            sendResponse(exchange, 404, "{\"error\":\"Sharing not found\"}");
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
