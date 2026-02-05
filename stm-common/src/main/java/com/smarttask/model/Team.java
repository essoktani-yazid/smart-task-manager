package com.smarttask.model;

import java.time.LocalDateTime;

/**
 * Model representing a team.
 * Shared between client and server.
 */
public class Team {

    private String id;
    private String name;
    private String description;
    private String color;
    private String ownerId;
    private boolean active;
    private LocalDateTime createdAt;

    // Constructors
    public Team() {
    }

    public Team(String name, String description, String color, String ownerId) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.ownerId = ownerId;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}

