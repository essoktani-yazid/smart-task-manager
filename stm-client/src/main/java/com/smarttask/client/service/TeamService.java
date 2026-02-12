package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.client.config.AppConfig;
import com.smarttask.client.util.GsonUtils;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.model.User;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TeamService {
    private final String baseUrl = AppConfig.API_URL + "/teams";
    private final String usersUrl = AppConfig.API_URL + "/users";
    private final Gson gson = GsonUtils.getGson();

    public List<User> getAllUsers() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(usersUrl);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode == 200) {
                    Type listType = new TypeToken<List<User>>() {
                    }.getType();
                    return gson.fromJson(body, listType);
                } else {
                    System.err.println("Error loading users: " + body);
                    return List.of();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public boolean addMember(String teamId, String userId, String role) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(baseUrl + "/" + teamId + "/members");
            request.setHeader("Content-Type", "application/json");

            TeamMember member = new TeamMember();
            member.setTeamId(teamId);
            member.setUserId(userId);
            member.setRole(role);

            String json = gson.toJson(member);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 201;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Team> getTeamsByUser(String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(baseUrl + "?userId=" + userId);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode == 200) {
                    Type listType = new TypeToken<List<Team>>() {
                    }.getType();
                    return gson.fromJson(body, listType);
                } else {
                    System.err.println("Error loading teams: " + body);
                    return List.of();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public String createTeam(Team team) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(baseUrl);
            request.setHeader("Content-Type", "application/json");
            String json = gson.toJson(team);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status == 201)
                    return body;
                System.err.println("Error creating team: " + body);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<TeamMember> getTeamMembers(String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(baseUrl + "/" + teamId + "/members");
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode == 200) {
                    Type listType = new TypeToken<List<TeamMember>>() {
                    }.getType();
                    return gson.fromJson(body, listType);
                } else {
                    return List.of();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public boolean updateTeam(Team team) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(baseUrl + "/" + team.getId());
            request.setHeader("Content-Type", "application/json");
            String json = gson.toJson(team);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deactivateTeam(String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(baseUrl + "/" + teamId);
            request.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

        public List<Team> getMyTeams(String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(baseUrl + "?userId=" + userId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return List.of();
                }

                Type listType = new TypeToken<List<Team>>() {}.getType();
                return gson.fromJson(
                        new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8),
                        listType
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
