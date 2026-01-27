package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Plain model for a calendar event (no JPA annotations).
 */
public class CalendarEvent {

    private String id;

    private String title;

    private String description;

    private Date startTime;

    private Date endTime;

    private String location;

    private String color = "#3788d8";

    private Boolean isAllDay = false;

    private User user;

    // Link to a task if applicable
    private Task task;

    private Date createdAt;

    private String googleEventId;

    // Constructeurs
    public CalendarEvent() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }

    public CalendarEvent(String title, Date startTime, Date endTime, User user) {
        this();
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.user = user;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Boolean getIsAllDay() { return isAllDay; }
    public void setIsAllDay(Boolean isAllDay) { this.isAllDay = isAllDay; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }
}