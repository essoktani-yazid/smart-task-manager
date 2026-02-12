package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.util.GsonUtils;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.SharedTask;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.smarttask.client.config.AppConfig;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SharedTaskService {

    private static final String BASE_URL = AppConfig.API_URL + "/shared-tasks";

    private final Gson gson = GsonUtils.getGson();

    public List<SharedTask> getSharedByTask(String taskId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/task/" + taskId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 200) {
                    Type listType = new TypeToken<List<SharedTask>>(){}.getType();
                    return gson.fromJson(body, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SharedTask> getSharedWithUser(String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/user/" + userId);
            request.setHeader("Content-Type", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 200) {
                    Type listType = new TypeToken<List<SharedTask>>(){}.getType();
                    return gson.fromJson(body, listType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean shareTask(String taskId, String userId, String permissionLevel) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL);
            request.setHeader("Content-Type", "application/json");
            Map<String, String> body = Map.of("taskId", taskId, "userId", userId, "permissionLevel", permissionLevel);
            String json = gson.toJson(body);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 201;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePermission(String taskId, String userId, String permissionLevel) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/" + taskId + "/" + userId);
            request.setHeader("Content-Type", "application/json");
            Map<String, String> body = Map.of("permissionLevel", permissionLevel);
            String json = gson.toJson(body);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean revokeSharing(String taskId, String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + "/" + taskId + "/" + userId);
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
