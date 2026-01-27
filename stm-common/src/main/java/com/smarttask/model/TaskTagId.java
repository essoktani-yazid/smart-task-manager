package com.smarttask.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite ID for TaskTag (plain model, no JPA annotations).
 */
public class TaskTagId implements Serializable {

    private String taskId;
    private String tagName;

    public TaskTagId() {}

    public TaskTagId(String taskId, String tagName) {
        this.taskId = taskId;
        this.tagName = tagName;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskTagId that = (TaskTagId) o;
        return Objects.equals(taskId, that.taskId) &&
                Objects.equals(tagName, that.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, tagName);
    }

    @Override
    public String toString() {
        return taskId + "_" + tagName;
    }
}