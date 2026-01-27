package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Plain model for time tracking (no JPA annotations).
 */
public class TimeTracking {

    private String id;

    private Task task;

    private User user;

    private Date startTime;

    private Date endTime;

    private Long durationMs;

    private String notes;

    // Constructeurs
    public TimeTracking() {
        this.id = UUID.randomUUID().toString();
    }

    public TimeTracking(Task task, User user) {
        this();
        this.task = task;
        this.user = user;
        this.startTime = new Date();
    }

    // Méthodes utilitaires
    public void stop() {
        if (startTime != null && endTime == null) {
            endTime = new Date();
            durationMs = endTime.getTime() - startTime.getTime();
        }
    }

    public boolean isRunning() {
        return startTime != null && endTime == null;
    }

    public String getFormattedDuration() {
        if (durationMs == null) return "00:00:00";

        long seconds = durationMs / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}