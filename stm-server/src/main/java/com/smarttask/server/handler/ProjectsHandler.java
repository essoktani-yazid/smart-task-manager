package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.smarttask.model.Project;
import com.smarttask.server.dao.ProjectDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProjectsHandler implements HttpHandler {
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final Gson gson;

    public ProjectsHandler() {
        this.gson = GsonUtils.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        try {
            if ("GET".equals(method)) {
                if (path.equals("/api/projects")) {
                    handleGetAll(exchange);
                } else if (parts.length == 4) {
                    handleGetById(exchange, parts[3]);
                } else if (parts.length == 5) {
                    if (parts[4].equals("user")) handleGetByUser(exchange, parts[3]);
                    if (parts[4].equals("teams")) handleGetTeams(exchange, parts[3]);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } 
            else if ("POST".equals(method)) {
                if (path.equals("/api/projects")) {
                    handleCreate(exchange);
                } else if (parts.length == 6 && parts[4].equals("teams")) {
                    handleAddTeam(exchange, parts[3], parts[5]);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } 
            else if ("DELETE".equals(method)) {
                if (parts.length == 4) {
                    handleDelete(exchange, parts[3]);
                } else if (parts.length == 6 && parts[4].equals("teams")) {
                    handleRemoveTeam(exchange, parts[3], parts[5]);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("PUT".equals(method)) {
                if (path.startsWith("/api/projects/")) {
                    if (parts.length == 4) {
                        String id = parts[3];
                        handleUpdate(exchange, id);
                    } else sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                } else sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        List<Project> list = projectDAO.findAll();
        sendResponse(exchange, 200, gson.toJson(list));
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        Project p = projectDAO.findById(id).orElse(null);
        if (p == null) sendResponse(exchange, 404, "{\"error\":\"Project not found\"}");
        else sendResponse(exchange, 200, gson.toJson(p));
    }

    private void handleGetByUser(HttpExchange exchange, String userId) throws IOException {
        List<Project> list = projectDAO.findByUserId(userId);
        sendResponse(exchange, 200, gson.toJson(list));
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Project p = gson.fromJson(body, Project.class);
        if (p.getUserId() == null) {
            sendResponse(exchange, 400, "{\"error\":\"userId required\"}");
            return;
        }
        String id = projectDAO.save(p);
        p.setId(id);
        sendResponse(exchange, 201, gson.toJson(p));
    }

    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        Project existing = projectDAO.findById(id).orElse(null);
        if (existing == null) {
            sendResponse(exchange, 404, "{\"error\":\"Project not found\"}");
            return;
        }
        String body = readRequestBody(exchange);
        Project updated = gson.fromJson(body, Project.class);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setColor(updated.getColor());
        existing.setUpdatedAt(updated.getUpdatedAt());
        existing.setActive(updated.isActive());
        projectDAO.update(existing);
        sendResponse(exchange, 200, gson.toJson(existing));
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        boolean ok = projectDAO.deleteById(id);
        if (ok) sendResponse(exchange, 200, "{\"message\":\"Project deleted\"}");
        else sendResponse(exchange, 404, "{\"error\":\"Project not found\"}");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void handleGetTeams(HttpExchange exchange, String projectId) throws IOException {
        List<com.smarttask.model.Team> teams = projectDAO.findTeamsByProjectId(projectId);
        sendResponse(exchange, 200, gson.toJson(teams));
    }

    private void handleAddTeam(HttpExchange exchange, String projectId, String teamId) throws IOException {
        projectDAO.addTeamToProject(projectId, teamId);
        sendResponse(exchange, 200, "{\"message\":\"Team added to project\"}");
    }

    private void handleRemoveTeam(HttpExchange exchange, String projectId, String teamId) throws IOException {
        projectDAO.removeTeamFromProject(projectId, teamId);
        sendResponse(exchange, 200, "{\"message\":\"Team removed from project\"}");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
