package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Plain model for a notification (no JPA annotations).
 */
public class Notification {

    private String id;

    private User user;

    private String type;

    private String title;

    private String message;

    private Boolean isRead = false;

    private Date createdAt;

    // Constructeur simple
    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }

    // Getters et Setters basiques
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}