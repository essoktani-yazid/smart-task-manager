package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.dao.TaskDAO;
import com.smarttask.server.dao.UserDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handler HTTP pour gérer les opérations sur les tâches (CRUD).
 */
public class TaskHandler implements HttpHandler {
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson;

    public TaskHandler() {
        this.gson = GsonUtils.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // GESTION CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("GET".equals(method)) {
                if (path.equals("/api/tasks")) {
                    handleGetAllTasks(exchange);
                }
                // Route Sous-tâches
                else if (path.startsWith("/api/tasks/sub/")) {
                    String parentId = path.substring("/api/tasks/sub/".length());
                    handleGetSubTasks(exchange, parentId);
                }
                else if (path.startsWith("/api/tasks/project/")) {
                    String projectId = path.substring("/api/tasks/project/".length());
                    handleGetTasksByProject(exchange, projectId);
                }
                else if (path.startsWith("/api/tasks/")) {
                    String[] parts = path.split("/");
                    // /api/tasks/{id}
                    if (parts.length == 4) {
                        String taskId = parts[3];
                        handleGetTask(exchange, taskId);
                    }
                    // /api/tasks/{id}/user
                    else if (parts.length == 5 && parts[4].equals("user")) {
                        String userId = parts[3];
                        handleGetTasksByUser(exchange, userId);
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    }
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/tasks")) {
                    handleCreateTask(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("PUT".equals(method)) {
                if (path.startsWith("/api/tasks/")) {
                    String taskId = path.substring("/api/tasks/".length());
                    handleUpdateTask(exchange, taskId);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/tasks/")) {
                    String taskId = path.substring("/api/tasks/".length());
                    handleDeleteTask(exchange, taskId);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Database Error: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }

    // --- IMPLEMENTATION DES METHODES ---

    private void handleGetAllTasks(HttpExchange exchange) throws IOException, SQLException {
        List<Task> tasks = taskDAO.findAll();
        sendResponse(exchange, 200, gson.toJson(tasks));
    }

    private void handleGetTask(HttpExchange exchange, String taskId) throws IOException, SQLException {
        // Adaptation DAO : Si findById n'existe pas ou renvoie un objet différent, on filtre findAll temporairement
        Task task = null;
        try {
            List<Task> all = taskDAO.findAll();
            task = all.stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
        } catch (Exception e) { e.printStackTrace(); }

        if (task == null) {
            sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
        } else {
            sendResponse(exchange, 200, gson.toJson(task));
        }
    }

    private void handleGetTasksByUser(HttpExchange exchange, String userId) throws IOException, SQLException {
        // On récupère toutes les tâches et on filtre par User ID
        // (Idéalement, utilisez taskDAO.findByUserId(userId) si elle existe)
        List<Task> tasks = taskDAO.findAll();

        List<Task> userTasks = tasks.stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
                .toList();

        sendResponse(exchange, 200, gson.toJson(userTasks));
    }

    private void handleGetSubTasks(HttpExchange exchange, String parentId) throws IOException, SQLException {
        // Appelle la méthode findSubTasks que nous avons ajoutée au DAO plus tôt
        List<Task> subTasks = taskDAO.findSubTasks(parentId);
        sendResponse(exchange, 200, gson.toJson(subTasks));
    }

    private void handleCreateTask(HttpExchange exchange) throws IOException, SQLException {
        String requestBody = readRequestBody(exchange);
        Task task = gson.fromJson(requestBody, Task.class);

        if (task.getUser() == null || task.getUser().getId() == null) {
            sendResponse(exchange, 400, "{\"error\":\"User ID is required\"}");
            return;
        }

        // --- CORRECTION ICI : Gestion de l'Optional ---
        User user = userDAO.findById(task.getUser().getId()).orElse(null);
        // ----------------------------------------------

        if (user == null) {
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
            return;
        }

        task.setUser(user);
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }

        taskDAO.save(task);
        sendResponse(exchange, 201, gson.toJson(task));
    }

    private void handleUpdateTask(HttpExchange exchange, String taskId) throws IOException, SQLException {
        String requestBody = readRequestBody(exchange);
        Task updatedTask = gson.fromJson(requestBody, Task.class);
        updatedTask.setId(taskId);

        taskDAO.update(updatedTask);
        sendResponse(exchange, 200, gson.toJson(updatedTask));
    }

    private void handleDeleteTask(HttpExchange exchange, String taskId) throws IOException, SQLException {
        taskDAO.deleteById(taskId);

        sendResponse(exchange, 200, "{\"message\":\"Task deleted\"}");
    }

    // --- UTILITAIRES ---

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleGetTasksByProject(HttpExchange exchange, String projectId) throws IOException, SQLException {
        List<Task> tasks = taskDAO.findByProjectId(projectId);
        sendResponse(exchange, 200, gson.toJson(tasks));
    }
}