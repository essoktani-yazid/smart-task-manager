package com.smarttask.client.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.smarttask.model.CalendarEvent;
import com.smarttask.model.Task;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * API CLIENT with proper LocalTime support and debugging
 */
public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private boolean debugMode = true; // Set to false in production

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gson = createGson();
    }

    public ApiClient() {
        this("http://localhost:8080");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CREATE GSON WITH ALL TYPE ADAPTERS
    // ════════════════════════════════════════════════════════════════════════════
    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();

        // LocalDateTime adapter
        builder.registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        builder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });

        // LocalDate adapter
        builder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        });
        builder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });

        // LocalTime adapter - THE FIX for the original error!
        builder.registerTypeAdapter(LocalTime.class, new JsonSerializer<LocalTime>() {
            @Override
            public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME));
            }
        });
        builder.registerTypeAdapter(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                return LocalTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME);
            }
        });

        builder.serializeNulls();
        builder.setPrettyPrinting(); // Makes debugging easier
        return builder.create();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TASK API CALLS
    // ════════════════════════════════════════════════════════════════════════════

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

    public Task createTask(Task task) {
        try {
            String url = baseUrl + "/api/tasks";
            String json = gson.toJson(task);

            // DEBUG: Print what we're sending
            if (debugMode) {
                System.out.println("🔍 DEBUG - Sending Task to API:");
                System.out.println("   URL: " + url);
                System.out.println("   Task ID: " + task.getId());
                System.out.println("   Task Title: " + task.getTitle());
                System.out.println("   User: " + (task.getUser() != null ? task.getUser().getUsername() : "null"));
                System.out.println("   User ID: " + (task.getUser() != null ? task.getUser().getId() : "null"));
                System.out.println("   JSON Payload:");
                System.out.println(json);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (debugMode) {
                System.out.println("🔍 DEBUG - API Response:");
                System.out.println("   Status: " + response.statusCode());
                System.out.println("   Body: " + response.body());
            }

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

    // ════════════════════════════════════════════════════════════════════════════
    // EVENT API CALLS
    // ════════════════════════════════════════════════════════════════════════════

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

    public CalendarEvent createEvent(CalendarEvent event) {
        try {
            String url = baseUrl + "/api/events";
            String json = gson.toJson(event);

            // DEBUG: Print what we're sending
            if (debugMode) {
                System.out.println("🔍 DEBUG - Sending Event to API:");
                System.out.println("   URL: " + url);
                System.out.println("   Event ID: " + event.getId());
                System.out.println("   Event Title: " + event.getTitle());
                System.out.println("   Creator ID: " + event.getCreatorUserId());
                System.out.println("   Start Time: " + event.getStartTime());
                System.out.println("   End Time: " + event.getEndTime());
                System.out.println("   JSON Payload:");
                System.out.println(json);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (debugMode) {
                System.out.println("🔍 DEBUG - API Response:");
                System.out.println("   Status: " + response.statusCode());
                System.out.println("   Body: " + response.body());
            }

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

    // ════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════════

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

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}