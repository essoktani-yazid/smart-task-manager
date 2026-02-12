package com.smarttask.client.service;

import com.smarttask.model.TimeTracking;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public class TimeTrackingService {
    private final String baseUrl;
    private final HttpClient client;

    public TimeTrackingService() {
        this(AppConfig.API_URL + "/timetracking");
    }

    public TimeTrackingService(String baseUrl) {
        this.baseUrl = baseUrl;
        // ⚡ OPTIMISÉ: HTTP/2 pour multiplexage
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public List<TimeTracking> getTimeLogsByUser(String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + userId + "/user")) 
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return Arrays.asList(GsonUtils.getGson().fromJson(response.body(), TimeTracking[].class));
            } else {
                System.err.println("Erreur getTimeLogsByUser: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<TimeTracking> getTimeLogs(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/task/" + taskId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Arrays.asList(GsonUtils.getGson().fromJson(response.body(), TimeTracking[].class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public TimeTracking startTracking(TimeTracking log) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(log);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("Error starting tracking: " + response.body());
        return GsonUtils.getGson().fromJson(response.body(), TimeTracking.class);
    }

    public TimeTracking updateTracking(TimeTracking log) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(log);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + log.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Error updating tracking: " + response.body());
        return GsonUtils.getGson().fromJson(response.body(), TimeTracking.class);
    }
}
