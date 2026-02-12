package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.model.Comment;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommentService {

    private String baseUrl = AppConfig.API_URL + "/comments";

    private final HttpClient client;
    private final Gson gson;

    // Constructeur par défaut
    public CommentService() {
        this.client = HttpClient.newHttpClient();
        this.gson = GsonUtils.getGson();
    }

    // Constructeur avec URL personnalisée (pour la compatibilité avec votre Controller)
    public CommentService(String url) {
        this.client = HttpClient.newHttpClient();
        this.gson = GsonUtils.getGson();
        // On s'assure que l'URL pointe bien vers le endpoint des commentaires
        this.baseUrl = url.endsWith("/comments") ? url : url + "/comments";
    }

    /**
     * Récupère la liste des commentaires pour une tâche donnée.
     */
    public List<Comment> getTaskComments(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/task/" + taskId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Comment[] comments = gson.fromJson(response.body(), Comment[].class);
                return Arrays.asList(comments);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Ajoute un commentaire.
     */
    public Comment addComment(Comment comment) throws IOException, InterruptedException {
        String json = gson.toJson(comment);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return gson.fromJson(response.body(), Comment.class);
        } else {
            throw new IOException("Failed to add comment: HTTP " + response.statusCode());
        }
    }

    /**
     * Met à jour un commentaire (Optionnel).
     */
    public boolean updateComment(String commentId, String content) {
        try {
            Comment c = new Comment();
            c.setContent(content);
            String json = gson.toJson(c);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + commentId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime un commentaire (Optionnel).
     */
    public boolean deleteComment(String commentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + commentId))
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
