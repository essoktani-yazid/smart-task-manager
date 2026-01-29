package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.model.User;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;

/**
 * Service to manage user operations.
 */
public class UserService {
    private static final String BASE_URL = "http://localhost:8080/api/users";
    private final Gson gson = new Gson();

    /**
     * Updates user profile.
     */
    public boolean updateUser(User user) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/" + user.getId());
            request.setHeader("Content-Type", "application/json");

            String json = gson.toJson(user);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
