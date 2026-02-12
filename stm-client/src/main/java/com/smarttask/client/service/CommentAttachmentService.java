package com.smarttask.client.service;

import com.smarttask.model.CommentAttachment;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

public class CommentAttachmentService {

    private String baseUrl = AppConfig.API_URL + "/comment-attachments";

    private final HttpClient client;

    public CommentAttachmentService() {
        this.client = HttpClient.newHttpClient();
    }

    public CommentAttachmentService(String baseUrl) {
        this.client = HttpClient.newHttpClient();
        this.baseUrl = baseUrl.endsWith("/comment-attachments") ? baseUrl : baseUrl + "/comment-attachments";
    }

    /**
     * Récupère tous les attachments d'un commentaire
     */
    public List<CommentAttachment> getAttachmentsByCommentId(String commentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/comment/" + commentId))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                CommentAttachment[] attachments = GsonUtils.getGson().fromJson(response.body(), CommentAttachment[].class);
                return Arrays.asList(attachments);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList();
    }

    /**
     * Récupère un attachment par ID
     */
    public CommentAttachment getAttachmentById(String attachmentId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + attachmentId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return GsonUtils.getGson().fromJson(response.body(), CommentAttachment.class);
        }
        throw new IOException("Failed to fetch attachment: HTTP " + response.statusCode());
    }

    /**
     * Ajoute un nouvel attachment
     */
    public CommentAttachment addAttachment(CommentAttachment attachment) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(attachment);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return GsonUtils.getGson().fromJson(response.body(), CommentAttachment.class);
        } else {
            throw new IOException("Failed to add attachment: HTTP " + response.statusCode());
        }
    }

    /**
     * Supprime un attachment
     */
    public boolean deleteAttachment(String attachmentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + attachmentId))
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
