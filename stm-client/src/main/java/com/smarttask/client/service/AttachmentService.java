package com.smarttask.client.service;

import com.smarttask.model.Attachment;
import com.smarttask.client.util.GsonUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

public class AttachmentService {
    private final String baseUrl;
    private final HttpClient client;

    public AttachmentService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
    }

    public List<Attachment> getAttachments(String taskId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/attachments/task/" + taskId))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Error fetching attachments: " + response.body());
        return Arrays.asList(GsonUtils.getGson().fromJson(response.body(), Attachment[].class));
    }

    public Attachment addAttachment(Attachment attachment) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(attachment);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/attachments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("Error adding attachment: " + response.body());
        return GsonUtils.getGson().fromJson(response.body(), Attachment.class);
    }

    public void deleteAttachment(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/attachments/" + id))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Error deleting attachment: " + response.body());
    }
}
