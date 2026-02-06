package com.smarttask.client.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import com.smarttask.model.CalendarEvent;
import com.smarttask.model.Task;
import com.smarttask.model.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * GOOGLE CALENDAR SYNC SERVICE
 *
 * Handles synchronization between SmartTask and Google Calendar
 * - OAuth 2.0 authentication
 * - Create/Update/Delete events on Google Calendar
 * - Async operations to prevent UI blocking
 * - Persistent mapping to prevent duplicates
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 */
public class GoogleCalendarService {

    // ════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════════

    private static final String APPLICATION_NAME = "SmartTask Calendar";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Directory to store user credentials
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // ✨ NEW: Directory to store sync mappings
    private static final String SYNC_DATA_DIRECTORY_PATH = "sync_data";

    // OAuth 2.0 scopes - we need full calendar access
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    // Path to credentials.json file (downloaded from Google Cloud Console)
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    // ════════════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES
    // ════════════════════════════════════════════════════════════════════════════

    private Calendar calendarService;
    private User currentUser;
    private boolean isAuthenticated = false;
    private String userCalendarId = "primary"; // Use primary calendar by default

    // Async executor for non-blocking operations
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Map to track Google Event IDs for our tasks/events
    // Key: SmartTask ID, Value: Google Calendar Event ID
    private final Map<String, String> taskToGoogleEventMap = new HashMap<>();
    private final Map<String, String> eventToGoogleEventMap = new HashMap<String, String>();

    // Sync listeners
    private final List<GoogleCalendarSyncListener> syncListeners = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════════════════
    // SINGLETON PATTERN
    // ════════════════════════════════════════════════════════════════════════════

    private static GoogleCalendarService instance;

    public static synchronized GoogleCalendarService getInstance() {
        if (instance == null) {
            instance = new GoogleCalendarService();
        }
        return instance;
    }

    private GoogleCalendarService() {
        // Private constructor for singleton
    }

