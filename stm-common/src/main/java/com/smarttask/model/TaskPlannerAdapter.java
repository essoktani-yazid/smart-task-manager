package com.smarttask.model;

import com.smarttask.model.Task;
import com.smarttask.model.Priority;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Adapter to make Task compatible with PlannerItem interface
 * WITHOUT modifying the original Task class
 */
public class TaskPlannerAdapter implements PlannerItem {
    
    private final Task task;
    private LocalDate taskDate;      // Extracted from dueDate
    private LocalTime startTime;
    private LocalTime endTime;
    
    public TaskPlannerAdapter(Task task) {
        this.task = task;
        
        // Extract date and time from Task.dueDate
        if (task.getDueDate() != null) {
            this.taskDate = task.getDueDate().toLocalDate();
            this.startTime = task.getDueDate().toLocalTime();
            // Default: 1 hour duration for tasks
            this.endTime = this.startTime.plusHours(1);
        } else {
            this.taskDate = LocalDate.now();
            this.startTime = LocalTime.of(9, 0);
            this.endTime = LocalTime.of(10, 0);
        }
    }
    
    /**
     * Constructor with custom time
     */
    public TaskPlannerAdapter(Task task, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.task = task;
        this.taskDate = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    @Override
    public String getId() {
        return task.getId();
    }
    
    @Override
    public String getTitle() {
        return task.getTitle();
    }
    
    @Override
    public String getDescription() {
        return task.getDescription();
    }
    
    @Override
    public LocalDate getDate() {
        return taskDate;
    }
    
    @Override
    public LocalTime getStartTime() {
        return startTime;
    }
    
    @Override
    public LocalTime getEndTime() {
        return endTime;
    }
    
    @Override
    public boolean isCompleted() {
        return task.getStatus() == Status.COMPLETED;
    }
    
    @Override
    public String getColor() {
        // Task colors based on priority (different shades of blue/purple)
        return switch (task.getPriority()) {
            case LOW -> "#9CA3AF";       // Gray
            case MEDIUM -> "#60A5FA";    // Light Blue
            case HIGH -> "#8B5CF6";      // Purple
            case URGENT -> "#EF4444";    // Red
        };
    }
    
    @Override
    public PlannerItemType getItemType() {
        return PlannerItemType.TASK;
    }
    
    @Override
    public boolean occursOn(LocalDate date) {
        return taskDate != null && taskDate.equals(date);
    }
    
    @Override
    public int getPriorityLevel() {
        return switch (task.getPriority()) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case URGENT -> 3;
        };
    }
    
    /**
     * Get the underlying Task object
     */
    public Task getTask() {
        return task;
    }
    
    /**
     * Update the date/time
     */
    public void setDate(LocalDate date) {
        this.taskDate = date;
        updateTaskDueDate();
    }
    
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        updateTaskDueDate();
    }
    
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    /**
     * Sync back to Task's dueDate
     */
    private void updateTaskDueDate() {
        if (taskDate != null && startTime != null) {
            task.setDueDate(taskDate.atTime(startTime));
        }
    }
}
