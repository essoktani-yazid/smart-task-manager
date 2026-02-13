package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.CalendarEvent;
import com.smarttask.server.dao.CalendarEventDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * EVENT HANDLER - REST API for Calendar Events
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * Endpoints:
 * - GET    /api/events                    → Get all events
 * - GET    /api/events/{id}               → Get event by ID
 * - GET    /api/events/user/{userId}      → Get events for user (respects visibility)
 * - GET    /api/events/creator/{userId}   → Get events created by user
 * - GET    /api/events/shared/{userId}    → Get events shared with user
 * - GET    /api/events/date/{date}        → Get events on specific date
 * - GET    /api/events/range?start={}&end={}  → Get events in date range
 * - POST   /api/events                    → Create new event
 * - PUT    /api/events/{id}               → Update event
 * - DELETE /api/events/{id}               → Delete event
 */
public class EventHandler implements HttpHandler {

    private final CalendarEventDAO eventDAO = new CalendarEventDAO();
    private final Gson gson;

    public EventHandler() {
        this.gson = GsonUtils.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // CORS Headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "PUT" -> handlePut(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GET OPERATIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/api/events")) {
            handleGetAllEvents(exchange);
        } 
        else if (path.startsWith("/api/events/user/")) {
            String userId = extractPathParam(path, "/api/events/user/");
            handleGetEventsForUser(exchange, userId);
        }
        else if (path.startsWith("/api/events/creator/")) {
            String userId = extractPathParam(path, "/api/events/creator/");
            handleGetEventsByCreator(exchange, userId);
        }
        else if (path.startsWith("/api/events/shared/")) {
            String userId = extractPathParam(path, "/api/events/shared/");
            handleGetEventsSharedWithUser(exchange, userId);
        }
        else if (path.startsWith("/api/events/date/")) {
            String dateStr = extractPathParam(path, "/api/events/date/");
            handleGetEventsByDate(exchange, dateStr);
        }
        else if (path.startsWith("/api/events/range")) {
            handleGetEventsByRange(exchange);
        }
        else if (path.startsWith("/api/events/")) {
            String eventId = extractPathParam(path, "/api/events/");
            handleGetEvent(exchange, eventId);
        }
        else {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        }
    }

    /**
     * GET /api/events
     * Get all events (admin use only)
     */
    private void handleGetAllEvents(HttpExchange exchange) throws IOException {
        List<CalendarEvent> events = eventDAO.getAllEvents();
        sendResponse(exchange, 200, gson.toJson(events));
    }

    /**
     * GET /api/events/{id}
     * Get event by ID
     */
    private void handleGetEvent(HttpExchange exchange, String eventId) throws IOException {
        CalendarEvent event = eventDAO.getEventById(eventId);
        
        if (event == null) {
            sendResponse(exchange, 404, "{\"error\":\"Event not found\"}");
        } else {
            sendResponse(exchange, 200, gson.toJson(event));
        }
    }

    /**
     * GET /api/events/user/{userId}
     * Get events for user (respects visibility: PUBLIC + user's PRIVATE + SHARED with user)
     */
    private void handleGetEventsForUser(HttpExchange exchange, String userId) throws IOException {
        System.out.println("📅 Fetching events for user: " + userId);
        
        List<CalendarEvent> events = eventDAO.getEventsForUser(userId);
        
        System.out.println("✅ Found " + events.size() + " events for user " + userId);
        sendResponse(exchange, 200, gson.toJson(events));
    }

    /**
     * GET /api/events/creator/{userId}
     * Get events created by user
     */
    private void handleGetEventsByCreator(HttpExchange exchange, String userId) throws IOException {
        List<CalendarEvent> events = eventDAO.getEventsByCreator(userId);
        sendResponse(exchange, 200, gson.toJson(events));
    }

    /**
     * GET /api/events/shared/{userId}
     * Get events shared with user
     */
    private void handleGetEventsSharedWithUser(HttpExchange exchange, String userId) throws IOException {
        List<CalendarEvent> events = eventDAO.getEventsSharedWithUser(userId);
        sendResponse(exchange, 200, gson.toJson(events));
    }

    /**
     * GET /api/events/date/{date}
     * Get events on specific date (format: YYYY-MM-DD)
     */
    private void handleGetEventsByDate(HttpExchange exchange, String dateStr) throws IOException {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            List<CalendarEvent> events = eventDAO.getEventsByDate(date);
            sendResponse(exchange, 200, gson.toJson(events));
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid date format. Use YYYY-MM-DD\"}");
        }
    }

    /**
     * GET /api/events/range?start=YYYY-MM-DD&end=YYYY-MM-DD
     * Get events in date range
     */
    private void handleGetEventsByRange(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        
        if (query == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing query parameters\"}");
            return;
        }

        String[] params = query.split("&");
        LocalDate startDate = null;
        LocalDate endDate = null;

        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                if ("start".equals(keyValue[0])) {
                    startDate = LocalDate.parse(keyValue[1]);
                } else if ("end".equals(keyValue[0])) {
                    endDate = LocalDate.parse(keyValue[1]);
                }
            }
        }

        if (startDate == null || endDate == null) {
            sendResponse(exchange, 400, "{\"error\":\"Missing start or end date\"}");
            return;
        }

        List<CalendarEvent> events = eventDAO.getEventsByDateRange(startDate, endDate);
        sendResponse(exchange, 200, gson.toJson(events));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // POST OPERATION (Create)
    // ════════════════════════════════════════════════════════════════════════════

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/api/events")) {
            handleCreateEvent(exchange);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        }
    }

    /**
     * POST /api/events
     * Create new event
     */
    private void handleCreateEvent(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        CalendarEvent event = gson.fromJson(requestBody, CalendarEvent.class);

        // Validate
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"Title is required\"}");
            return;
        }

        // Set defaults
        if (event.getVisibility() == null) {
            event.setVisibility(CalendarEvent.EventVisibility.PUBLIC);
        }

        boolean success = eventDAO.createEvent(event);

        if (success) {
            System.out.println("✅ Event created: " + event.getTitle() + " (ID: " + event.getId() + ")");
            sendResponse(exchange, 201, gson.toJson(event));
        } else {
            sendResponse(exchange, 500, "{\"error\":\"Failed to create event\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PUT OPERATION (Update)
    // ════════════════════════════════════════════════════════════════════════════

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        if (path.startsWith("/api/events/")) {
            String eventId = extractPathParam(path, "/api/events/");
            handleUpdateEvent(exchange, eventId);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        }
    }

    /**
     * PUT /api/events/{id}
     * Update event
     */
    private void handleUpdateEvent(HttpExchange exchange, String eventId) throws IOException {
        String requestBody = readRequestBody(exchange);
        CalendarEvent event = gson.fromJson(requestBody, CalendarEvent.class);
        
        // Ensure ID matches
        event.setId(eventId);

        boolean success = eventDAO.updateEvent(event);

        if (success) {
            System.out.println("✅ Event updated: " + event.getTitle());
            sendResponse(exchange, 200, gson.toJson(event));
        } else {
            sendResponse(exchange, 500, "{\"error\":\"Failed to update event\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // DELETE OPERATION
    // ════════════════════════════════════════════════════════════════════════════

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.startsWith("/api/events/")) {
            String eventId = extractPathParam(path, "/api/events/");
            handleDeleteEvent(exchange, eventId);
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
        }
    }

    /**
     * DELETE /api/events/{id}
     * Delete event
     */
    private void handleDeleteEvent(HttpExchange exchange, String eventId) throws IOException {
        boolean success = eventDAO.deleteEvent(eventId);

        if (success) {
            System.out.println("🗑️ Event deleted: " + eventId);
            sendResponse(exchange, 200, "{\"message\":\"Event deleted successfully\"}");
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Event not found\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════════

    private String extractPathParam(String path, String prefix) {
        return path.substring(prefix.length());
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}
