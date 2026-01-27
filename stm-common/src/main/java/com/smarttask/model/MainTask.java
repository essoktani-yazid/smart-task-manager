package com.smarttask.model;

/**
 * Main task (root task) extending base Task model.
 */
public class MainTask extends Task {

    public MainTask() {
        super();
    }

    public MainTask(String title, String description, User user) {
        super(title, description, Priority.MEDIUM, user);
    }

    // Specific methods for main tasks
    public double getProgress() {
        if (getSubTasks().isEmpty()) {
            return getStatus() == Status.COMPLETED ? 100.0 : 0.0;
        }

        long completed = getSubTasks().stream()
                .filter(task -> task.getStatus() == Status.COMPLETED)
                .count();

        return (completed * 100.0) / getSubTasks().size();
    }
}