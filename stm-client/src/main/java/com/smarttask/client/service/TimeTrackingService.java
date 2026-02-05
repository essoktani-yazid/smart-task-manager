package com.smarttask.client.service;

import com.smarttask.model.TimeTracking;
import com.smarttask.client.util.GsonUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

public class TimeTrackingService {
    private final String baseUrl;
    private final HttpClient client;

    // Constructeur par défaut
    public TimeTrackingService() {
        this("http://localhost:8090/api/timetracking");
    }

    public TimeTrackingService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newHttpClient();
    }

    public List<TimeTracking> getTimeLogs(String taskId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/timetracking/task/" + taskId))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Error fetching time logs: " + response.body());
        return Arrays.asList(GsonUtils.getGson().fromJson(response.body(), TimeTracking[].class));
    }

    public TimeTracking startTracking(TimeTracking log) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(log);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/timetracking"))
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
                .uri(URI.create(baseUrl + "/api/timetracking/" + log.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Error updating tracking: " + response.body());
        return GsonUtils.getGson().fromJson(response.body(), TimeTracking.class);
    }
}
