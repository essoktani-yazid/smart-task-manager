package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.config.AppConfig;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.Project;
import com.smarttask.model.Team;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collections;

public class ProjectService {

    private String baseUrl = AppConfig.API_URL + "/projects";

    private final Gson gson = GsonUtils.getGson();
    // ⚡ OPTIMISÉ: HttpClient réutilisable avec HTTP/2
    private final HttpClient client;

    // Constructeur 1 : Sans arguments (pour la compatibilité)
    public ProjectService() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    // Constructeur 2 : Avec l'URL (pour la flexibilité dans ProjectsViewController)
    public ProjectService(String url) {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        // Si l'URL reçue est juste l'hôte (ex: http://127.0.0.1:8090), on ajoute le chemin
        if (url != null && !url.contains("/projects")) {
            this.baseUrl = url + "/projects";
        } else {
            this.baseUrl = url;
        }
    }

    public List<Project> getProjectsByUser(String userId) {
        try {
            // ⚡ OPTIMISÉ: Utilisation de Java 11 HttpClient
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + userId + "/user"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Type listType = new TypeToken<List<Project>>(){}.getType();
                return gson.fromJson(response.body(), listType);
            } else {
                System.err.println("Error loading projects: " + response.body());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Project createProject(Project project) {
        try {
            // ⚡ OPTIMISÉ: Java 11 HttpClient
            String json = gson.toJson(project);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 201) {
                return gson.fromJson(response.body(), Project.class);
            } else {
                System.err.println("Error creating project: " + response.body());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Project updateProject(Project project) {
        try {
            // ⚡ OPTIMISÉ: Java 11 HttpClient
            String json = gson.toJson(project);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + project.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), Project.class);
            } else {
                System.err.println("Error updating project: " + response.body());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteProject(String projectId) {
        try {
            // ⚡ OPTIMISÉ: Java 11 HttpClient
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/" + projectId))
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addTeamToProject(String projectId, String teamId) {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        // Format typique REST: POST /api/projects/{id}/teams/{teamId}
        HttpPost request = new HttpPost(baseUrl + "/" + projectId + "/teams/" + teamId);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getStatusLine().getStatusCode() == 200;
        }
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    public boolean removeTeamFromProject(String projectId, String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(baseUrl + "/" + projectId + "/teams/" + teamId);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Team> getTeamsByProject(String projectId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(baseUrl + "/" + projectId + "/teams");
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Type listType = new TypeToken<List<Team>>(){}.getType();
                    return gson.fromJson(body, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
