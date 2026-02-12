package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service pour gérer les opérations sur les tâches via l'API HTTP.
 * Version standardisée utilisant java.net.http (Java 11+).
 */
public class TaskService {


    private String baseUrl = AppConfig.API_URL + "/tasks";

    private final HttpClient client;
    private final Gson gson;

    // ⚡ OPTIMISÉ: Constructeur avec HTTP/2 et keep-alive
    public TaskService() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)  // ⚡ HTTP/2 pour multiplexage
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.gson = GsonUtils.getGson();
    }

    // Constructeur avec URL personnalisée (si besoin)
    public TaskService(String url) {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)  // ⚡ HTTP/2 pour multiplexage
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.gson = GsonUtils.getGson();
        // Logique pour s'assurer que l'URL est correcte
        this.baseUrl = url.contains("/api/tasks") ? url : url + "/api/tasks";
    }

    public List<Task> getTasksByProject(String projectId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/project/" + projectId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Task[] tasks = gson.fromJson(response.body(), Task[].class);
                return Arrays.asList(tasks);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    /**
     * Récupère toutes les tâches d'un utilisateur.
     */
    public List<Task> getTasksByUser(String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + userId + "/user"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Task[] tasks = gson.fromJson(response.body(), Task[].class);
                return Arrays.asList(tasks);
            } else {
                System.err.println("Erreur getTasksByUser: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Récupère les sous-tâches d'une tâche parente.
     */
    public List<Task> getSubTasks(String parentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/sub/" + parentId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Task[] tasks = gson.fromJson(response.body(), Task[].class);
                return Arrays.asList(tasks);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Récupère une tâche par son ID.
     */
    public Task getTaskById(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + taskId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), Task.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Crée une nouvelle tâche (Version simplifiée).
     */
    public Task createTask(String title, String description, Priority priority, User user) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(Status.TODO);
        task.setUser(user);
        task.setCreatedAt(LocalDateTime.now());

        return createTask(task);
    }

    /**
     * Crée une tâche en envoyant l'objet complet (Main ou Sub task).
     */
    public Task createTask(Task task) {
        try {
            if (task.getCreatedAt() == null) task.setCreatedAt(LocalDateTime.now());
            if (task.getStatus() == null) task.setStatus(Status.TODO);

            String json = gson.toJson(task);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return gson.fromJson(response.body(), Task.class);
            } else {
                System.err.println("Erreur createTask: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Met à jour une tâche existante.
     */
    public Task updateTask(Task task) {
        try {
            String json = gson.toJson(task);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + task.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), Task.class);
            } else {
                System.err.println("Erreur updateTask: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Supprime une tâche.
     */
    public boolean deleteTask(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + taskId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
