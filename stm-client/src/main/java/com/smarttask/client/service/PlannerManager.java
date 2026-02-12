package com.smarttask.client.service;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PLANNER MANAGER - VISIBILITY FIX
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * This file contains the key fixes for the PlannerManager to properly filter
 * events based on visibility settings.
 *
 * KEY CHANGES:
 * 1. loadFromDatabase() now uses getEventsForUser() instead of getAllEvents()
 * 2. Events are filtered based on visibility (PUBLIC/PRIVATE/SHARED)
 * 3. Only authorized users see events they should see
 */

import com.smarttask.client.util.SessionManager;
import com.smarttask.model.*;
import com.smarttask.server.dao.CalendarEventDAO;
import com.smarttask.server.dao.TaskDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified manager for both Tasks and Events in the planner
 * Handles database integration for both types
 *
 * 🔧 FIXED: Now properly filters events based on visibility
 */
public class PlannerManager {

    private final CalendarEventDAO eventDAO;
    private final TaskDAO taskDAO;

    private final ObservableList<CalendarEvent> events;
    private final ObservableList<Task> tasks;

    private final User currentUser;
    private final boolean useDatabaseMode;

    private final GoogleCalendarService googleCalendarService;
    private boolean googleSyncEnabled = true;

    public PlannerManager(User currentUser) {
        this(currentUser, true);
    }

    public PlannerManager(User currentUser, boolean useDatabaseMode) {
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        this.useDatabaseMode = useDatabaseMode;

        this.eventDAO = new CalendarEventDAO();
        this.taskDAO = new TaskDAO();
        this.googleCalendarService = GoogleCalendarService.getInstance();

        this.events = FXCollections.observableArrayList();
        this.tasks = FXCollections.observableArrayList();

        if (useDatabaseMode) {
            loadFromDatabase();
        }
    }

    /**
     * 🔧 FIXED: Load events with visibility filtering
     *
     * Now only loads:
     * - PUBLIC events (everyone can see)
     * - PRIVATE events created by current user
     * - SHARED events where current user is in the shared list
     */
    private void loadFromDatabase() {
        try {
            if (currentUser == null) {
                System.err.println("⚠️ Cannot load events: No user logged in");
                return;
            }

            // 🔧 FIX: Use getEventsForUser instead of getAllEvents
            // This respects visibility settings
            List<CalendarEvent> dbEvents = eventDAO.getEventsForUser(currentUser.getId());
            events.clear();
            events.addAll(dbEvents);

            System.out.println("✅ Loaded " + dbEvents.size() + " events for user " + currentUser.getUsername());

            // Debug: Show breakdown by visibility
            long publicCount = dbEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.PUBLIC)
                    .count();
            long privateCount = dbEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.PRIVATE)
                    .count();
            long sharedCount = dbEvents.stream()
                    .filter(e -> e.getVisibility() == CalendarEvent.EventVisibility.SHARED)
                    .count();

            System.out.println("   📊 Breakdown: " + publicCount + " public, " +
                    privateCount + " private, " + sharedCount + " shared");

            // Load tasks for current user only (private)
            List<Task> dbTasks = taskDAO.findByUserId(currentUser.getId());
            tasks.clear();
            tasks.addAll(dbTasks);
            System.out.println("✅ Loaded " + dbTasks.size() + " tasks for user " + currentUser.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Error loading planner items from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get ALL planner items (tasks + events) for a specific date
     * Events are already filtered by visibility during load
     */
    public List<PlannerItem> getItemsForDate(LocalDate date) {
        List<PlannerItem> items = new ArrayList<>();

        // Add events (already filtered by visibility)
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
    // EVENT OPERATIONS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Add a new event
     * 🔧 FIXED: Properly sets creator and respects visibility
     */
    public void addEvent(CalendarEvent event) {
        if (event == null || events.contains(event)) return;

        // 🔧 FIX: Ensure creator is set
        if (event.getCreatorUserId() == null && currentUser != null) {
            event.setCreatorUserId(currentUser.getId());
        }

        // 🔧 FIX: Log visibility for debugging
        System.out.println("📅 Adding event: " + event.getTitle());
        System.out.println("   Visibility: " + event.getVisibility());
        System.out.println("   Creator: " + event.getCreatorUserId());
        if (event.getVisibility() == CalendarEvent.EventVisibility.SHARED) {
            System.out.println("   Shared with users: " + event.getSharedWithUserIds());
            System.out.println("   Shared with emails: " + event.getSharedWithEmails());
        }

        events.add(event);

        if (useDatabaseMode) {
            boolean success = eventDAO.createEvent(event);
            if (success) {
                System.out.println("✅ Event saved to database");
            } else {
                System.err.println("❌ Failed to save event to database");
                events.remove(event);
            }
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
     * Update an event
     * 🔧 FIXED: Properly updates visibility settings
     */
    public void updateEvent(CalendarEvent event) {
        if (event == null) return;

        // 🔧 FIX: Log visibility for debugging
        System.out.println("📝 Updating event: " + event.getTitle());
        System.out.println("   Visibility: " + event.getVisibility());

        // Update in memory
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.set(i, event);
                break;
            }
        }

        if (useDatabaseMode) {
            boolean success = eventDAO.updateEvent(event);
            if (success) {
                System.out.println("✅ Event updated in database");
            } else {
                System.err.println("❌ Failed to update event in database");
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
     * Remove an event
     */
    public void removeEvent(CalendarEvent event) {
        if (event == null) return;

        events.remove(event);

        if (useDatabaseMode) {
            eventDAO.deleteEvent(event.getId());
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
    // TASK OPERATIONS
    // ════════════════════════════════════════════════════════════════════════════

    public void addTask(Task task) {
        if (task == null || tasks.contains(task)) return;

        if (task.getUser() == null) {
            task.setUser(currentUser);
        }

        tasks.add(task);

        if (useDatabaseMode) {
            try {
                taskDAO.save(task);
                System.out.println("✅ Task added: " + task.getTitle() + " (ID: " + task.getId() + ")");
            } catch (Exception e) {
                System.err.println("❌ Failed to add task to database: " + e.getMessage());
                tasks.remove(task);
                e.printStackTrace();
            }
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

    public void updateTask(Task task) {
        if (task == null) return;

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                break;
            }
        }

        if (useDatabaseMode) {
            try {
                taskDAO.update(task);
                System.out.println("✅ Task updated: " + task.getTitle());
            } catch (Exception e) {
                System.err.println("❌ Failed to update task: " + e.getMessage());
                e.printStackTrace();
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

    public void removeTask(Task task) {
        if (task == null) return;

        tasks.remove(task);

        if (useDatabaseMode) {
            taskDAO.deleteById(task.getId());
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

    public void reloadFromDatabase() {
        if (useDatabaseMode) {
            loadFromDatabase();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isDatabaseMode() {
        return useDatabaseMode;
    }

    public void clearAll() {
        events.clear();
        tasks.clear();
    }
}