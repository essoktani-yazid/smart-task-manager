package com.smarttask.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smarttask.client.util.GsonUtil;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage team operations (client side).
 */
public class TeamService {

    private static final String BASE_URL = "http://localhost:8080/api/teams";
    private final Gson gson = GsonUtil.createLocalDateTimeGsonAdapter();

    /** Get teams where user is owner or member */
    public List<Team> getMyTeams(String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "?userId=" + userId);

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

    /** Get a single team */
    public Optional<Team> getTeam(String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/" + teamId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return Optional.empty();
                }

                Team team = gson.fromJson(
                        new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8),
                        Team.class
                );
                return Optional.of(team);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /** Create a new team */
    public boolean createTeam(Team team) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL);
            request.setHeader("Content-Type", "application/json");

            request.setEntity(new StringEntity(
                    gson.toJson(team),
                    StandardCharsets.UTF_8
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 201;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Update team info */
    public boolean updateTeam(Team team) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/" + team.getId());
            request.setHeader("Content-Type", "application/json");

            request.setEntity(new StringEntity(
                    gson.toJson(team),
                    StandardCharsets.UTF_8
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Soft delete / deactivate team */
    public boolean deleteTeam(String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + "/" + teamId);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Get members of a team */
    public List<TeamMember> getMembers(String teamId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/" + teamId + "/members");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return List.of();
                }

                Type listType = new TypeToken<List<TeamMember>>() {}.getType();
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

    /** Add a member to team */
    public boolean addMember(String teamId, String userId, String role) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + "/" + teamId + "/members");
            request.setHeader("Content-Type", "application/json");

            TeamMember member = new TeamMember();
            member.setUserId(userId);
            member.setRole(role);

            request.setEntity(new StringEntity(
                    gson.toJson(member),
                    StandardCharsets.UTF_8
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 201;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Update member role */
    public boolean updateMemberRole(String teamId, String userId, String role) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + "/" + teamId + "/members");
            request.setHeader("Content-Type", "application/json");

            TeamMember member = new TeamMember();
            member.setUserId(userId);
            member.setRole(role);

            request.setEntity(new StringEntity(
                    gson.toJson(member),
                    StandardCharsets.UTF_8
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Remove member from team */
    public boolean removeMember(String teamId, String userId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(
                    BASE_URL + "/" + teamId + "/members?userId=" + userId
            );

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return response.getStatusLine().getStatusCode() == 204;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

