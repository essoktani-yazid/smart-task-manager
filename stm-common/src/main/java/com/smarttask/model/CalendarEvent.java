package com.smarttask.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Advanced Calendar Event model with recurring events, priorities, and completion status
 */
public class CalendarEvent {

    // Event types
    public enum EventType {
        ONE_TIME_EVENT,
        RECURRING_EVENT
    }

    // Event priorities with associated colors
    public enum Priority {
        OPTIONAL("#9E9E9E", "Optional"),      // Gray
        STANDARD("#2196F3", "Standard"),      // Blue
        IMPORTANT("#FF9800", "Important"),    // Orange
        URGENT("#F44336", "Urgent");          // Red

        private final String color;
        private final String displayName;

        Priority(String color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }

        public String getColor() { return color; }
        public String getDisplayName() { return displayName; }
    }

    // Recurring event types
    public enum PeriodicType {
        NONE,
        PER_DAY,
        PER_WEEK,
        PER_MONTH,
        PER_YEAR
    }

    // Month position for recurring events
    public enum MonthPlace {
        START_OF_MONTH,
        END_OF_MONTH
    }

    // Core properties
    private String id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Priority priority;
    private boolean completed;

    // Event type properties
    private EventType eventType;

    // Recurring event properties
    private PeriodicType periodicType;
    private String daysInWeek;  // Format: "1,3,5" for Monday, Wednesday, Friday
    private MonthPlace placeInMonth;
    private LocalDate yearlyDate;

    // Metadata
    private LocalDate createdAt;
    private LocalDate lastModified;

    // Constructors
    public CalendarEvent() {
        this.id = UUID.randomUUID().toString();
        this.eventType = EventType.ONE_TIME_EVENT;
        this.periodicType = PeriodicType.NONE;
        this.priority = Priority.STANDARD;
        this.completed = false;
        this.createdAt = LocalDate.now();
        this.lastModified = LocalDate.now();
        this.startTime = LocalTime.of(9, 0);
        this.endTime = LocalTime.of(10, 0);
    }

    public CalendarEvent(String title, Priority priority, String description) {
        this();
        this.title = title;
        this.priority = priority;
        this.description = description;
    }

    public CalendarEvent(String id, String title, LocalDate date, LocalTime startTime,
                         LocalTime endTime, String description, Priority priority) {
        this();
        this.id = id;
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.priority = priority;
    }

    // Business logic methods
    public boolean isRecurring() {
        return eventType == EventType.RECURRING_EVENT;
    }

    public boolean occursOn(LocalDate checkDate) {
        if (eventType == EventType.ONE_TIME_EVENT) {
            return date != null && date.equals(checkDate);
        }

        // Recurring event logic
        switch (periodicType) {
            case PER_DAY:
                return true;

            case PER_WEEK:
                if (daysInWeek != null && !daysInWeek.isEmpty()) {
                    int dayOfWeek = checkDate.getDayOfWeek().getValue();
                    return daysInWeek.contains(String.valueOf(dayOfWeek));
                }
                return false;

            case PER_MONTH:
                if (placeInMonth == MonthPlace.START_OF_MONTH) {
                    return checkDate.getDayOfMonth() == 1;
                } else if (placeInMonth == MonthPlace.END_OF_MONTH) {
                    return checkDate.getDayOfMonth() == checkDate.lengthOfMonth();
                }
                return false;

            case PER_YEAR:
                if (yearlyDate != null) {
                    return checkDate.getMonth() == yearlyDate.getMonth() &&
                            checkDate.getDayOfMonth() == yearlyDate.getDayOfMonth();
                }
                return false;

            default:
                return false;
        }
    }

    public String getColor() {
        return priority.getColor();
    }

    public void toggleCompleted() {
        this.completed = !this.completed;
        this.lastModified = LocalDate.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.lastModified = LocalDate.now();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.lastModified = LocalDate.now();
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) {
        this.date = date;
        this.lastModified = LocalDate.now();
    }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        this.lastModified = LocalDate.now();
    }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        this.lastModified = LocalDate.now();
    }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) {
        this.priority = priority;
        this.lastModified = LocalDate.now();
    }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.lastModified = LocalDate.now();
    }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
        this.lastModified = LocalDate.now();
    }

    public PeriodicType getPeriodicType() { return periodicType; }
    public void setPeriodicType(PeriodicType periodicType) {
        this.periodicType = periodicType;
        this.lastModified = LocalDate.now();
    }

    public String getDaysInWeek() { return daysInWeek; }
    public void setDaysInWeek(String daysInWeek) {
        this.daysInWeek = daysInWeek;
        this.lastModified = LocalDate.now();
    }

    public MonthPlace getPlaceInMonth() { return placeInMonth; }
    public void setPlaceInMonth(MonthPlace placeInMonth) {
        this.placeInMonth = placeInMonth;
        this.lastModified = LocalDate.now();
    }

    public LocalDate getYearlyDate() { return yearlyDate; }
    public void setYearlyDate(LocalDate yearlyDate) {
        this.yearlyDate = yearlyDate;
        this.lastModified = LocalDate.now();
    }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getLastModified() { return lastModified; }
    public void setLastModified(LocalDate lastModified) { this.lastModified = lastModified; }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "title='" + title + '\'' +
                ", date=" + date +
                ", priority=" + priority +
                ", completed=" + completed +
                '}';
    }

    public CalendarEvent clone() {
        CalendarEvent clone = new CalendarEvent();
        clone.id = UUID.randomUUID().toString();
        clone.title = this.title;
        clone.description = this.description;
        clone.date = this.date;
        clone.startTime = this.startTime;
        clone.endTime = this.endTime;
        clone.priority = this.priority;
        clone.completed = this.completed;
        clone.eventType = this.eventType;
        clone.periodicType = this.periodicType;
        clone.daysInWeek = this.daysInWeek;
        clone.placeInMonth = this.placeInMonth;
        clone.yearlyDate = this.yearlyDate;
        return clone;
    }
}
