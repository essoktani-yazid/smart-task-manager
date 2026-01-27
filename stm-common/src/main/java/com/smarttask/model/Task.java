package com.smarttask.model;

import java.time.LocalDateTime;

/**
 * Model representing a task in the application.
 * This class is shared between the client and the server.
 * Note: Hibernate annotations removed - using JDBC now.
 */
public class Task {
    private Long id;
    private String title;
    private String description;
    private Priority priority = Priority.MEDIUM;
    private Status status = Status.TODO;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private User user;

    // Constructeurs
    public Task() {
        this.createdAt = LocalDateTime.now();
    }

    public Task(String title, String description, Priority priority, User user) {
        this();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.user = user;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}

