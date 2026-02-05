package com.smarttask.model;

/**
 * Plain model representing a shared task between users (no JPA annotations).
 */
public class SharedTask {

    private SharedTaskId id;
    private Task task;
    private User user;
    private PermissionLevel permissionLevel = PermissionLevel.READ;
    private java.util.Date sharedAt;

    // Constructeurs
    public SharedTask() {
        this.sharedAt = new java.util.Date();
    }

    public SharedTask(Task task, User user, PermissionLevel permissionLevel) {
        this();
        this.id = new SharedTaskId(String.valueOf(task.getId()), String.valueOf(user.getId()));
        this.task = task;
        this.user = user;
        this.permissionLevel = permissionLevel;
    }

    // Getters et Setters
    public SharedTaskId getId() { return id; }
    public void setId(SharedTaskId id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }

    public java.util.Date getSharedAt() { return sharedAt; }
    public void setSharedAt(java.util.Date sharedAt) { this.sharedAt = sharedAt; }
    
    // Permission levels for shared tasks
    public static enum PermissionLevel {
        READ,
        WRITE,
        ADMIN
    }
}

// Composite ID for SharedTask (plain model, no JPA annotations)
class SharedTaskId implements java.io.Serializable {

    private String taskId;

    private String userId;

    public SharedTaskId() {}

    public SharedTaskId(String taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    // Getters et Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedTaskId that = (SharedTaskId) o;
        return taskId.equals(that.taskId) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return 31 * taskId.hashCode() + userId.hashCode();
    }
}