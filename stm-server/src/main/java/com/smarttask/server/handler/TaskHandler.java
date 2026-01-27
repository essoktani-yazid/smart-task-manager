package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.dao.TaskDAO;
import com.smarttask.server.dao.UserDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handler HTTP pour gérer les opérations sur les tâches (CRUD).
 */
public class TaskHandler implements HttpHandler {
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson;

    public TaskHandler() {
        // Configuration de Gson pour gérer les LocalDateTime et les enums
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new com.google.gson.JsonSerializer<LocalDateTime>() {
            @Override
            public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
                return new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new com.google.gson.JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonDeserializationContext context) {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });
        this.gson = gsonBuilder.create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.equals("/api/tasks")) {
                    handleGetAllTasks(exchange);
                } else if (path.startsWith("/api/tasks/")) {
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        Long taskId = Long.parseLong(parts[3]);
                        handleGetTask(exchange, taskId);
                    } else if (parts.length == 5 && parts[4].equals("user")) {
                        Long userId = Long.parseLong(parts[3]);
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
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        Long taskId = Long.parseLong(parts[3]);
                        handleUpdateTask(exchange, taskId);
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    }
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/tasks/")) {
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        Long taskId = Long.parseLong(parts[3]);
                        handleDeleteTask(exchange, taskId);
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

    private void handleGetAllTasks(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskDAO.findAll();
        sendResponse(exchange, 200, gson.toJson(tasks));
    }

    private void handleGetTask(HttpExchange exchange, Long taskId) throws IOException {
        Task task = taskDAO.findById(taskId).orElse(null);
        if (task == null) {
            sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
        } else {
            sendResponse(exchange, 200, gson.toJson(task));
        }
    }

    private void handleGetTasksByUser(HttpExchange exchange, Long userId) throws IOException {
        List<Task> tasks = taskDAO.findByUserId(userId);
        sendResponse(exchange, 200, gson.toJson(tasks));
    }

    private void handleCreateTask(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        Task task = gson.fromJson(requestBody, Task.class);

        // Vérifier que l'utilisateur existe
        if (task.getUser() == null || task.getUser().getId() == null) {
            sendResponse(exchange, 400, "{\"error\":\"User ID is required\"}");
            return;
        }

        User user = userDAO.findById(task.getUser().getId()).orElse(null);
        if (user == null) {
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
            return;
        }

        task.setUser(user);
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }

        Long taskId = taskDAO.save(task);
        task.setId(taskId);
        sendResponse(exchange, 201, gson.toJson(task));
    }

    private void handleUpdateTask(HttpExchange exchange, Long taskId) throws IOException {
        Task existingTask = taskDAO.findById(taskId).orElse(null);
        if (existingTask == null) {
            sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
            return;
        }

        String requestBody = readRequestBody(exchange);
        Task updatedTask = gson.fromJson(requestBody, Task.class);

        // Mettre à jour les champs
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setPriority(updatedTask.getPriority());
        existingTask.setStatus(updatedTask.getStatus());
        existingTask.setDueDate(updatedTask.getDueDate());

        taskDAO.update(existingTask);
        sendResponse(exchange, 200, gson.toJson(existingTask));
    }

    private void handleDeleteTask(HttpExchange exchange, Long taskId) throws IOException {
        boolean deleted = taskDAO.deleteById(taskId);
        if (deleted) {
            sendResponse(exchange, 200, "{\"message\":\"Task deleted successfully\"}");
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
        }
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


