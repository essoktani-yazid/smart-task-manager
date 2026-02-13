package com.smarttask.client.service;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PLANNER MANAGER - API VERSION
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * UPDATED: Now uses ApiClient to communicate with REST API instead of direct DAO
 *
 * Architecture:
 * CalendarController → PlannerManager → ApiClient → HTTP → EventHandler/TaskHandler → DAO
 *
 * Changes from old version:
 * - Removed direct DAO imports
 * - Added ApiClient for all database operations
 * - All CRUD operations now go through HTTP requests
 */

import com.smarttask.client.util.SessionManager;
import com.smarttask.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.smarttask.client.config.AppConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified manager for both Tasks and Events in the planner
 * Handles API communication for both types
 */
public class PlannerManager {

    private final ApiClient apiClient;  // ✅ NEW: API client instead of DAOs

    private final ObservableList<CalendarEvent> events;
    private final ObservableList<Task> tasks;

    private final User currentUser;
    private final boolean useApiMode;

    private final GoogleCalendarService googleCalendarService;
    private boolean googleSyncEnabled = true;

    /**
     * Constructor with API mode enabled
     */
    public PlannerManager(User currentUser) {
        this(currentUser, true);
    }

    /**
     * Constructor with configurable API mode
     */
    public PlannerManager(User currentUser, boolean useApiMode) {
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        this.useApiMode = useApiMode;

        // ✅ NEW: Initialize API client instead of DAOs
        this.apiClient = new ApiClient(AppConfig.BASE_URL);
        this.googleCalendarService = GoogleCalendarService.getInstance();

        this.events = FXCollections.observableArrayList();
        this.tasks = FXCollections.observableArrayList();

        if (useApiMode) {
            loadFromApi();
        }
    }