    // ════════════════════════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Initialize the Google Calendar service with OAuth 2.0
     * This will open a browser for user authentication if needed
     */
    public CompletableFuture<Boolean> authenticate(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.currentUser = user;

                // Build a new authorized API client service
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

                Credential credential = getCredentials(HTTP_TRANSPORT, user);

                calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                isAuthenticated = true;

                // ✨ NEW: Load existing mappings from disk
                loadSyncMappings(user);

                System.out.println("✅ Google Calendar authenticated for user: " + user.getEmail());
                notifyListeners(SyncStatus.CONNECTED, "Connected to Google Calendar");

                return true;

            } catch (Exception e) {
                System.err.println("❌ Google Calendar authentication failed: " + e.getMessage());
                e.printStackTrace();
                isAuthenticated = false;
                notifyListeners(SyncStatus.ERROR, "Authentication failed: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /**
     * Get OAuth 2.0 credentials
     * Uses stored tokens if available, otherwise prompts for authorization
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, User user)
            throws IOException {

        // Load client secrets from credentials.json
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

        if (in == null) {
            throw new FileNotFoundException(
                    "Resource not found: " + CREDENTIALS_FILE_PATH + "\n" +
                            "Please download credentials.json from Google Cloud Console and place it in src/main/resources/"
            );
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Create user-specific token directory
        String userTokenPath = TOKENS_DIRECTORY_PATH + "/" + user.getId();
        File tokenFolder = new File(userTokenPath);
        if (!tokenFolder.exists()) {
            tokenFolder.mkdirs();
        }

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
                .setAccessType("offline")
                .build();

        // Use a local server to receive the authorization code
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return isAuthenticated && calendarService != null;
    }

    /**
     * Disconnect from Google Calendar and DELETE stored tokens
     */
    public void disconnect() {
        if (currentUser != null) {
            // ✨ Save mappings before disconnecting (so we don't lose track)
            saveSyncMappings(currentUser);

            // Delete the stored tokens for this user
            String userTokenPath = TOKENS_DIRECTORY_PATH + "/" + currentUser.getId();
            File tokenFolder = new File(userTokenPath);

            if (tokenFolder.exists()) {
                try {
                    deleteDirectory(tokenFolder);
                    System.out.println("🗑️ Deleted stored tokens for user: " + currentUser.getId());
                } catch (IOException e) {
                    System.err.println("⚠️ Failed to delete tokens: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Clear service and state
        calendarService = null;
        isAuthenticated = false;
        currentUser = null;
        // ✨ DON'T clear the maps - they're saved to disk

        notifyListeners(SyncStatus.DISCONNECTED, "Disconnected from Google Calendar");
    }

    /**
     * Helper method to recursively delete a directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory.getAbsolutePath());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ✨ NEW: PERSISTENT SYNC MAPPING METHODS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Load sync mappings from disk for a user
     */
    private void loadSyncMappings(User user) {
        try {
            String syncDataPath = SYNC_DATA_DIRECTORY_PATH + "/" + user.getId();
            File syncDataFolder = new File(syncDataPath);

            if (!syncDataFolder.exists()) {
                System.out.println("📂 No existing sync data for user: " + user.getId());
                return;
            }

            // Load task mappings
            File taskMappingFile = new File(syncDataFolder, "task_mappings.txt");
            if (taskMappingFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(taskMappingFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            taskToGoogleEventMap.put(parts[0], parts[1]);
                        }
                    }
                }
                System.out.println("✅ Loaded " + taskToGoogleEventMap.size() + " task mappings");
            }

            // Load event mappings
            File eventMappingFile = new File(syncDataFolder, "event_mappings.txt");
            if (eventMappingFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(eventMappingFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            eventToGoogleEventMap.put(parts[0], parts[1]);
                        }
                    }
                }
                System.out.println("✅ Loaded " + eventToGoogleEventMap.size() + " event mappings");
            }

        } catch (IOException e) {
            System.err.println("⚠️ Failed to load sync mappings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save sync mappings to disk for a user
     */
    private void saveSyncMappings(User user) {
        try {
            String syncDataPath = SYNC_DATA_DIRECTORY_PATH + "/" + user.getId();
            File syncDataFolder = new File(syncDataPath);

            if (!syncDataFolder.exists()) {
                syncDataFolder.mkdirs();
            }

            // Save task mappings
            File taskMappingFile = new File(syncDataFolder, "task_mappings.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(taskMappingFile))) {
                for (Map.Entry<String, String> entry : taskToGoogleEventMap.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }
            System.out.println("💾 Saved " + taskToGoogleEventMap.size() + " task mappings");

            // Save event mappings
            File eventMappingFile = new File(syncDataFolder, "event_mappings.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(eventMappingFile))) {
                for (Map.Entry<String, String> entry : eventToGoogleEventMap.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }
            System.out.println("💾 Saved " + eventToGoogleEventMap.size() + " event mappings");

        } catch (IOException e) {
            System.err.println("⚠️ Failed to save sync mappings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SYNC TASKS TO GOOGLE CALENDAR
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Sync a Task to Google Calendar (Create or Update)
     */
    public CompletableFuture<String> syncTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("⚠️ Cannot sync task - not authenticated");
                return null;
            }

            try {
                String taskId = String.valueOf(task.getId());
                String existingGoogleEventId = taskToGoogleEventMap.get(taskId);

                // Convert Task to Google Calendar Event
                Event googleEvent = convertTaskToGoogleEvent(task);

                Event result;
                if (existingGoogleEventId != null) {
                    // Update existing event
                    result = calendarService.events()
                            .update(userCalendarId, existingGoogleEventId, googleEvent)
                            .execute();
                    System.out.println("✅ Task updated in Google Calendar: " + task.getTitle());
                } else {
                    // Create new event
                    result = calendarService.events()
                            .insert(userCalendarId, googleEvent)
                            .execute();
                    taskToGoogleEventMap.put(taskId, result.getId());

                    // ✨ NEW: Save mappings after creating new item
                    if (currentUser != null) {
                        saveSyncMappings(currentUser);
                    }

                    System.out.println("✅ Task added to Google Calendar: " + task.getTitle());
                }

                notifyListeners(SyncStatus.SYNCED, "Task synced: " + task.getTitle());
                return result.getId();

            } catch (Exception e) {
                System.err.println("❌ Failed to sync task: " + e.getMessage());
                notifyListeners(SyncStatus.ERROR, "Failed to sync task: " + e.getMessage());
                return null;
            }
        }, executor);
    }

    /**
     * Convert SmartTask Task to Google Calendar Event
     */
    private Event convertTaskToGoogleEvent(Task task) {
        Event event = new Event();

        // Title with task indicator
        event.setSummary("📋 " + task.getTitle());

        // Description
        StringBuilder description = new StringBuilder();
        description.append("📌 SmartTask - Task\n\n");

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            description.append(task.getDescription()).append("\n\n");
        }

        description.append("Priority: ").append(task.getPriority().name()).append("\n");
        description.append("Status: ").append(task.getStatus().name()).append("\n");

        if (task.getUser() != null) {
            description.append("Assigned to: ").append(task.getUser().getUsername());
        }

        event.setDescription(description.toString());

        // Date and Time
        LocalDateTime dueDate = task.getDueDate();
        if (dueDate != null) {
            // Create start and end times (1 hour duration)
            ZonedDateTime startZoned = dueDate.atZone(ZoneId.systemDefault());
            ZonedDateTime endZoned = startZoned.plusHours(1);

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());

            event.setStart(start);
            event.setEnd(end);
        }

        // Color based on priority
        event.setColorId(getColorIdForPriority(task.getPriority().ordinal()));

        // Add reminder
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("popup").setMinutes(30),
                        new EventReminder().setMethod("email").setMinutes(60)
                ));
        event.setReminders(reminders);

        return event;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SYNC EVENTS TO GOOGLE CALENDAR
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Sync a CalendarEvent to Google Calendar (Create or Update)
     */
    public CompletableFuture<String> syncEvent(CalendarEvent calendarEvent) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("⚠️ Cannot sync event - not authenticated");
                return null;
            }

            try {
                String eventId = calendarEvent.getId();
                String existingGoogleEventId = eventToGoogleEventMap.get(eventId);

                // Convert CalendarEvent to Google Calendar Event
                Event googleEvent = convertCalendarEventToGoogleEvent(calendarEvent);

                Event result;
                if (existingGoogleEventId != null) {
                    // Update existing event
                    result = calendarService.events()
                            .update(userCalendarId, existingGoogleEventId, googleEvent)
                            .execute();
                    System.out.println("✅ Event updated in Google Calendar: " + calendarEvent.getTitle());
                } else {
                    // Create new event
                    result = calendarService.events()
                            .insert(userCalendarId, googleEvent)
                            .execute();
                    eventToGoogleEventMap.put(eventId, result.getId());

                    // ✨ NEW: Save mappings after creating new item
                    if (currentUser != null) {
                        saveSyncMappings(currentUser);
                    }

                    System.out.println("✅ Event added to Google Calendar: " + calendarEvent.getTitle());
                }

                notifyListeners(SyncStatus.SYNCED, "Event synced: " + calendarEvent.getTitle());
                return result.getId();

            } catch (Exception e) {
                System.err.println("❌ Failed to sync event: " + e.getMessage());
                notifyListeners(SyncStatus.ERROR, "Failed to sync event: " + e.getMessage());
                return null;
            }
        }, executor);
    }

    /**
     * Convert SmartTask CalendarEvent to Google Calendar Event
     */
    private Event convertCalendarEventToGoogleEvent(CalendarEvent calendarEvent) {
        Event event = new Event();

        // Title with event indicator
        event.setSummary("📅 " + calendarEvent.getTitle());

        // Description
        StringBuilder description = new StringBuilder();
        description.append("📌 SmartTask - Event\n\n");

        if (calendarEvent.getDescription() != null && !calendarEvent.getDescription().isEmpty()) {
            description.append(calendarEvent.getDescription()).append("\n\n");
        }

        description.append("Priority: ").append(calendarEvent.getPriority().name()).append("\n");
        description.append("Type: ").append(calendarEvent.getEventType().name()).append("\n");

        if (calendarEvent.isCompleted()) {
            description.append("Status: ✅ Completed");
        }

        event.setDescription(description.toString());

        // Date and Time
        LocalDate date = calendarEvent.getDate();
        LocalTime startTime = calendarEvent.getStartTime();
        LocalTime endTime = calendarEvent.getEndTime();

        if (date != null && startTime != null && endTime != null) {
            ZonedDateTime startZoned = LocalDateTime.of(date, startTime)
                    .atZone(ZoneId.systemDefault());
            ZonedDateTime endZoned = LocalDateTime.of(date, endTime)
                    .atZone(ZoneId.systemDefault());

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());

            event.setStart(start);
            event.setEnd(end);
        }

        // Handle recurring events
        if (calendarEvent.getEventType() == CalendarEvent.EventType.RECURRING_EVENT) {
            List<String> recurrence = buildRecurrenceRule(calendarEvent);
            if (!recurrence.isEmpty()) {
                event.setRecurrence(recurrence);
            }
        }

        // Color based on priority
        event.setColorId(getColorIdForEventPriority(calendarEvent.getPriority()));

        // Add reminder
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("popup").setMinutes(15),
                        new EventReminder().setMethod("popup").setMinutes(60)
                ));
        event.setReminders(reminders);

        return event;
    }

    /**
     * Build recurrence rule (RRULE) for recurring events
     */
    private List<String> buildRecurrenceRule(CalendarEvent event) {
        List<String> rules = new ArrayList<>();

        if (event.getPeriodicType() == null) {
            return rules;
        }

        StringBuilder rrule = new StringBuilder("RRULE:FREQ=");

        switch (event.getPeriodicType()) {
            case PER_DAY:
                rrule.append("DAILY");
                break;

            case PER_WEEK:
                rrule.append("WEEKLY");
                if (event.getDaysInWeek() != null && !event.getDaysInWeek().isEmpty()) {
                    rrule.append(";BYDAY=");
                    String[] days = event.getDaysInWeek().split(",");
                    String[] dayNames = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
                    StringJoiner joiner = new StringJoiner(",");
                    for (String day : days) {
                        int idx = Integer.parseInt(day.trim()) - 1;
                        if (idx >= 0 && idx < 7) {
                            joiner.add(dayNames[idx]);
                        }
                    }
                    rrule.append(joiner.toString());
                }
                break;

            case PER_MONTH:
                rrule.append("MONTHLY");
                if (event.getPlaceInMonth() != null) {
                    // First, Second, Third, Fourth, Last
                    int weekNum = event.getPlaceInMonth().ordinal() + 1;
                    if (weekNum == 5) weekNum = -1; // Last
                    rrule.append(";BYDAY=").append(weekNum).append("MO"); // Example: 1MO = First Monday
                }
                break;

            case PER_YEAR:
                rrule.append("YEARLY");
                break;
        }

        rules.add(rrule.toString());
        return rules;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // DELETE FROM GOOGLE CALENDAR
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Delete a Task from Google Calendar
     */
    public CompletableFuture<Boolean> deleteTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                return false;
            }

            try {
                String taskId = String.valueOf(task.getId());
                String googleEventId = taskToGoogleEventMap.get(taskId);

                if (googleEventId != null) {
                    calendarService.events()
                            .delete(userCalendarId, googleEventId)
                            .execute();
                    taskToGoogleEventMap.remove(taskId);

                    // ✨ NEW: Save mappings after deletion
                    if (currentUser != null) {
                        saveSyncMappings(currentUser);
                    }

                    System.out.println("✅ Task deleted from Google Calendar: " + task.getTitle());
                    notifyListeners(SyncStatus.SYNCED, "Task deleted: " + task.getTitle());
                    return true;
                }

                return false;

            } catch (Exception e) {
                System.err.println("❌ Failed to delete task from Google Calendar: " + e.getMessage());
                notifyListeners(SyncStatus.ERROR, "Failed to delete task: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /**
     * Delete an Event from Google Calendar
     */
    public CompletableFuture<Boolean> deleteEvent(CalendarEvent calendarEvent) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                return false;
            }

            try {
                String eventId = calendarEvent.getId();
                String googleEventId = eventToGoogleEventMap.get(eventId);

                if (googleEventId != null) {
                    calendarService.events()
                            .delete(userCalendarId, googleEventId)
                            .execute();
                    eventToGoogleEventMap.remove(eventId);

                    // ✨ NEW: Save mappings after deletion
                    if (currentUser != null) {
                        saveSyncMappings(currentUser);
                    }

                    System.out.println("✅ Event deleted from Google Calendar: " + calendarEvent.getTitle());
                    notifyListeners(SyncStatus.SYNCED, "Event deleted: " + calendarEvent.getTitle());
                    return true;
                }

                return false;

            } catch (Exception e) {
                System.err.println("❌ Failed to delete event from Google Calendar: " + e.getMessage());
                notifyListeners(SyncStatus.ERROR, "Failed to delete event: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // ════════════════════════════════════════════════════════════════════════════
// REPLACE THE syncAll() METHOD IN GoogleCalendarService.java
// ════════════════════════════════════════════════════════════════════════════

    /**
     * ✨ FIXED: Sync all tasks and events to Google Calendar in parallel
     * Now properly handles deletions - removes items from Google Calendar that no longer exist in SmartTask
     */
    public CompletableFuture<Integer> syncAll(List<Task> tasks, List<CalendarEvent> events) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("⚠️ Cannot sync - not authenticated");
                return 0;
            }

            notifyListeners(SyncStatus.SYNCING, "Starting full sync...");

            // ════════════════════════════════════════════════════════════════════════
            // STEP 1: Sync all current items (create/update)
            // ════════════════════════════════════════════════════════════════════════
            List<CompletableFuture<String>> syncFutures = new ArrayList<>();

            // Collect IDs of tasks that currently exist
            Set<String> currentTaskIds = new HashSet<>();
            for (Task task : tasks) {
                currentTaskIds.add(String.valueOf(task.getId()));
                syncFutures.add(syncTask(task));
            }

            // Collect IDs of events that currently exist
            Set<String> currentEventIds = new HashSet<>();
            for (CalendarEvent event : events) {
                currentEventIds.add(event.getId());
                syncFutures.add(syncEvent(event));
            }

            // Wait for all syncs to complete
            CompletableFuture<Void> allSyncs = CompletableFuture.allOf(
                    syncFutures.toArray(new CompletableFuture[0])
            );

            try {
                allSyncs.get();

                int syncedCount = (int) syncFutures.stream()
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .count();

                System.out.println("✅ Synced " + syncedCount + " items to Google Calendar");

                // ════════════════════════════════════════════════════════════════════════
                // STEP 2: Clean up deleted items (remove from Google Calendar)
                // ════════════════════════════════════════════════════════════════════════
                int deletedCount = 0;

                // Find and delete orphaned tasks (in Google but not in SmartTask)
                List<String> orphanedTaskIds = new ArrayList<>();
                for (String taskId : taskToGoogleEventMap.keySet()) {
                    if (!currentTaskIds.contains(taskId)) {
                        orphanedTaskIds.add(taskId);
                    }
                }

                System.out.println("🔍 Found " + orphanedTaskIds.size() + " orphaned tasks to delete");

                for (String taskId : orphanedTaskIds) {
                    String googleEventId = taskToGoogleEventMap.get(taskId);
                    if (googleEventId != null) {
                        try {
                            calendarService.events()
                                    .delete(userCalendarId, googleEventId)
                                    .execute();
                            taskToGoogleEventMap.remove(taskId);
                            deletedCount++;
                            System.out.println("🗑️ Deleted orphaned task from Google Calendar (ID: " + taskId + ")");
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to delete task " + taskId + ": " + e.getMessage());
                        }
                    }
                }

                // Find and delete orphaned events (in Google but not in SmartTask)
                List<String> orphanedEventIds = new ArrayList<>();
                for (String eventId : eventToGoogleEventMap.keySet()) {
                    if (!currentEventIds.contains(eventId)) {
                        orphanedEventIds.add(eventId);
                    }
                }

                System.out.println("🔍 Found " + orphanedEventIds.size() + " orphaned events to delete");

                for (String eventId : orphanedEventIds) {
                    String googleEventId = eventToGoogleEventMap.get(eventId);
                    if (googleEventId != null) {
                        try {
                            calendarService.events()
                                    .delete(userCalendarId, googleEventId)
                                    .execute();
                            eventToGoogleEventMap.remove(eventId);
                            deletedCount++;
                            System.out.println("🗑️ Deleted orphaned event from Google Calendar (ID: " + eventId + ")");
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to delete event " + eventId + ": " + e.getMessage());
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════════════════
                // STEP 3: Save updated mappings
                // ════════════════════════════════════════════════════════════════════════
                if (currentUser != null) {
                    saveSyncMappings(currentUser);
                }

                // ════════════════════════════════════════════════════════════════════════
                // STEP 4: Report results
                // ════════════════════════════════════════════════════════════════════════
                String message = "Synced " + syncedCount + " items";
                if (deletedCount > 0) {
                    message += ", deleted " + deletedCount + " orphaned items";
                }

                System.out.println("✅ Bulk sync completed: " + message);
                notifyListeners(SyncStatus.SYNCED, message);

                return syncedCount;

            } catch (Exception e) {
                System.err.println("❌ Bulk sync failed: " + e.getMessage());
                e.printStackTrace();
                notifyListeners(SyncStatus.ERROR, "Sync failed: " + e.getMessage());
                return 0;
            }

        }, executor);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Get Google Calendar color ID based on task priority
     * Google Calendar colors: 1-11
     */
    private String getColorIdForPriority(int priority) {
        return switch (priority) {
            case 3 -> "11"; // Red - Urgent
            case 2 -> "6";  // Orange - Important
            case 1 -> "9";  // Blue - Standard
            default -> "8"; // Gray - Optional
        };
    }

    /**
     * Get Google Calendar color ID for CalendarEvent priority
     */
    private String getColorIdForEventPriority(CalendarEvent.Priority priority) {
        return switch (priority) {
            case URGENT -> "11";    // Red
            case IMPORTANT -> "6";  // Orange
            case STANDARD -> "9";   // Blue
            case OPTIONAL -> "8";   // Gray
        };
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SYNC LISTENERS
    // ════════════════════════════════════════════════════════════════════════════

    public enum SyncStatus {
        CONNECTED,
        DISCONNECTED,
        SYNCING,
        SYNCED,
        ERROR
    }

    public interface GoogleCalendarSyncListener {
        void onSyncStatusChanged(SyncStatus status, String message);
    }

    public void addSyncListener(GoogleCalendarSyncListener listener) {
        syncListeners.add(listener);
    }

    public void removeSyncListener(GoogleCalendarSyncListener listener) {
        syncListeners.remove(listener);
    }

    private void notifyListeners(SyncStatus status, String message) {
        for (GoogleCalendarSyncListener listener : syncListeners) {
            listener.onSyncStatusChanged(status, message);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════════════════

    public void shutdown() {
        // Save mappings before shutdown
        if (currentUser != null) {
            saveSyncMappings(currentUser);
        }
        executor.shutdown();
    }
}