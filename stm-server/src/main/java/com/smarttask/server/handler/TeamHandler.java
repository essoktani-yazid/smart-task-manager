package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.server.dao.TeamDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.smarttask.server.util.GsonUtils;

public class TeamHandler implements HttpHandler {

    private final TeamDAO teamDAO = new TeamDAO();

    private final Gson gson = GsonUtils.getGson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        System.out.println("🌐 [TeamHandler] Incoming Request: " + method + " " + path);

        // GESTION CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // Normaliser le chemin : on ignore si ça commence par /api ou non
            String normalizedPath = path.startsWith("/api") ? path.substring(4) : path;
            System.out.println("🔍 Normalized Path: " + normalizedPath);

            if (normalizedPath.equals("/teams")) {
                if (method.equals("GET")) {
                    handleGetUserTeams(exchange, query);
                    return;
                }
                if (method.equals("POST")) {
                    handleCreateTeam(exchange);
                    return;
                }
            }

            if (normalizedPath.matches("/teams/[^/]+")) {
                String teamId = normalizedPath.split("/")[2];

                switch (method) {
                    case "GET" -> handleGetTeam(exchange, teamId);
                    case "PUT" -> handleUpdateTeam(exchange, teamId);
                    case "DELETE" -> handleDeleteTeam(exchange, teamId);
                }
                return;
            }

            if (normalizedPath.matches("/teams/[^/]+/members")) {
                String teamId = normalizedPath.split("/")[2];

                switch (method) {
                    case "GET" -> handleGetMembers(exchange, teamId);
                    case "POST" -> handleAddMember(exchange, teamId);
                    case "DELETE" -> handleRemoveMember(exchange, teamId, query);
                }
                return;
            }

            System.err.println("No route matched for " + method + " " + path);
            sendResponse(exchange, 404, "Not found (Route mismatch): " + path);

        } catch (Exception e) {
            System.err.println("Error in TeamHandler: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetUserTeams(HttpExchange exchange, String query) throws IOException {
        String userId = getQueryParam(query, "userId");
        if (userId == null) {
            sendResponse(exchange, 400, "Missing userId");
            return;
        }
        List<Team> teams = teamDAO.findByUser(userId);
        sendJson(exchange, 200, teams);
    }

    private void handleGetTeam(HttpExchange exchange, String teamId) throws IOException {
        Optional<Team> team = teamDAO.findById(teamId);
        if (team.isEmpty()) {
            sendResponse(exchange, 404, "Team not found");
            return;
        }
        sendJson(exchange, 200, team.get());
    }

    private void handleCreateTeam(HttpExchange exchange) throws IOException {
        Team team = readBody(exchange, Team.class);
        System.out.println("Creating team: " + team.getName() + " for owner: " + team.getOwnerId());
        String id = teamDAO.create(team);
        System.out.println("Team created with ID: " + id);
        sendResponse(exchange, 201, id);
    }

    private void handleUpdateTeam(HttpExchange exchange, String teamId) throws IOException {
        Team team = readBody(exchange, Team.class);
        team.setId(teamId);
        teamDAO.update(team);
        exchange.sendResponseHeaders(204, -1);
		exchange.close();
    }

    private void handleDeleteTeam(HttpExchange exchange, String teamId) throws IOException {
        try {
            teamDAO.delete(teamId);
            exchange.sendResponseHeaders(204, -1);
        } catch (Exception e) {
            String response = "{\"error\":\"" + e.getMessage() + "\"}";
            sendResponse(exchange, 500, response);
        } finally {
            exchange.close();
        }
    }

    private void handleGetMembers(HttpExchange exchange, String teamId) throws IOException {
        List<TeamMember> members = teamDAO.findMembers(teamId);
        sendJson(exchange, 200, members);
    }

    private void handleAddMember(HttpExchange exchange, String teamId) throws IOException {
        TeamMember member = readBody(exchange, TeamMember.class);
        teamDAO.addMember(teamId, member.getUserId());
        sendResponse(exchange, 201, "");
    }

    private void handleRemoveMember(HttpExchange exchange, String teamId, String query) throws IOException {
        String userId = getQueryParam(query, "userId");
        if (userId == null) {
            sendResponse(exchange, 400, "Missing userId");
            return;
        }
        teamDAO.removeMember(teamId, userId);
        sendResponse(exchange, 204, "");
    }

    private <T> T readBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return gson.fromJson(json, clazz);
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] response = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String getQueryParam(String query, String key) {
        if (query == null)
            return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=");
            if (parts.length == 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }
}
