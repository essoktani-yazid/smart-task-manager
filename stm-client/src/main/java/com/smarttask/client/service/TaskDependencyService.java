package com.smarttask.client.service;

import com.smarttask.model.TaskDependency;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

public class TaskDependencyService {

    private String baseUrl = AppConfig.API_URL + "/task-dependencies";

    private final HttpClient client;

    public TaskDependencyService() {
        this.client = HttpClient.newHttpClient();
    }

    public TaskDependencyService(String baseUrl) {
        this.client = HttpClient.newHttpClient();
        this.baseUrl = baseUrl.endsWith("/api/task-dependencies") ? baseUrl : baseUrl + "/task-dependencies";
    }

    /**
     * Récupère toutes les dépendances précédentes (tâches qui dépendent de celle-ci)
     */
    public List<TaskDependency> getPredecessors(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/predecessor/" + taskId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                TaskDependency[] deps = GsonUtils.getGson().fromJson(response.body(), TaskDependency[].class);
                return Arrays.asList(deps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    /**
     * Récupère toutes les dépendances suivantes (tâches dont celle-ci dépend)
     */
    public List<TaskDependency> getSuccessors(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/successor/" + taskId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                TaskDependency[] deps = GsonUtils.getGson().fromJson(response.body(), TaskDependency[].class);
                return Arrays.asList(deps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    /**
     * Ajoute une dépendance entre deux tâches
     */
    public TaskDependency addDependency(TaskDependency dependency) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(dependency);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return GsonUtils.getGson().fromJson(response.body(), TaskDependency.class);
        } else {
            throw new IOException("Failed to add dependency: HTTP " + response.statusCode());
        }
    }

    /**
     * Supprime une dépendance
     */
    public boolean deleteDependency(String dependencyId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + dependencyId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 204;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
