package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.Project;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProjectService {
    private String baseUrl = "http://127.0.0.1:8080/api/projects";
    private final Gson gson = GsonUtils.getGson();

    // Constructeur 1 : Sans arguments (pour la compatibilité)
    public ProjectService() {
    }

    // Constructeur 2 : Avec l'URL (pour la flexibilité dans ProjectsViewController)
    public ProjectService(String url) {
        // Si l'URL reçue est juste l'hôte (ex: http://127.0.0.1:8090), on ajoute le chemin
        if (url != null && !url.contains("/api/projects")) {
            this.baseUrl = url + "/api/projects";
        } else {
            this.baseUrl = url;
        }
    }

    public List<Project> getProjectsByUser(String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(baseUrl + "/" + userId + "/user");
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (statusCode == 200) {
                    Type listType = new TypeToken<List<Project>>(){}.getType();
                    return gson.fromJson(body, listType);
                } else {
                    System.err.println("Error loading projects: " + body);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Project createProject(Project project) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(baseUrl);
            request.setHeader("Content-Type", "application/json");
            String json = gson.toJson(project);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 201) return gson.fromJson(body, Project.class);
                System.err.println("Error creating project: " + body);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Project updateProject(Project project) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(baseUrl + "/" + project.getId());
            request.setHeader("Content-Type", "application/json");
            String json = gson.toJson(project);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 200) return gson.fromJson(body, Project.class);
                System.err.println("Error updating project: " + body);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteProject(String projectId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(baseUrl + "/" + projectId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                return status == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
