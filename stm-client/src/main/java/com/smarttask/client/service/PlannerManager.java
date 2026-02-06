package com.smarttask.client.service;


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
 */
public class PlannerManager {
    
    private final CalendarEventDAO eventDAO;
    private final TaskDAO taskDAO;
    
    private final ObservableList<CalendarEvent> events;
    private final ObservableList<Task> tasks;
    
    private final User currentUser; // Current logged-in user
    private final boolean useDatabaseMode;

    // Google Calendar Service
    private final GoogleCalendarService googleCalendarService;
    private boolean googleSyncEnabled = true;
    
    public PlannerManager(User currentUser) {
        this(currentUser, true);
    }
    
    public PlannerManager(User currentUser, boolean useDatabaseMode) {
        this.currentUser = SessionManager.getInstance().getCurrentUser();;
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
     * Load both tasks and events from database
     */
    private void loadFromDatabase() {
        try {
            // Load all events (public - visible to everyone)
            List<CalendarEvent> dbEvents = eventDAO.getAllEvents();
            events.clear();
            events.addAll(dbEvents);
            System.out.println("✅ Loaded " + dbEvents.size() + " events from database");
            
            // Load tasks for current user only (private)
            if (currentUser != null) {
                List<Task> dbTasks = taskDAO.findByUserId(currentUser.getId());
                tasks.clear();
                tasks.addAll(dbTasks);
                System.out.println(currentUser.getId());
                System.out.println(currentUser.getUsername());
                System.out.println("✅ Loaded " + dbTasks.size() + " tasks for user " + currentUser.getUsername());
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading planner items from database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get ALL planner items (tasks + events) for a specific date
     */
    public List<PlannerItem> getItemsForDate(LocalDate date) {
        List<PlannerItem> items = new ArrayList<>();
        
        // Add events (all public events)
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

    /**
     * Enable Google Calendar sync
     * This will authenticate with Google and start syncing
     */
    public void enableGoogleSync() {
        googleCalendarService.authenticate(currentUser)
                .thenAccept(success -> {
                    if (success) {
                        googleSyncEnabled = true;
                        System.out.println("✅ Google Calendar sync enabled");

                        // Optionally sync all existing items
                        // syncAllToGoogle();
                    } else {
                        googleSyncEnabled = false;
                        System.err.println("❌ Failed to enable Google Calendar sync");
                    }
                });
    }

    /**
     * Disable Google Calendar sync
     */
    public void disableGoogleSync() {
        googleSyncEnabled = false;
        googleCalendarService.disconnect();
        System.out.println("🔌 Google Calendar sync disabled");
    }

    /**
     * Check if Google sync is enabled and authenticated
     */
    public boolean isGoogleSyncEnabled() {
        return googleSyncEnabled && googleCalendarService.isAuthenticated();
    }

    /**
     * Sync all tasks and events to Google Calendar
     */
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

    /**
     * Add a sync listener
     */
    public void addGoogleSyncListener(GoogleCalendarService.GoogleCalendarSyncListener listener) {
        googleCalendarService.addSyncListener(listener);
    }



    // ============= EVENT OPERATIONS =============

    
    /**
     * Add a new event (public)
     */
    public void addEvent(CalendarEvent event) {
        if (event == null || events.contains(event)) return;
        
        events.add(event);
        
        if (useDatabaseMode) {
            boolean success = eventDAO.createEvent(event);
            if (success) {
                System.out.println("✅ Event added: " + event.getTitle());
            } else {
                System.err.println("❌ Failed to add event to database");
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
     */
    public void updateEvent(CalendarEvent event) {
        if (event == null) return;
        
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
                System.out.println("✅ Event updated: " + event.getTitle());
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
        // Delete from Google first
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
     * Get all events
     */
    public List<CalendarEvent> getAllEvents() {
        return new ArrayList<>(events);
    }
    
    // ============= TASK OPERATIONS =============
    
    /**
     * Add a new task (private to current user)
     */
    public void addTask(Task task) {
        if (task == null || tasks.contains(task)) return;
        
        // Set user if not set
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
        // Sync to Google Calendar
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
     * Update a task
     */
    public void updateTask(Task task) {
        if (task == null) return;
        
        // Update in memory
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
        // Sync to Google Calendar
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
     * Remove a task
     */
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
    
    /**
     * Get all tasks for current user
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }
    
    /**
     * Get tasks by status
     */
    public List<Task> getTasksByStatus(com.smarttask.model.Status status) {
        return tasks.stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Reload everything from database
     */
    public void reloadFromDatabase() {
        if (useDatabaseMode) {
            loadFromDatabase();
        }
    }
    
    /**
     * Get current user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if database mode is enabled
     */
    public boolean isDatabaseMode() {
        return useDatabaseMode;
    }
    
    /**
     * Clear all data (use with caution!)
     */
    public void clearAll() {
        events.clear();
        tasks.clear();
    }
}
