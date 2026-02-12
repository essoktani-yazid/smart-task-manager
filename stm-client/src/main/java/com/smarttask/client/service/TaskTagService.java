package com.smarttask.client.service;

import com.smarttask.model.TaskTag;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

public class TaskTagService {
    private final String baseUrl;
    private final HttpClient client;

    /**
     * CONSTRUCTEUR PAR DÉFAUT (Celui qui manquait)
     * Pointe automatiquement vers le serveur local port 8090.
     */
    public TaskTagService() {
        this.baseUrl = AppConfig.API_URL;
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Constructeur avec URL personnalisée
     */
    public TaskTagService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
    }

    public List<TaskTag> listByTaskId(String taskId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tags/task/" + taskId))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            // On renvoie une liste vide ou on log l'erreur pour ne pas bloquer l'UI
            System.err.println("Erreur récupération tags: " + resp.statusCode());
            return List.of();
        }
        TaskTag[] arr = GsonUtils.getGson().fromJson(resp.body(), TaskTag[].class);
        return Arrays.asList(arr);
    }

    public TaskTag add(TaskTag tag) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(tag);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tags"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 201 && resp.statusCode() != 200) {
            throw new IOException("Failed to add tag: " + resp.body());
        }
        return GsonUtils.getGson().fromJson(resp.body(), TaskTag.class);
    }

    public void delete(String taskId, String tagName) throws IOException, InterruptedException {
        // Encodage basique pour éviter les erreurs si le tag contient des espaces
        String encodedTag = tagName.replace(" ", "%20");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/tags/" + taskId + "/" + encodedTag))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Failed to delete tag: " + resp.body());
        }
    }
}
