package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.google.gson.reflect.TypeToken;
import com.smarttask.client.config.AppConfig;
import com.smarttask.model.TaskDependency;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DependencyService {

    private static final String BASE_URL = AppConfig.API_URL + "/dependencies";

    private final Gson gson = GsonUtils.getGson();

    public List<TaskDependency> getPredecessors(String taskId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/successor/" + taskId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 200) {
                    Type listType = new TypeToken<List<TaskDependency>>(){}.getType();
                    return gson.fromJson(body, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<TaskDependency> getSuccessors(String taskId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/predecessor/" + taskId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 200) {
                    Type listType = new TypeToken<List<TaskDependency>>(){}.getType();
                    return gson.fromJson(body, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TaskDependency addDependency(TaskDependency dependency) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL);
            request.setHeader("Content-Type", "application/json");
            String json = gson.toJson(dependency);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 201) return gson.fromJson(body, TaskDependency.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean removeDependency(String dependencyId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + "/" + dependencyId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
