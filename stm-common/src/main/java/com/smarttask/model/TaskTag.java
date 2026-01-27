package com.smarttask.model;

/**
 * Plain model representing a tag associated with a task (no JPA annotations).
 */
public class TaskTag {

    private TaskTagId id;
    private Task task;
    // tagName is stored redundantly here for convenience
    private String tagName;

    // Constructeurs
    public TaskTag() {}

    public TaskTag(Task task, String tagName) {
        this.id = new TaskTagId(String.valueOf(task.getId()), tagName);
        this.task = task;
        this.tagName = tagName;
    }

    // Getters et Setters
    public TaskTagId getId() { return id; }
    public void setId(TaskTagId id) { this.id = id; }

    // Méthode utilitaire pour obtenir l'ID comme String
    public String getCompositeId() {
        return id != null ? id.toString() : null;
    }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
}