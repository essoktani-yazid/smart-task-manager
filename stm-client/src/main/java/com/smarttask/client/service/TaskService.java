package com.smarttask.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour gérer les opérations sur les tâches via l'API HTTP.
 * Communique avec le serveur pour toutes les opérations CRUD sur les tâches.
 */
public class TaskService {
    private static final String BASE_URL = "http://localhost:8080/api/tasks";
    private final Gson gson;

    public TaskService() {
        // Configuration de Gson pour gérer les LocalDateTime
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

    /**
     * Récupère toutes les tâches d'un utilisateur.
     */
    public List<Task> getTasksByUser(Long userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/" + userId + "/user");
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 200) {
                    Type listType = new TypeToken<List<Task>>(){}.getType();
                    return gson.fromJson(responseBody, listType);
                } else {
                    System.err.println("Erreur lors de la récupération des tâches: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Récupère une tâche par son ID.
     */
    public Task getTaskById(Long taskId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/" + taskId);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 200) {
                    return gson.fromJson(responseBody, Task.class);
                } else {
                    System.err.println("Erreur lors de la récupération de la tâche: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Crée une nouvelle tâche.
     */
    public Task createTask(String title, String description, Priority priority, User user) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL);
            request.setHeader("Content-Type", "application/json");

            Task task = new Task();
            task.setTitle(title);
            task.setDescription(description);
            task.setPriority(priority);
            task.setStatus(Status.TODO);
            task.setUser(user);
            task.setCreatedAt(LocalDateTime.now());

            String json = gson.toJson(task);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 201) {
                    return gson.fromJson(responseBody, Task.class);
                } else {
                    System.err.println("Erreur lors de la création de la tâche: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Met à jour une tâche existante.
     */
    public Task updateTask(Task task) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/" + task.getId());
            request.setHeader("Content-Type", "application/json");

            String json = gson.toJson(task);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 200) {
                    return gson.fromJson(responseBody, Task.class);
                } else {
                    System.err.println("Erreur lors de la mise à jour de la tâche: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Supprime une tâche.
     */
    public boolean deleteTask(Long taskId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + "/" + taskId);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

