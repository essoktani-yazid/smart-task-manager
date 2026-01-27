package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Plain model for a comment (no JPA annotations).
 */
public class Comment {

    private String id;

    private Task task;

    private User user;

    private String content;

    private Date createdAt;

    private Date updatedAt;

    // Constructeurs
    public Comment() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Comment(Task task, User user, String content) {
        this();
        this.task = task;
        this.user = user;
        this.content = content;
    }

    // Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", content='" + (content.length() > 20 ? content.substring(0, 20) + "..." : content) + '\'' +
                ", user=" + user.getUsername() +
                '}';
    }
}