    /**
     * ✅ NEW: Load data via API instead of direct database access
     */
    private void loadFromApi() {
        try {
            if (currentUser == null) {
                System.err.println("⚠️ Cannot load data: No user logged in");
                return;
            }

            // Test API connection first
            if (!apiClient.testConnection()) {
                System.err.println("❌ Cannot connect to API server");
                return;
            }

            // ✅ Load events via API
            List<CalendarEvent> apiEvents = apiClient.getEventsForUser(currentUser.getId());
            events.clear();
            events.addAll(apiEvents);

            System.out.println("✅ Loaded " + apiEvents.size() + " events via API for user " + currentUser.getUsername());

            // Debug visibility breakdown
            long publicCount = apiEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.PUBLIC)
                    .count();
            long privateCount = apiEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.PRIVATE)
                    .count();
            long sharedCount = apiEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.SHARED)
                    .count();

            System.out.println("   📊 Breakdown: " + publicCount + " public, " +
                    privateCount + " private, " + sharedCount + " shared");

            // ✅ Load tasks via API
            List<Task> apiTasks = apiClient.getTasksForUser(currentUser.getId());
            tasks.clear();
            tasks.addAll(apiTasks);
            System.out.println("✅ Loaded " + apiTasks.size() + " tasks via API for user " + currentUser.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Error loading planner items from API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get ALL planner items (tasks + events) for a specific date
     */
    public List<PlannerItem> getItemsForDate(LocalDate date) {
        List<PlannerItem> items = new ArrayList<>();

        // Add events (already filtered by visibility via API)
        events.stream()
                .filter(e -> e.occursOn(date))
                .map(EventPlannerAdapter::new)
                .forEach(items::add);

        // Add tasks (only for current user)
        tasks.stream()
                .filter(t -> {
                    if (t.getDueDate() == null) return false;
                    return t.getDueDate().toLocalDate().equals(date);
                })
                .map(TaskPlannerAdapter::new)
                .forEach(items::add);

        // Sort by start time
        items.sort(Comparator.comparing(PlannerItem::getStartTime));

        return items;
    }

    /**
     * Get all planner items in a date range
     */
    public List<PlannerItem> getItemsInRange(LocalDate startDate, LocalDate endDate) {
        List<PlannerItem> items = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            items.addAll(getItemsForDate(current));
            current = current.plusDays(1);
        }

        return items;
    }

    /**
     * Get upcoming items (next N days)
     */
    public List<PlannerItem> getUpcomingItems(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        return getItemsInRange(today, endDate).stream()
                .filter(item -> !item.isCompleted())
                .sorted((i1, i2) -> {
                    int dateCompare = i1.getDate().compareTo(i2.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return i1.getStartTime().compareTo(i2.getStartTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if a date has any items
     */
    public boolean hasItemsOn(LocalDate date) {
        return !getItemsForDate(date).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GOOGLE CALENDAR SYNC SETTINGS
    // ════════════════════════════════════════════════════════════════════════════

    public void enableGoogleSync() {
        googleCalendarService.authenticate(currentUser)
                .thenAccept(success -> {
                    if (success) {
                        googleSyncEnabled = true;
                        System.out.println("✅ Google Calendar sync enabled");
                    } else {
                        googleSyncEnabled = false;
                        System.err.println("❌ Failed to enable Google Calendar sync");
                    }
                });
    }

    public void disableGoogleSync() {
        googleSyncEnabled = false;
        googleCalendarService.disconnect();
        System.out.println("🔌 Google Calendar sync disabled");
    }

    public boolean isGoogleSyncEnabled() {
        return googleSyncEnabled && googleCalendarService.isAuthenticated();
    }

    public void syncAllToGoogle() {
        if (!isGoogleSyncEnabled()) {
            System.err.println("⚠️ Google sync not enabled");
            return;
        }

        List<Task> tasks = getAllTasks();
        List<CalendarEvent> events = getAllEvents();

        googleCalendarService.syncAll(tasks, events)
                .thenAccept(count -> {
                    System.out.println("✅ Synced " + count + " items to Google Calendar");
                });
    }

    public void addGoogleSyncListener(GoogleCalendarService.GoogleCalendarSyncListener listener) {
        googleCalendarService.addSyncListener(listener);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EVENT OPERATIONS - API VERSION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ UPDATED: Add event via API
     */
    public void addEvent(CalendarEvent event) {
        if (event == null || events.contains(event)) return;

        // Ensure creator is set
        if (event.getCreatorUserId() == null && currentUser != null) {
            event.setCreatorUserId(currentUser.getId());
        }

        System.out.println("📅 Adding event via API: " + event.getTitle());
        System.out.println("   Visibility: " + event.getVisibility());
        System.out.println("   Creator: " + event.getCreatorUserId());

        if (useApiMode) {
            // ✅ Use API instead of DAO
            CalendarEvent createdEvent = apiClient.createEvent(event);
            if (createdEvent != null) {
                events.add(createdEvent);
                System.out.println("✅ Event saved via API");
            } else {
                System.err.println("❌ Failed to save event via API");
            }
        } else {
            events.add(event);
        }

        // Sync to Google Calendar
        if (isGoogleSyncEnabled()) {
            googleCalendarService.syncEvent(event)
                    .thenAccept(googleId -> {
                        if (googleId != null) {
                            System.out.println("   📤 Synced to Google Calendar");
                        }
                    });
        }
    }

    /**
     * ✅ UPDATED: Update event via API
     */
    public void updateEvent(CalendarEvent event) {
        if (event == null) return;

        System.out.println("📝 Updating event via API: " + event.getTitle());
        System.out.println("   Visibility: " + event.getVisibility());

        // Update in memory
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.set(i, event);
                break;
            }
        }

        if (useApiMode) {
            // ✅ Use API instead of DAO
            boolean success = apiClient.updateEvent(event);
            if (!success) {
                System.err.println("❌ Failed to update event via API");
            }
        }

        // Sync to Google Calendar
        if (isGoogleSyncEnabled()) {
            googleCalendarService.syncEvent(event)
                    .thenAccept(googleId -> {
                        if (googleId != null) {
                            System.out.println("   📤 Synced update to Google Calendar");
                        }
                    });
        }
    }

    /**
     * ✅ UPDATED: Remove event via API
     */
    public void removeEvent(CalendarEvent event) {
        if (event == null) return;

        events.remove(event);

        if (useApiMode) {
            // ✅ Use API instead of DAO
            apiClient.deleteEvent(event.getId());
        }

        if (isGoogleSyncEnabled()) {
            googleCalendarService.deleteEvent(event)
                    .thenAccept(success -> {
                        if (success) {
                            System.out.println("   🗑️ Deleted from Google Calendar");
                        }
                    });
        }
    }

    public GoogleCalendarService getGoogleCalendarService() {
        return googleCalendarService;
    }

    /**
     * Get all events (already filtered by visibility)
     */
    public List<CalendarEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TASK OPERATIONS - API VERSION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ UPDATED: Add task via API
     */
    public void addTask(Task task) {
        if (task == null || tasks.contains(task)) return;

        if (task.getUser() == null) {
            task.setUser(currentUser);
        }

        if (useApiMode) {
            // ✅ Use API instead of DAO
            Task createdTask = apiClient.createTask(task);
            if (createdTask != null) {
                tasks.add(createdTask);
                System.out.println("✅ Task added via API: " + task.getTitle());
            } else {
                System.err.println("❌ Failed to add task via API");
            }
        } else {
            tasks.add(task);
        }

        if (isGoogleSyncEnabled()) {
            googleCalendarService.syncTask(task)
                    .thenAccept(googleId -> {
                        if (googleId != null) {
                            System.out.println("   📤 Synced to Google Calendar");
                        }
                    });
        }
    }

    /**
     * ✅ UPDATED: Update task via API
     */
    public void updateTask(Task task) {
        if (task == null) return;

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                break;
            }
        }

        if (useApiMode) {
            // ✅ Use API instead of DAO
            boolean success = apiClient.updateTask(task);
            if (!success) {
                System.err.println("❌ Failed to update task via API");
            }
        }

        if (isGoogleSyncEnabled()) {
            googleCalendarService.syncTask(task)
                    .thenAccept(googleId -> {
                        if (googleId != null) {
                            System.out.println("   📤 Synced update to Google Calendar");
                        }
                    });
        }
    }

    /**
     * ✅ UPDATED: Remove task via API
     */
    public void removeTask(Task task) {
        if (task == null) return;

        tasks.remove(task);

        if (useApiMode) {
            // ✅ Use API instead of DAO
            apiClient.deleteTask(task.getId());
        }

        if (isGoogleSyncEnabled()) {
            googleCalendarService.deleteTask(task)
                    .thenAccept(success -> {
                        if (success) {
                            System.out.println("   🗑️ Deleted from Google Calendar");
                        }
                    });
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksByStatus(com.smarttask.model.Status status) {
        return tasks.stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ✅ UPDATED: Reload via API
     */
    public void reloadFromDatabase() {
        if (useApiMode) {
            loadFromApi();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isApiMode() {
        return useApiMode;
    }

    /**
     * ✅ NEW: Get API client for advanced usage
     */
    public ApiClient getApiClient() {
        return apiClient;
    }

    public void clearAll() {
        events.clear();
        tasks.clear();
    }
}