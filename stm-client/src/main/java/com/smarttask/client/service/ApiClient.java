package com.smarttask.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.CalendarEvent;
import com.smarttask.model.Task;
import com.smarttask.server.util.GsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * API CLIENT - Frontend service for making HTTP requests to backend API
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * Handles all communication between frontend (CalendarController/PlannerManager)
 * and backend REST API (TaskHandler/EventHandler)
 * 
 * Usage:
 *   ApiClient client = new ApiClient("http://localhost:8080");
 *   List<Task> tasks = client.getTasksForUser(userId);
 *   client.createEvent(event);
 */
public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Constructor with custom base URL
     */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gson = GsonUtils.getGson();
    }

    /**
     * Default constructor (localhost:8080)
     */
    public ApiClient() {
        this("http://localhost:8080");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TASK API CALLS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/tasks/{id}/user
     * Get all tasks for a user
     */
    public List<Task> getTasksForUser(String userId) {
        try {
            String url = baseUrl + "/api/tasks/" + userId + "/user";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<Task>>(){}.getType());
            } else {
                System.err.println("❌ Error getting tasks: " + response.statusCode() + " - " + response.body());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting tasks for user: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * GET /api/tasks/{id}
     * Get task by ID
     */
    public Task getTaskById(String taskId) {
        try {
            String url = baseUrl + "/api/tasks/" + taskId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), Task.class);
            } else {
                System.err.println("❌ Error getting task: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting task by ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * POST /api/tasks
     * Create new task
     */
    public Task createTask(Task task) {
        try {
            String url = baseUrl + "/api/tasks";
            String json = gson.toJson(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("✅ Task created via API: " + task.getTitle());
                return gson.fromJson(response.body(), Task.class);
            } else {
                System.err.println("❌ Error creating task: " + response.statusCode() + " - " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating task: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * PUT /api/tasks/{id}
     * Update task
     */
    public boolean updateTask(Task task) {
        try {
            String url = baseUrl + "/api/tasks/" + task.getId();
            String json = gson.toJson(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Task updated via API: " + task.getTitle());
                return true;
            } else {
                System.err.println("❌ Error updating task: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * DELETE /api/tasks/{id}
     * Delete task
     */
    public boolean deleteTask(String taskId) {
        try {
            String url = baseUrl + "/api/tasks/" + taskId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("🗑️ Task deleted via API: " + taskId);
                return true;
            } else {
                System.err.println("❌ Error deleting task: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * GET /api/tasks/project/{projectId}
     * Get tasks by project
     */
    public List<Task> getTasksByProject(String projectId) {
        try {
            String url = baseUrl + "/api/tasks/project/" + projectId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<Task>>(){}.getType());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting tasks by project: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EVENT API CALLS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/events/user/{userId}
     * Get events for user (respects visibility)
     */
    public List<CalendarEvent> getEventsForUser(String userId) {
        try {
            String url = baseUrl + "/api/events/user/" + userId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<CalendarEvent> events = gson.fromJson(response.body(), new TypeToken<List<CalendarEvent>>(){}.getType());
                System.out.println("✅ Loaded " + events.size() + " events via API for user " + userId);
                return events;
            } else {
                System.err.println("❌ Error getting events: " + response.statusCode() + " - " + response.body());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting events for user: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * GET /api/events/{id}
     * Get event by ID
     */
    public CalendarEvent getEventById(String eventId) {
        try {
            String url = baseUrl + "/api/events/" + eventId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), CalendarEvent.class);
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting event by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * POST /api/events
     * Create new event
     */
    public CalendarEvent createEvent(CalendarEvent event) {
        try {
            String url = baseUrl + "/api/events";
            String json = gson.toJson(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("✅ Event created via API: " + event.getTitle());
                return gson.fromJson(response.body(), CalendarEvent.class);
            } else {
                System.err.println("❌ Error creating event: " + response.statusCode() + " - " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * PUT /api/events/{id}
     * Update event
     */
    public boolean updateEvent(CalendarEvent event) {
        try {
            String url = baseUrl + "/api/events/" + event.getId();
            String json = gson.toJson(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Event updated via API: " + event.getTitle());
                return true;
            } else {
                System.err.println("❌ Error updating event: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * DELETE /api/events/{id}
     * Delete event
     */
    public boolean deleteEvent(String eventId) {
        try {
            String url = baseUrl + "/api/events/" + eventId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("🗑️ Event deleted via API: " + eventId);
                return true;
            } else {
                System.err.println("❌ Error deleting event: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * GET /api/events/date/{date}
     * Get events on specific date
     */
    public List<CalendarEvent> getEventsByDate(LocalDate date) {
        try {
            String url = baseUrl + "/api/events/date/" + date.toString();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<CalendarEvent>>(){}.getType());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting events by date: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * GET /api/events/range?start={}&end={}
     * Get events in date range
     */
    public List<CalendarEvent> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String url = baseUrl + "/api/events/range?start=" + startDate.toString() + "&end=" + endDate.toString();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<CalendarEvent>>(){}.getType());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting events by range: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * GET /api/events/creator/{userId}
     * Get events created by user
     */
    public List<CalendarEvent> getEventsByCreator(String userId) {
        try {
            String url = baseUrl + "/api/events/creator/" + userId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<CalendarEvent>>(){}.getType());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting events by creator: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * GET /api/events/shared/{userId}
     * Get events shared with user
     */
    public List<CalendarEvent> getEventsSharedWithUser(String userId) {
        try {
            String url = baseUrl + "/api/events/shared/" + userId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), new TypeToken<List<CalendarEvent>>(){}.getType());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting shared events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Test if API is reachable
     */
    public boolean testConnection() {
        try {
            String url = baseUrl + "/api/tasks";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean success = response.statusCode() < 500;
            
            if (success) {
                System.out.println("✅ API connection successful: " + baseUrl);
            } else {
                System.err.println("❌ API connection failed: " + response.statusCode());
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("❌ Cannot reach API at " + baseUrl + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
