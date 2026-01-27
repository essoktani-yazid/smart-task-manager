package com.smarttask.model;

/**
 * Sub-task extending base Task model.
 */
public class SubTask extends Task {

    public SubTask() {
        super();
    }

    public SubTask(String title, String description, User user, MainTask parentTask) {
        super(title, description, Priority.MEDIUM, user);
        setParentTask(parentTask);
    }
}